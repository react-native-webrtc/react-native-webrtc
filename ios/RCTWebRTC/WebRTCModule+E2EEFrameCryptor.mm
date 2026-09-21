#import <atomic>
#import <mutex>

#import <CommonCrypto/CommonCrypto.h>

#import "E2EEFrameCryptorABI.h"
#import "WebRTCModule+E2EEFrameCryptor.h"

// BEGIN-E2EE-CRYPTO-CORE
// The block between the BEGIN/END markers is self-contained C++/ObjC (only
// needs the ABI header, Foundation and CommonCrypto) and is extracted
// verbatim for the standalone compile/functional check.

// CCCryptorGCMOneshot{Encrypt,Decrypt} are exported by libcommonCrypto but
// are not declared in the public SDK headers; declare them here. They are
// available since iOS 13 / macOS 10.13.
extern "C" {

CCCryptorStatus CCCryptorGCMOneshotEncrypt(CCAlgorithm alg,
                                           const void *key,
                                           size_t keyLength,
                                           const void *iv,
                                           size_t ivLength,
                                           const void *aData,
                                           size_t aDataLength,
                                           const void *dataIn,
                                           size_t dataInLength,
                                           void *dataOut,
                                           void *tagOut,
                                           size_t tagLength)
    API_AVAILABLE(macos(10.13), ios(13.0), watchos(6.0), tvos(13.0));

CCCryptorStatus CCCryptorGCMOneshotDecrypt(CCAlgorithm alg,
                                           const void *key,
                                           size_t keyLength,
                                           const void *iv,
                                           size_t ivLength,
                                           const void *aData,
                                           size_t aDataLength,
                                           const void *dataIn,
                                           size_t dataInLength,
                                           void *dataOut,
                                           const void *tagIn,
                                           size_t tagLength)
    API_AVAILABLE(macos(10.13), ios(13.0), watchos(6.0), tvos(13.0));

}  // extern "C"

namespace {

constexpr size_t kAesKeySize = 16;
constexpr size_t kKeyRingSize = 16;
constexpr size_t kGcmTagSize = 16;
constexpr size_t kIvSize = 12;
constexpr size_t kTrailerSize = 2;
// GCM tag (16) + IV (12) + trailer (2). Must match the Android side.
constexpr size_t kFrameCryptorOverhead = kGcmTagSize + kIvSize + kTrailerSize;

// Length of the unencrypted frame header: 1 byte (Opus TOC) for audio;
// 10 bytes for VP8 keyframes, 3 bytes for other video frames.
size_t FrameHeaderLength(cricket::MediaType media_type, rtc::ArrayView<const uint8_t> frame) {
    if (media_type == cricket::MEDIA_TYPE_AUDIO) {
        return 1;
    }
    if (frame.size() > 0 && (frame[0] & 0x01) == 0) {
        return 10;
    }
    return 3;
}

// Shared cryptor state: the key ring, the current send key index, the
// per-cryptor frame counter and the enabled flag.
class AesGcmState {
   public:
    AesGcmState() : send_key_index(0), enabled(true), detached(false), frame_counter(arc4random()) {}

    // Derives a 16-byte AES key from the passphrase (first 16 bytes of
    // SHA-256 of its UTF-8 encoding) and stores it in the given key ring
    // slot.
    void SetKey(int key_index, NSString *passphrase) {
        NSData *passphraseData = [passphrase dataUsingEncoding:NSUTF8StringEncoding];
        uint8_t digest[CC_SHA256_DIGEST_LENGTH];
        CC_SHA256(passphraseData.bytes, static_cast<CC_LONG>(passphraseData.length), digest);

        std::lock_guard<std::mutex> lock(key_ring_mutex);
        memcpy(key_ring[key_index & (kKeyRingSize - 1)], digest, kAesKeySize);
        key_valid[key_index & (kKeyRingSize - 1)] = true;
    }

    // Copies the key at the given slot into out_key. Returns false if no key
    // is set there.
    bool GetKey(int key_index, uint8_t *out_key) {
        std::lock_guard<std::mutex> lock(key_ring_mutex);
        if (!key_valid[key_index]) {
            return false;
        }
        memcpy(out_key, key_ring[key_index], kAesKeySize);
        return true;
    }

    std::atomic<int> send_key_index;
    std::atomic<bool> enabled;
    // Set when the cryptor is disposed. Encryptors pass frames through
    // unchanged; decryptors drop frames (the remote side is presumably still
    // sending encrypted frames).
    std::atomic<bool> detached;
    // The frame counter starts at a random 32-bit value and is incremented
    // for every encrypted frame.
    std::atomic<uint64_t> frame_counter;

   private:
    std::mutex key_ring_mutex;
    uint8_t key_ring[kKeyRingSize][kAesKeySize] = {};
    bool key_valid[kKeyRingSize] = {};
};

class AesGcmFrameEncryptor final : public webrtc::FrameEncryptorInterface, public AesGcmState {
   public:
    AesGcmFrameEncryptor() : ref_count_(0) {}

    void AddRef() const override { ref_count_.fetch_add(1, std::memory_order_relaxed); }

    webrtc::RefCountReleaseStatus Release() const override {
        if (ref_count_.fetch_sub(1, std::memory_order_acq_rel) == 1) {
            delete this;
            return webrtc::RefCountReleaseStatus::kDroppedLastRef;
        }
        return webrtc::RefCountReleaseStatus::kOtherRefsRemained;
    }

    // Stores the key and makes it the current send key.
    void SetKey(int key_index, NSString *passphrase) {
        AesGcmState::SetKey(key_index, passphrase);
        send_key_index.store(key_index & (kKeyRingSize - 1), std::memory_order_relaxed);
    }

    int Encrypt(cricket::MediaType media_type,
                uint32_t ssrc,
                rtc::ArrayView<const uint8_t> /* additional_data */,
                rtc::ArrayView<const uint8_t> frame,
                rtc::ArrayView<uint8_t> encrypted_frame,
                size_t *bytes_written) override {
        *bytes_written = 0;

        if (detached.load(std::memory_order_relaxed) || !enabled.load(std::memory_order_relaxed)) {
            return PassThrough(frame, encrypted_frame, bytes_written);
        }

        size_t header_length = FrameHeaderLength(media_type, frame);
        if (frame.size() < header_length) {
            return PassThrough(frame, encrypted_frame, bytes_written);
        }

        int key_index = send_key_index.load(std::memory_order_relaxed) & (kKeyRingSize - 1);
        uint8_t key[kAesKeySize];
        if (!GetKey(key_index, key)) {
            // Enabled but no key set: drop the frame.
            return 1;
        }

        if (__builtin_available(ios 13.0, macos 10.13, *)) {
            uint64_t counter = frame_counter.fetch_add(1, std::memory_order_relaxed);
            uint8_t iv[kIvSize];
            iv[0] = (ssrc >> 24) & 0xFF;
            iv[1] = (ssrc >> 16) & 0xFF;
            iv[2] = (ssrc >> 8) & 0xFF;
            iv[3] = ssrc & 0xFF;
            for (int i = 0; i < 8; i++) {
                iv[4 + i] = (counter >> (56 - i * 8)) & 0xFF;
            }

            uint8_t *out = encrypted_frame.data();
            memcpy(out, frame.data(), header_length);
            size_t payload_length = frame.size() - header_length;

            CCCryptorStatus status = CCCryptorGCMOneshotEncrypt(kCCAlgorithmAES,
                                                                key,
                                                                kAesKeySize,
                                                                iv,
                                                                kIvSize,
                                                                frame.data(),
                                                                header_length,
                                                                frame.data() + header_length,
                                                                payload_length,
                                                                out + header_length,
                                                                out + header_length + payload_length,
                                                                kGcmTagSize);
            if (status != kCCSuccess) {
                return 1;
            }

            memcpy(out + header_length + payload_length + kGcmTagSize, iv, kIvSize);
            out[header_length + payload_length + kGcmTagSize + kIvSize] = kIvSize;
            out[header_length + payload_length + kGcmTagSize + kIvSize + 1] = key_index & 0xFF;
            *bytes_written = frame.size() + kFrameCryptorOverhead;
            return 0;
        }

        // GCM unavailable (iOS < 13): drop the frame.
        return 1;
    }

    size_t GetMaxCiphertextByteSize(cricket::MediaType /* media_type */, size_t frame_size) override {
        return frame_size + kFrameCryptorOverhead;
    }

   private:
    static int PassThrough(rtc::ArrayView<const uint8_t> frame, rtc::ArrayView<uint8_t> out, size_t *bytes_written) {
        // The output buffer is sized via GetMaxCiphertextByteSize, which is
        // always >= the input size.
        if (frame.size() > out.size()) {
            return 1;
        }
        memcpy(out.data(), frame.data(), frame.size());
        *bytes_written = frame.size();
        return 0;
    }

    mutable std::atomic<int> ref_count_;
};

class AesGcmFrameDecryptor final : public webrtc::FrameDecryptorInterface, public AesGcmState {
   public:
    AesGcmFrameDecryptor() : ref_count_(0) {}

    void AddRef() const override { ref_count_.fetch_add(1, std::memory_order_relaxed); }

    webrtc::RefCountReleaseStatus Release() const override {
        if (ref_count_.fetch_sub(1, std::memory_order_acq_rel) == 1) {
            delete this;
            return webrtc::RefCountReleaseStatus::kDroppedLastRef;
        }
        return webrtc::RefCountReleaseStatus::kOtherRefsRemained;
    }

    Result Decrypt(cricket::MediaType media_type,
                   const std::vector<uint32_t> & /* csrcs */,
                   rtc::ArrayView<const uint8_t> /* additional_data */,
                   rtc::ArrayView<const uint8_t> encrypted_frame,
                   rtc::ArrayView<uint8_t> frame) override {
        if (detached.load(std::memory_order_relaxed)) {
            return Result(Status::kRecoverable, 0);
        }
        if (!enabled.load(std::memory_order_relaxed)) {
            return PassThrough(encrypted_frame, frame);
        }

        size_t header_length = FrameHeaderLength(media_type, encrypted_frame);
        if (encrypted_frame.size() < header_length) {
            return PassThrough(encrypted_frame, frame);
        }

        size_t total = encrypted_frame.size();
        if (total < header_length + kFrameCryptorOverhead || encrypted_frame[total - 2] != kIvSize) {
            return Result(Status::kRecoverable, 0);
        }

        int key_index = encrypted_frame[total - 1] & (kKeyRingSize - 1);
        uint8_t key[kAesKeySize];
        if (!GetKey(key_index, key)) {
            return Result(Status::kRecoverable, 0);
        }

        if (__builtin_available(ios 13.0, macos 10.13, *)) {
            const uint8_t *in = encrypted_frame.data();
            const uint8_t *iv = in + total - kIvSize - kTrailerSize;
            size_t payload_length = total - header_length - kFrameCryptorOverhead;
            const uint8_t *ciphertext = in + header_length;
            const uint8_t *tag = ciphertext + payload_length;

            memcpy(frame.data(), in, header_length);
            CCCryptorStatus status = CCCryptorGCMOneshotDecrypt(kCCAlgorithmAES,
                                                                key,
                                                                kAesKeySize,
                                                                iv,
                                                                kIvSize,
                                                                in,
                                                                header_length,
                                                                ciphertext,
                                                                payload_length,
                                                                frame.data() + header_length,
                                                                tag,
                                                                kGcmTagSize);
            if (status != kCCSuccess) {
                // Authentication failure: drop the frame silently.
                return Result(Status::kRecoverable, 0);
            }
            return Result(Status::kOk, header_length + payload_length);
        }

        return Result(Status::kRecoverable, 0);
    }

    size_t GetMaxPlaintextByteSize(cricket::MediaType /* media_type */, size_t encrypted_frame_size) override {
        return encrypted_frame_size;
    }

   private:
    static Result PassThrough(rtc::ArrayView<const uint8_t> encrypted_frame, rtc::ArrayView<uint8_t> frame) {
        // The output buffer is sized via GetMaxPlaintextByteSize, which is
        // always >= the input size.
        if (encrypted_frame.size() > frame.size()) {
            return Result(Status::kRecoverable, 0);
        }
        memcpy(frame.data(), encrypted_frame.data(), encrypted_frame.size());
        return Result(Status::kOk, encrypted_frame.size());
    }

    mutable std::atomic<int> ref_count_;
};

}  // namespace

// END-E2EE-CRYPTO-CORE

// Private WebRTC SDK API for attaching frame cryptors. Present in the Jitsi
// WebRTC build (verified in the framework's ObjC runtime metadata); the
// argument is a rtc::scoped_refptr passed by value, i.e. a single pointer.
@interface RTCRtpSender (E2EEFrameCryptor)
- (void)setFrameEncryptor:(rtc::scoped_refptr<webrtc::FrameEncryptorInterface>)frameEncryptor;
@end

@interface RTCRtpReceiver (E2EEFrameCryptor)
- (void)setFrameDecryptor:(rtc::scoped_refptr<webrtc::FrameDecryptorInterface>)frameDecryptor;
@end

// Owns a frame cryptor: the scoped_refptr keeps the C++ object alive for as
// long as the entry (and WebRTC, which took its own reference when the
// cryptor was attached) needs it.
@interface E2EEFrameCryptorEntry : NSObject

- (instancetype)initWithSender:(RTCRtpSender *)sender;
- (instancetype)initWithReceiver:(RTCRtpReceiver *)receiver;
- (void)setKey:(NSString *)key atIndex:(NSInteger)keyIndex;
- (void)setEnabled:(BOOL)enabled;
- (void)detach;

@end

@implementation E2EEFrameCryptorEntry {
    rtc::scoped_refptr<webrtc::FrameEncryptorInterface> _encryptor;
    rtc::scoped_refptr<webrtc::FrameDecryptorInterface> _decryptor;
    AesGcmFrameEncryptor *_rawEncryptor;
    AesGcmFrameDecryptor *_rawDecryptor;
}

- (instancetype)initWithSender:(RTCRtpSender *)sender {
    self = [super init];
    if (self) {
        _rawEncryptor = new AesGcmFrameEncryptor();
        _encryptor = rtc::scoped_refptr<webrtc::FrameEncryptorInterface>(_rawEncryptor);
        [sender setFrameEncryptor:_encryptor];
    }
    return self;
}

- (instancetype)initWithReceiver:(RTCRtpReceiver *)receiver {
    self = [super init];
    if (self) {
        _rawDecryptor = new AesGcmFrameDecryptor();
        _decryptor = rtc::scoped_refptr<webrtc::FrameDecryptorInterface>(_rawDecryptor);
        [receiver setFrameDecryptor:_decryptor];
    }
    return self;
}

- (void)setKey:(NSString *)key atIndex:(NSInteger)keyIndex {
    if (_rawEncryptor) {
        _rawEncryptor->SetKey((int)keyIndex, key);
    } else if (_rawDecryptor) {
        _rawDecryptor->SetKey((int)keyIndex, key);
    }
}

- (void)setEnabled:(BOOL)enabled {
    if (_rawEncryptor) {
        _rawEncryptor->enabled.store(enabled, std::memory_order_relaxed);
    } else if (_rawDecryptor) {
        _rawDecryptor->enabled.store(enabled, std::memory_order_relaxed);
    }
}

- (void)detach {
    if (_rawEncryptor) {
        _rawEncryptor->detached.store(true, std::memory_order_relaxed);
    } else if (_rawDecryptor) {
        _rawDecryptor->detached.store(true, std::memory_order_relaxed);
    }
}

@end

static NSMutableDictionary<NSString *, E2EEFrameCryptorEntry *> *E2EEFrameCryptorEntries() {
    static NSMutableDictionary<NSString *, E2EEFrameCryptorEntry *> *entries = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        entries = [NSMutableDictionary new];
    });
    return entries;
}

@implementation WebRTCModule (E2EEFrameCryptor)

RCT_EXPORT_METHOD(e2eeCreateFrameCryptor : (nonnull NSDictionary *)options resolver : (RCTPromiseResolveBlock)
                      resolve rejecter : (RCTPromiseRejectBlock)reject) {
    NSNumber *peerConnectionId = options[@"peerConnectionId"];
    NSString *type = options[@"type"];
    NSString *rtpId = options[@"rtpId"];

    RTCPeerConnection *peerConnection = self.peerConnections[peerConnectionId];
    if (!peerConnection) {
        reject(@"E2EE_CREATE_FAILED", @"PeerConnection not found", nil);
        return;
    }

    E2EEFrameCryptorEntry *entry = nil;
    if ([type isEqualToString:@"sender"]) {
        RTCRtpSender *sender = nil;
        for (RTCRtpSender *s in peerConnection.senders) {
            if ([s.senderId isEqualToString:rtpId]) {
                sender = s;
                break;
            }
        }
        if (!sender) {
            reject(@"E2EE_CREATE_FAILED", @"RtpSender not found", nil);
            return;
        }
        entry = [[E2EEFrameCryptorEntry alloc] initWithSender:sender];
    } else if ([type isEqualToString:@"receiver"]) {
        RTCRtpReceiver *receiver = nil;
        for (RTCRtpReceiver *r in peerConnection.receivers) {
            if ([r.receiverId isEqualToString:rtpId]) {
                receiver = r;
                break;
            }
        }
        if (!receiver) {
            reject(@"E2EE_CREATE_FAILED", @"RtpReceiver not found", nil);
            return;
        }
        entry = [[E2EEFrameCryptorEntry alloc] initWithReceiver:receiver];
    } else {
        reject(@"E2EE_CREATE_FAILED", @"type must be 'sender' or 'receiver'", nil);
        return;
    }

    NSString *cryptorId = [[NSUUID UUID] UUIDString];
    E2EEFrameCryptorEntries()[cryptorId] = entry;
    resolve(cryptorId);
}

RCT_EXPORT_METHOD(e2eeFrameCryptorSetKey : (nonnull NSDictionary *)options resolver : (RCTPromiseResolveBlock)
                      resolve rejecter : (RCTPromiseRejectBlock)reject) {
    NSString *cryptorId = options[@"cryptorId"];
    E2EEFrameCryptorEntry *entry = E2EEFrameCryptorEntries()[cryptorId];
    if (!entry) {
        reject(@"E2EE_SET_KEY_FAILED", @"Frame cryptor not found", nil);
        return;
    }

    [entry setKey:options[@"key"] atIndex:[options[@"keyIndex"] integerValue]];
    resolve(@(YES));
}

RCT_EXPORT_METHOD(e2eeFrameCryptorSetEnabled : (nonnull NSDictionary *)options resolver : (RCTPromiseResolveBlock)
                      resolve rejecter : (RCTPromiseRejectBlock)reject) {
    NSString *cryptorId = options[@"cryptorId"];
    E2EEFrameCryptorEntry *entry = E2EEFrameCryptorEntries()[cryptorId];
    if (!entry) {
        reject(@"E2EE_SET_ENABLED_FAILED", @"Frame cryptor not found", nil);
        return;
    }

    [entry setEnabled:[options[@"enabled"] boolValue]];
    resolve(@(YES));
}

RCT_EXPORT_METHOD(e2eeFrameCryptorDispose : (nonnull NSDictionary *)options resolver : (RCTPromiseResolveBlock)
                      resolve rejecter : (RCTPromiseRejectBlock)reject) {
    NSString *cryptorId = options[@"cryptorId"];
    NSMutableDictionary *entries = E2EEFrameCryptorEntries();
    E2EEFrameCryptorEntry *entry = entries[cryptorId];
    if (!entry) {
        reject(@"E2EE_DISPOSE_FAILED", @"Frame cryptor not found", nil);
        return;
    }

    // Dropping the entry releases our reference to the cryptor. If WebRTC
    // still has it attached the cryptor stays alive but detached (encryptors
    // pass frames through, decryptors drop frames) until WebRTC releases it.
    [entry detach];
    [entries removeObjectForKey:cryptorId];
    resolve(@(YES));
}

@end
