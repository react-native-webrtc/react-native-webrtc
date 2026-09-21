package com.oney.WebRTCModule;

import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;

import org.webrtc.FrameDecryptor;
import org.webrtc.FrameEncryptor;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Manages end-to-end encrypted frame cryptors (AES-128-GCM) attached to
 * RtpSenders and RtpReceivers.
 *
 * All cryptor state (key ring, frame counter, enabled flag) lives here in
 * Java. The native side (see android/src/main/cpp) is a stateless trampoline
 * implementing the WebRTC FrameEncryptorInterface / FrameDecryptorInterface
 * ABI which calls back into {@link #encryptFrame} and {@link #decryptFrame}
 * for every frame.
 *
 * Wire format (byte-identical with the iOS implementation):
 *
 *   header || AES-128-GCM(payload) || tag(16) || IV(12) || trailer(2)
 *
 * The header (1 byte for audio, 3/10 bytes for video) is left in plaintext
 * and used as the GCM additional authenticated data. The IV is the SSRC
 * (4 bytes, big-endian) concatenated with a per-cryptor monotonic frame
 * counter (8 bytes, big-endian). The trailer holds the IV length and the
 * key index.
 */
public class E2EEFrameCryptorManager {
    private static final String TAG = "E2EEFrameCryptor";

    private static final int AES_KEY_SIZE = 16;
    private static final int KEY_RING_SIZE = 16;
    private static final int GCM_TAG_SIZE = 16;
    private static final int GCM_TAG_LENGTH_BITS = GCM_TAG_SIZE * 8;
    private static final int IV_LENGTH = 12;
    private static final int TRAILER_LENGTH = 2;

    // cricket::MediaType values from the WebRTC C++ API.
    private static final int MEDIA_TYPE_AUDIO = 0;

    private static final String NATIVE_LIBRARY_NAME = "jitsie2eeframecryptor";

    // Cryptors keyed by the ID handed out to JS.
    private static final Map<String, CryptorEntry> cryptors = new ConcurrentHashMap<>();
    // Cryptors keyed by the handle embedded in the native trampoline, used
    // by the native -> Java per-frame callbacks.
    private static final Map<Long, CryptorEntry> cryptorsByHandle = new ConcurrentHashMap<>();

    private static final AtomicLong nextHandle = new AtomicLong(1);

    private final WebRTCModule webRTCModule;

    public E2EEFrameCryptorManager(WebRTCModule webRTCModule) {
        this.webRTCModule = webRTCModule;
    }

    private static native long nativeCreateEncryptor(long handle);
    private static native long nativeCreateDecryptor(long handle);
    private static native void nativeDestroyCryptor(long nativeCryptor);

    private static boolean nativeLibraryLoaded = false;
    private static UnsatisfiedLinkError nativeLibraryError = null;

    /**
     * Loads the native trampoline library on first use. Done lazily (rather
     * than in a static initializer) so that a broken native package only
     * affects E2EE calls and never breaks regular WebRTC usage.
     */
    private static synchronized void loadNativeLibrary() throws UnsatisfiedLinkError {
        if (nativeLibraryLoaded) {
            return;
        }
        if (nativeLibraryError != null) {
            throw nativeLibraryError;
        }
        try {
            Log.d(TAG, "Loading library: " + NATIVE_LIBRARY_NAME);
            System.loadLibrary(NATIVE_LIBRARY_NAME);
            nativeLibraryLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load " + NATIVE_LIBRARY_NAME, e);
            nativeLibraryError = e;
            throw e;
        }
    }

    public void createFrameCryptor(ReadableMap options, Promise promise) {
        try {
            loadNativeLibrary();

            int peerConnectionId = options.getInt("peerConnectionId");
            String type = options.getString("type");
            String rtpId = options.getString("rtpId");

            PeerConnectionObserver pco = webRTCModule.getPeerConnectionObserver(peerConnectionId);
            if (pco == null || pco.getPeerConnection() == null) {
                promise.reject("E2EE_CREATE_FAILED", "PeerConnection not found");
                return;
            }

            long handle = nextHandle.getAndIncrement();
            CryptorEntry entry;

            if ("sender".equals(type)) {
                RtpSender sender = pco.getSender(rtpId);
                if (sender == null) {
                    promise.reject("E2EE_CREATE_FAILED", "RtpSender not found: " + rtpId);
                    return;
                }
                long nativeCryptor = nativeCreateEncryptor(handle);
                if (nativeCryptor == 0) {
                    promise.reject("E2EE_CREATE_FAILED", "Failed to create native frame encryptor");
                    return;
                }
                entry = new CryptorEntry(handle, nativeCryptor, true);
                sender.setFrameEncryptor(entry.encryptor);
            } else if ("receiver".equals(type)) {
                RtpReceiver receiver = pco.getReceiver(rtpId);
                if (receiver == null) {
                    promise.reject("E2EE_CREATE_FAILED", "RtpReceiver not found: " + rtpId);
                    return;
                }
                long nativeCryptor = nativeCreateDecryptor(handle);
                if (nativeCryptor == 0) {
                    promise.reject("E2EE_CREATE_FAILED", "Failed to create native frame decryptor");
                    return;
                }
                entry = new CryptorEntry(handle, nativeCryptor, false);
                receiver.setFrameDecryptor(entry.decryptor);
            } else {
                promise.reject("E2EE_CREATE_FAILED", "type must be 'sender' or 'receiver'");
                return;
            }

            String cryptorId = UUID.randomUUID().toString();
            cryptors.put(cryptorId, entry);
            cryptorsByHandle.put(handle, entry);

            promise.resolve(cryptorId);
        } catch (Exception | UnsatisfiedLinkError e) {
            promise.reject("E2EE_CREATE_FAILED", e.getMessage());
        }
    }

    /**
     * Derives a 16-byte AES key from the passphrase (first 16 bytes of
     * SHA-256 of its UTF-8 encoding) and stores it in the given key ring
     * slot. For senders this also makes it the current send key.
     */
    public void setKey(ReadableMap options, Promise promise) {
        CryptorEntry entry = cryptors.get(options.getString("cryptorId"));
        if (entry == null) {
            promise.reject("E2EE_SET_KEY_FAILED", "Frame cryptor not found");
            return;
        }

        int keyIndex = options.getInt("keyIndex") & (KEY_RING_SIZE - 1);
        String key = options.getString("key");

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec keySpec = new SecretKeySpec(hash, 0, AES_KEY_SIZE, "AES");

            synchronized (entry.keyRing) {
                entry.keyRing[keyIndex] = keySpec;
                if (entry.isSender) {
                    entry.sendKeyIndex = keyIndex;
                }
            }

            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("E2EE_SET_KEY_FAILED", e.getMessage());
        }
    }

    public void setEnabled(ReadableMap options, Promise promise) {
        CryptorEntry entry = cryptors.get(options.getString("cryptorId"));
        if (entry == null) {
            promise.reject("E2EE_SET_ENABLED_FAILED", "Frame cryptor not found");
            return;
        }

        entry.enabled = options.getBoolean("enabled");

        promise.resolve(true);
    }

    public void dispose(ReadableMap options, Promise promise) {
        String cryptorId = options.getString("cryptorId");
        CryptorEntry entry = cryptors.remove(cryptorId);
        if (entry == null) {
            promise.reject("E2EE_DISPOSE_FAILED", "Frame cryptor not found");
            return;
        }

        cryptorsByHandle.remove(entry.handle);

        // Drops the reference held by the entry. If WebRTC still has the
        // cryptor attached the native object stays alive (encryptors pass
        // frames through unchanged, decryptors drop frames, since the entry
        // is gone) until WebRTC releases it.
        nativeDestroyCryptor(entry.nativeCryptor);

        promise.resolve(true);
    }

    /**
     * Called from the native encryptor trampoline for every outgoing frame.
     *
     * @return the fully formatted output frame, or null if the frame must be
     * dropped.
     */
    static byte[] encryptFrame(long handle, int mediaType, int ssrc, byte[] frame) {
        CryptorEntry entry = cryptorsByHandle.get(handle);
        if (entry == null || !entry.enabled) {
            return frame;
        }

        int headerLength = getHeaderLength(mediaType, frame);
        if (frame.length < headerLength) {
            return frame;
        }

        SecretKeySpec key;
        int keyIndex;
        synchronized (entry.keyRing) {
            keyIndex = entry.sendKeyIndex;
            key = entry.keyRing[keyIndex];
        }
        if (key == null) {
            return null;
        }

        try {
            long counter = entry.frameCounter.getAndIncrement();
            byte[] iv = new byte[IV_LENGTH];
            writeUint32BE(iv, 0, ssrc);
            writeUint64BE(iv, 4, counter);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            cipher.updateAAD(frame, 0, headerLength);
            byte[] ciphertextAndTag = cipher.doFinal(frame, headerLength, frame.length - headerLength);

            byte[] out = new byte[headerLength + ciphertextAndTag.length + IV_LENGTH + TRAILER_LENGTH];
            System.arraycopy(frame, 0, out, 0, headerLength);
            System.arraycopy(ciphertextAndTag, 0, out, headerLength, ciphertextAndTag.length);
            System.arraycopy(iv, 0, out, headerLength + ciphertextAndTag.length, IV_LENGTH);
            out[out.length - 2] = (byte) IV_LENGTH;
            out[out.length - 1] = (byte) keyIndex;
            return out;
        } catch (GeneralSecurityException e) {
            Log.w(TAG, "encryptFrame: encryption failed", e);
            return null;
        }
    }

    /**
     * Called from the native decryptor trampoline for every incoming frame.
     *
     * @return the plaintext frame, or null if the frame must be dropped.
     */
    static byte[] decryptFrame(long handle, int mediaType, byte[] frame) {
        CryptorEntry entry = cryptorsByHandle.get(handle);
        if (entry == null) {
            // Disposed (or never registered): drop the frame, since the
            // remote side is presumably still sending encrypted frames.
            return null;
        }
        if (!entry.enabled) {
            return frame;
        }

        int headerLength = getHeaderLength(mediaType, frame);
        if (frame.length < headerLength) {
            return frame;
        }

        if (frame.length < headerLength + GCM_TAG_SIZE + IV_LENGTH + TRAILER_LENGTH
                || (frame[frame.length - 2] & 0xFF) != IV_LENGTH) {
            return null;
        }

        int keyIndex = frame[frame.length - 1] & (KEY_RING_SIZE - 1);
        SecretKeySpec key;
        synchronized (entry.keyRing) {
            key = entry.keyRing[keyIndex];
        }
        if (key == null) {
            return null;
        }

        try {
            byte[] iv =
                    Arrays.copyOfRange(frame, frame.length - IV_LENGTH - TRAILER_LENGTH, frame.length - TRAILER_LENGTH);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            cipher.updateAAD(frame, 0, headerLength);
            byte[] payload =
                    cipher.doFinal(frame, headerLength, frame.length - headerLength - IV_LENGTH - TRAILER_LENGTH);

            byte[] out = new byte[headerLength + payload.length];
            System.arraycopy(frame, 0, out, 0, headerLength);
            System.arraycopy(payload, 0, out, headerLength, payload.length);
            return out;
        } catch (GeneralSecurityException e) {
            // Includes AEADBadTagException (GCM authentication failure):
            // drop the frame silently.
            return null;
        }
    }

    private static int getHeaderLength(int mediaType, byte[] frame) {
        if (mediaType == MEDIA_TYPE_AUDIO) {
            // Opus TOC byte.
            return 1;
        }
        // Video: VP8 keyframes keep a longer unencrypted header.
        if (frame.length > 0 && (frame[0] & 0x01) == 0) {
            return 10;
        }
        return 3;
    }

    private static void writeUint32BE(byte[] out, int offset, int value) {
        out[offset] = (byte) (value >>> 24);
        out[offset + 1] = (byte) (value >>> 16);
        out[offset + 2] = (byte) (value >>> 8);
        out[offset + 3] = (byte) value;
    }

    private static void writeUint64BE(byte[] out, int offset, long value) {
        for (int i = 0; i < 8; i++) {
            out[offset + i] = (byte) (value >>> (56 - i * 8));
        }
    }

    private static class CryptorEntry {
        final long handle;
        final long nativeCryptor;
        final boolean isSender;
        final FrameEncryptor encryptor;
        final FrameDecryptor decryptor;
        final SecretKeySpec[] keyRing = new SecretKeySpec[KEY_RING_SIZE];
        final AtomicLong frameCounter = new AtomicLong(new SecureRandom().nextInt() & 0xFFFFFFFFL);
        volatile int sendKeyIndex;
        volatile boolean enabled = true;

        CryptorEntry(long handle, long nativeCryptor, boolean isSender) {
            this.handle = handle;
            this.nativeCryptor = nativeCryptor;
            this.isSender = isSender;
            this.encryptor = isSender ? () -> nativeCryptor : null;
            this.decryptor = isSender ? null : () -> nativeCryptor;
        }
    }
}
