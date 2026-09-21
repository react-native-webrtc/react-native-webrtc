// Stateless trampolines implementing the WebRTC FrameEncryptorInterface /
// FrameDecryptorInterface ABI. All cryptor state (key ring, frame counter,
// enabled flag) and the AES-128-GCM crypto itself live in Java
// (E2EEFrameCryptorManager); these objects only ferry frames back and forth
// through JNI.

#include <jni.h>

#include <atomic>
#include <mutex>

#include "E2EEFrameCryptorABI.h"

namespace {

// GCM tag (16) + IV (12) + trailer (2). Must match the Java side.
constexpr size_t kFrameCryptorOverhead = 30;

JavaVM *g_jvm = nullptr;
jclass g_manager_class = nullptr;
jmethodID g_encrypt_frame_method = nullptr;
jmethodID g_decrypt_frame_method = nullptr;
std::mutex g_jni_init_mutex;

// Caches the global class reference and method IDs of
// com.oney.WebRTCModule.E2EEFrameCryptorManager. Must be called from a Java
// thread (so that FindClass uses the application class loader).
bool EnsureManagerClass(JNIEnv *env) {
    std::lock_guard<std::mutex> lock(g_jni_init_mutex);
    if (g_manager_class != nullptr) {
        return true;
    }
    jclass local = env->FindClass("com/oney/WebRTCModule/E2EEFrameCryptorManager");
    if (local == nullptr) {
        return false;
    }
    g_manager_class = static_cast<jclass>(env->NewGlobalRef(local));
    env->DeleteLocalRef(local);
    g_encrypt_frame_method = env->GetStaticMethodID(g_manager_class, "encryptFrame", "(JII[B)[B");
    g_decrypt_frame_method = env->GetStaticMethodID(g_manager_class, "decryptFrame", "(JI[B)[B");
    if (g_encrypt_frame_method == nullptr || g_decrypt_frame_method == nullptr) {
        env->DeleteGlobalRef(g_manager_class);
        g_manager_class = nullptr;
        return false;
    }
    return true;
}

// Returns a JNIEnv for the current thread, attaching it to the JVM first if
// needed. *did_attach is set to true when this call attached the thread (the
// caller must detach it when done).
JNIEnv *GetEnv(bool *did_attach) {
    *did_attach = false;
    JNIEnv *env = nullptr;
    jint result = g_jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    if (result == JNI_EDETACHED) {
        if (g_jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            return nullptr;
        }
        *did_attach = true;
        return env;
    }
    if (result != JNI_OK) {
        return nullptr;
    }
    return env;
}

// Calls the static byte[]-returning method of E2EEFrameCryptorManager
// (encryptFrame for encryptors, decryptFrame for decryptors) and copies the
// result into out.
//
// Returns the number of bytes written to out, or -1 if the frame must be
// dropped (null result, exception, or result larger than the output buffer).
int CallFrameMethod(JNIEnv *env,
                    jmethodID method,
                    jlong handle,
                    jint media_type,
                    jint ssrc,
                    bool is_encrypt,
                    const uint8_t *frame_data,
                    size_t frame_size,
                    uint8_t *out_data,
                    size_t out_size) {
    jbyteArray jframe = env->NewByteArray(static_cast<jsize>(frame_size));
    if (jframe == nullptr) {
        // Pending OutOfMemoryError; the frame is dropped either way.
        env->ExceptionClear();
        return -1;
    }
    env->SetByteArrayRegion(jframe, 0, static_cast<jsize>(frame_size), reinterpret_cast<const jbyte *>(frame_data));

    jbyteArray jresult;
    if (is_encrypt) {
        jresult = static_cast<jbyteArray>(
            env->CallStaticObjectMethod(g_manager_class, method, handle, media_type, ssrc, jframe));
    } else {
        jresult =
            static_cast<jbyteArray>(env->CallStaticObjectMethod(g_manager_class, method, handle, media_type, jframe));
    }
    env->DeleteLocalRef(jframe);

    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return -1;
    }
    if (jresult == nullptr) {
        return -1;
    }
    jsize result_size = env->GetArrayLength(jresult);
    if (result_size < 0 || static_cast<size_t>(result_size) > out_size) {
        env->DeleteLocalRef(jresult);
        return -1;
    }
    env->GetByteArrayRegion(jresult, 0, result_size, reinterpret_cast<jbyte *>(out_data));
    env->DeleteLocalRef(jresult);
    return static_cast<int>(result_size);
}

class AesGcmFrameEncryptor final : public webrtc::FrameEncryptorInterface {
   public:
    explicit AesGcmFrameEncryptor(jlong java_handle) : java_handle_(java_handle), ref_count_(0) {}

    void AddRef() const override { ref_count_.fetch_add(1, std::memory_order_relaxed); }

    webrtc::RefCountReleaseStatus Release() const override {
        if (ref_count_.fetch_sub(1, std::memory_order_acq_rel) == 1) {
            delete this;
            return webrtc::RefCountReleaseStatus::kDroppedLastRef;
        }
        return webrtc::RefCountReleaseStatus::kOtherRefsRemained;
    }

    int Encrypt(cricket::MediaType media_type,
                uint32_t ssrc,
                rtc::ArrayView<const uint8_t> /* additional_data */,
                rtc::ArrayView<const uint8_t> frame,
                rtc::ArrayView<uint8_t> encrypted_frame,
                size_t *bytes_written) override {
        *bytes_written = 0;

        bool did_attach = false;
        JNIEnv *env = GetEnv(&did_attach);
        if (env == nullptr || g_manager_class == nullptr) {
            if (did_attach) {
                g_jvm->DetachCurrentThread();
            }
            return 1;
        }

        int result = CallFrameMethod(env,
                                     g_encrypt_frame_method,
                                     java_handle_,
                                     static_cast<jint>(media_type),
                                     static_cast<jint>(ssrc),
                                     /* is_encrypt */ true,
                                     frame.data(),
                                     frame.size(),
                                     encrypted_frame.data(),
                                     encrypted_frame.size());
        if (did_attach) {
            g_jvm->DetachCurrentThread();
        }
        if (result < 0) {
            return 1;
        }
        *bytes_written = static_cast<size_t>(result);
        return 0;
    }

    size_t GetMaxCiphertextByteSize(cricket::MediaType /* media_type */, size_t frame_size) override {
        return frame_size + kFrameCryptorOverhead;
    }

   private:
    const jlong java_handle_;
    mutable std::atomic<int> ref_count_;
};

class AesGcmFrameDecryptor final : public webrtc::FrameDecryptorInterface {
   public:
    explicit AesGcmFrameDecryptor(jlong java_handle) : java_handle_(java_handle), ref_count_(0) {}

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
        bool did_attach = false;
        JNIEnv *env = GetEnv(&did_attach);
        if (env == nullptr || g_manager_class == nullptr) {
            if (did_attach) {
                g_jvm->DetachCurrentThread();
            }
            return Result(Status::kRecoverable, 0);
        }

        int result = CallFrameMethod(env,
                                     g_decrypt_frame_method,
                                     java_handle_,
                                     static_cast<jint>(media_type),
                                     /* ssrc */ 0,
                                     /* is_encrypt */ false,
                                     encrypted_frame.data(),
                                     encrypted_frame.size(),
                                     frame.data(),
                                     frame.size());
        if (did_attach) {
            g_jvm->DetachCurrentThread();
        }
        if (result < 0) {
            return Result(Status::kRecoverable, 0);
        }
        return Result(Status::kOk, static_cast<size_t>(result));
    }

    size_t GetMaxPlaintextByteSize(cricket::MediaType /* media_type */, size_t encrypted_frame_size) override {
        return encrypted_frame_size;
    }

   private:
    const jlong java_handle_;
    mutable std::atomic<int> ref_count_;
};

}  // namespace

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void * /* reserved */) {
    g_jvm = vm;
    return JNI_VERSION_1_6;
}

JNIEXPORT jlong JNICALL Java_com_oney_WebRTCModule_E2EEFrameCryptorManager_nativeCreateEncryptor(JNIEnv *env,
                                                                                                 jclass /* clazz */,
                                                                                                 jlong handle) {
    if (!EnsureManagerClass(env)) {
        return 0;
    }
    AesGcmFrameEncryptor *cryptor = new AesGcmFrameEncryptor(handle);
    // Hold one reference on behalf of the Java CryptorEntry; it is dropped by
    // nativeDestroyCryptor(). WebRTC takes its own references when the cryptor
    // is attached to an RtpSender.
    cryptor->AddRef();
    return reinterpret_cast<jlong>(cryptor);
}

JNIEXPORT jlong JNICALL Java_com_oney_WebRTCModule_E2EEFrameCryptorManager_nativeCreateDecryptor(JNIEnv *env,
                                                                                                 jclass /* clazz */,
                                                                                                 jlong handle) {
    if (!EnsureManagerClass(env)) {
        return 0;
    }
    AesGcmFrameDecryptor *cryptor = new AesGcmFrameDecryptor(handle);
    cryptor->AddRef();
    return reinterpret_cast<jlong>(cryptor);
}

JNIEXPORT void JNICALL Java_com_oney_WebRTCModule_E2EEFrameCryptorManager_nativeDestroyCryptor(JNIEnv * /* env */,
                                                                                               jclass /* clazz */,
                                                                                               jlong native_cryptor) {
    webrtc::RefCountInterface *cryptor = reinterpret_cast<webrtc::RefCountInterface *>(native_cryptor);
    if (cryptor != nullptr) {
        cryptor->Release();
    }
}

}  // extern "C"
