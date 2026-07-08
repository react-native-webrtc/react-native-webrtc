// AHardwareBuffer (AHB) -> EGLImage -> OES GL texture import + sync-fd GPU
// fence wait for the Android custom-video-track.
//
// libwebrtc (org.jitsi:webrtc) encodes from a GL texture, not an AHardwareBuffer.
// So per pooled AHB we build, ONCE, a GL_TEXTURE_EXTERNAL_OES texture that aliases
// the AHB's pixels (eglGetNativeClientBufferANDROID -> eglCreateImageKHR ->
// glEGLImageTargetTexture2DOES) and hand the texture id to a TextureBufferImpl on
// the Java side. Every subsequent frame reuses the cached {EGLImage, texId} for
// that index; we only re-wait the GPU fence and re-deliver.
//
// EVERY entry point in this file MUST be called on the one dedicated GL thread
// whose EGL context is current (the SurfaceTextureHelper handler thread created
// with WebRTC's root EGL context). Cross-thread GL is invalid and these calls
// silently corrupt state if run elsewhere. The Java side (CustomVideoFrameDelivery)
// guarantees this by posting every native call onto that handler.
//
// The functions used here are EGL/GLES *extensions*, not part of the core NDK
// link surface, so they are resolved at runtime via eglGetProcAddress:
//   - eglGetNativeClientBufferANDROID   (EGL_ANDROID_get_native_client_buffer)
//   - eglCreateImageKHR / eglDestroyImageKHR (EGL_KHR_image_base)
//   - glEGLImageTargetTexture2DOES      (GL_OES_EGL_image)
//   - eglCreateSyncKHR / eglClientWaitSyncKHR / eglDestroySyncKHR
//                                       (EGL_KHR_fence_sync + EGL_ANDROID_native_fence_sync)
//
// None of these are __INTRODUCED_IN-versioned NDK symbols (they are all resolved
// through eglGetProcAddress function pointers), so no __builtin_available guard is
// required here even when compiling against minSdk 24.

#include <android/hardware_buffer.h>
#include <android/log.h>
#include <jni.h>

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include <unistd.h>

namespace {

constexpr EGLTimeKHR kFenceWaitTimeoutNs = 2'000'000'000ULL;

// Lazily-resolved extension entry points. Resolved once on first use from the
// GL thread (where an EGL display/context are current). They are process-global
// function pointers, so a plain one-shot init is safe.
PFNEGLGETNATIVECLIENTBUFFERANDROIDPROC eglGetNativeClientBufferANDROIDFn = nullptr;
PFNEGLCREATEIMAGEKHRPROC eglCreateImageKHRFn = nullptr;
PFNEGLDESTROYIMAGEKHRPROC eglDestroyImageKHRFn = nullptr;
PFNGLEGLIMAGETARGETTEXTURE2DOESPROC glEGLImageTargetTexture2DOESFn = nullptr;
PFNEGLCREATESYNCKHRPROC eglCreateSyncKHRFn = nullptr;
PFNEGLCLIENTWAITSYNCKHRPROC eglClientWaitSyncKHRFn = nullptr;
PFNEGLDESTROYSYNCKHRPROC eglDestroySyncKHRFn = nullptr;
bool extensionsResolved = false;

// Resolves the EGL/GLES extension entry points used in this file. Like every
// entry point here it assumes the single shared-context GL thread (an EGL
// context current on it) and is NOT thread-safe off it: the function-pointer
// globals and the one-shot `extensionsResolved` guard are written without
// synchronisation, so calling this (or any function below) from another thread
// races and corrupts GL/EGL state.
bool resolveExtensions() {
    if (extensionsResolved) {
        return eglGetNativeClientBufferANDROIDFn != nullptr && eglCreateImageKHRFn != nullptr &&
                glEGLImageTargetTexture2DOESFn != nullptr;
    }
    extensionsResolved = true;

    eglGetNativeClientBufferANDROIDFn = reinterpret_cast<PFNEGLGETNATIVECLIENTBUFFERANDROIDPROC>(
            eglGetProcAddress("eglGetNativeClientBufferANDROID"));
    eglCreateImageKHRFn =
            reinterpret_cast<PFNEGLCREATEIMAGEKHRPROC>(eglGetProcAddress("eglCreateImageKHR"));
    eglDestroyImageKHRFn =
            reinterpret_cast<PFNEGLDESTROYIMAGEKHRPROC>(eglGetProcAddress("eglDestroyImageKHR"));
    glEGLImageTargetTexture2DOESFn = reinterpret_cast<PFNGLEGLIMAGETARGETTEXTURE2DOESPROC>(
            eglGetProcAddress("glEGLImageTargetTexture2DOES"));
    eglCreateSyncKHRFn =
            reinterpret_cast<PFNEGLCREATESYNCKHRPROC>(eglGetProcAddress("eglCreateSyncKHR"));
    eglClientWaitSyncKHRFn = reinterpret_cast<PFNEGLCLIENTWAITSYNCKHRPROC>(
            eglGetProcAddress("eglClientWaitSyncKHR"));
    eglDestroySyncKHRFn =
            reinterpret_cast<PFNEGLDESTROYSYNCKHRPROC>(eglGetProcAddress("eglDestroySyncKHR"));

    return eglGetNativeClientBufferANDROIDFn != nullptr && eglCreateImageKHRFn != nullptr &&
            glEGLImageTargetTexture2DOESFn != nullptr;
}

}  // namespace

extern "C" {

// Imports the AHardwareBuffer at `ahbHandle` (an AHardwareBuffer* as a jlong) into
// a freshly-created GL_TEXTURE_EXTERNAL_OES texture that aliases its pixels, and
// returns the {eglImage, texId} packed for the Java cache.
//
// Returns a jlongArray of length 2: [0] = EGLImageKHR (as jlong, for later
// destroy), [1] = GLuint texture id (as jlong, fed to TextureBufferImpl). On any
// failure returns null. MUST run on the shared-context GL thread.
//
// Caching is the caller's responsibility: call this exactly once per pool index
// and reuse the returned {eglImage, texId} for every frame at that index.
JNIEXPORT jlongArray JNICALL
Java_com_oney_WebRTCModule_CustomVideoFrameDelivery_nativeImportAhbToOesTexture(
        JNIEnv* env, jclass /* clazz */, jlong ahbHandle) {
    if (ahbHandle == 0) {
        return nullptr;
    }
    if (!resolveExtensions()) {
        return nullptr;
    }

    // A current EGL context is required: the SurfaceTextureHelper handler thread
    // makes the shared context current on itself, so this is non-null only when we
    // are actually on the GL thread. Both are needed (display for eglCreateImageKHR,
    // context for the GL texture calls).
    if (eglGetCurrentContext() == EGL_NO_CONTEXT) {
        return nullptr;
    }
    EGLDisplay display = eglGetCurrentDisplay();
    if (display == EGL_NO_DISPLAY) {
        return nullptr;
    }

    AHardwareBuffer* buffer = reinterpret_cast<AHardwareBuffer*>(ahbHandle);
    EGLClientBuffer clientBuffer = eglGetNativeClientBufferANDROIDFn(buffer);
    if (clientBuffer == nullptr) {
        return nullptr;
    }

    // No attribs: an AHB-backed EGLImage aliases the buffer's memory directly, so
    // there are no separate contents to preserve (EGL_IMAGE_PRESERVED_KHR buys
    // nothing here, and strict drivers can fail creation over unsupported attribs).
    const EGLint imageAttribs[] = {EGL_NONE};
    EGLImageKHR eglImage = eglCreateImageKHRFn(display, EGL_NO_CONTEXT, EGL_NATIVE_BUFFER_ANDROID,
                                               clientBuffer, imageAttribs);
    if (eglImage == EGL_NO_IMAGE_KHR) {
        return nullptr;
    }

    // GL error state is sticky and shared per context: drain anything a previous
    // user of this thread's context left behind, so the check below reflects only
    // this import.
    while (glGetError() != GL_NO_ERROR) {
    }

    GLuint texId = 0;
    glGenTextures(1, &texId);
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, texId);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glEGLImageTargetTexture2DOESFn(GL_TEXTURE_EXTERNAL_OES,
                                   static_cast<GLeglImageOES>(eglImage));
    GLenum glError = glGetError();
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);
    if (glError != GL_NO_ERROR) {
        glDeleteTextures(1, &texId);
        eglDestroyImageKHRFn(display, eglImage);
        return nullptr;
    }

    jlongArray result = env->NewLongArray(2);
    if (result == nullptr) {
        glDeleteTextures(1, &texId);
        eglDestroyImageKHRFn(display, eglImage);
        return nullptr;
    }
    jlong values[2] = {reinterpret_cast<jlong>(eglImage), static_cast<jlong>(texId)};
    env->SetLongArrayRegion(result, 0, 2, values);
    return result;
}

// Client (CPU) wait on the glHandler delivery thread for the GPU fence behind
// `fenceFd` (a dup'd sync-fd file descriptor, or -1 for no fence). Blocks THIS
// thread (the GL delivery thread, NOT the JS thread) until the WebGPU render that
// produced the AHB contents has fully completed, BEFORE onFrameCaptured hands the
// OES texture to any encoder.
//
// WHY a client wait and not a server-side eglWaitSyncKHR: WebRTC's hardware
// encoder samples the imported GL_TEXTURE_EXTERNAL_OES on ITS OWN EGL
// context/thread (a separate context in the share group). A server-side wait
// (eglWaitSyncKHR) only orders subsequent GPU work on the CURRENT (glHandler)
// context, so it would NOT gate the encoder's context — under GPU load the
// encoder could sample a half-rendered AHardwareBuffer (tearing). A client wait
// blocks the delivery thread until the fence signals, so the render is provably
// complete before the texture reaches ANY encoder context. It does NOT copy
// pixels (zero-copy preserved); it is purely a sync barrier.
//
// EGL takes ownership of the fd inside eglCreateSyncKHR (react-native-webgpu
// dup()s the sync-fd before handing it over, so EGL owning + closing it is
// correct — no double-close). On success EGL closes the fd when the sync is
// destroyed; on failure ownership stays with us, so we close it ourselves.
//
// MUST run on the shared-context GL thread (the same one that will sample the
// texture). fenceFd < 0 means no fence supplied -> no-op (deliver immediately,
// accepting the render may not be finished).
JNIEXPORT jboolean JNICALL
Java_com_oney_WebRTCModule_CustomVideoFrameDelivery_nativeWaitSyncFd(
        JNIEnv* /* env */, jclass /* clazz */, jint fenceFd) {
    if (fenceFd < 0) {
        return JNI_TRUE;  // no-fence fallback
    }
    if (!resolveExtensions() || eglCreateSyncKHRFn == nullptr ||
            eglClientWaitSyncKHRFn == nullptr) {
        close(fenceFd);
        return JNI_FALSE;
    }

    EGLDisplay display = eglGetCurrentDisplay();
    if (display == EGL_NO_DISPLAY) {
        close(fenceFd);
        return JNI_FALSE;
    }

    // EGL takes ownership of the fd on success and closes it when the sync is
    // destroyed; on failure ownership stays with us, so we close it ourselves.
    const EGLint syncAttribs[] = {EGL_SYNC_NATIVE_FENCE_FD_ANDROID, fenceFd, EGL_NONE};
    EGLSyncKHR sync = eglCreateSyncKHRFn(display, EGL_SYNC_NATIVE_FENCE_ANDROID, syncAttribs);
    if (sync == EGL_NO_SYNC_KHR) {
        close(fenceFd);
        return JNI_FALSE;
    }

    // Client (CPU) wait: blocks this delivery thread until the producer fence
    // signals, but only up to kFenceWaitTimeoutNs so teardown cannot hang
    // forever on a bad or never-signaled fence. Timed-out frames are dropped.
    EGLint waitStatus =
            eglClientWaitSyncKHRFn(display, sync, EGL_SYNC_FLUSH_COMMANDS_BIT_KHR, kFenceWaitTimeoutNs);
    if (eglDestroySyncKHRFn != nullptr) {
        eglDestroySyncKHRFn(display, sync);
    }
    return waitStatus == EGL_CONDITION_SATISFIED_KHR ? JNI_TRUE : JNI_FALSE;
}

// Destroys one cached {EGLImage, texId} pair created by nativeImportAhbToOesTexture.
// MUST run on the shared-context GL thread (glDeleteTextures needs the context;
// eglDestroyImageKHR needs the display). No-op on zero handles.
JNIEXPORT void JNICALL
Java_com_oney_WebRTCModule_CustomVideoFrameDelivery_nativeReleaseImportedTexture(
        JNIEnv* /* env */, jclass /* clazz */, jlong eglImageHandle, jint texId) {
    // Same precondition as the import: without a current context glDeleteTextures
    // is a silent no-op and the texture would leak in the share group. Make a
    // mis-threaded (or post-teardown) call visible instead of silent.
    if (eglGetCurrentContext() == EGL_NO_CONTEXT) {
        __android_log_print(ANDROID_LOG_WARN, "WebRTCModule",
                            "nativeReleaseImportedTexture called without a current EGL context; leaking texture %d",
                            texId);
        return;
    }
    EGLDisplay display = eglGetCurrentDisplay();
    if (texId != 0) {
        GLuint id = static_cast<GLuint>(texId);
        glDeleteTextures(1, &id);
    }
    if (eglImageHandle != 0 && display != EGL_NO_DISPLAY && resolveExtensions() &&
            eglDestroyImageKHRFn != nullptr) {
        eglDestroyImageKHRFn(display, reinterpret_cast<EGLImageKHR>(eglImageHandle));
    }
}

// Closes a raw fence fd that never reached EGL ownership (Java error/bailout
// paths). Touches no GL/EGL state, so it is safe to call from any thread. No-op
// on a negative fd.
JNIEXPORT void JNICALL
Java_com_oney_WebRTCModule_CustomVideoFrameDelivery_nativeCloseFd(
        JNIEnv* /* env */, jclass /* clazz */, jint fenceFd) {
    if (fenceFd >= 0) {
        close(fenceFd);
    }
}

// --- Forwarding-mode AHardwareBuffer ref-counting / describe ---
//
// Unlike the pooled path (whose AHBs are owned by CustomVideoBufferPool), a
// forwarded frame hands us an app-owned AHardwareBuffer* that the caller releases
// right after the push returns. We take an owning reference before returning and
// balance it once the delivered VideoFrame is released. These three functions
// touch NO GL/EGL state (only AHardwareBuffer refcounts / metadata), so they are
// safe off the GL thread — nativeAcquireAhb in particular is called synchronously
// on the worklet push thread. The AHardwareBuffer_* APIs are __INTRODUCED_IN(26),
// so each is guarded by __builtin_available; callers reject on SDK_INT < 26.

// Takes one owning reference on a forwarded AHB so it outlives the caller's own
// release. No-op on 0. MUST be balanced by exactly one nativeReleaseAhb.
JNIEXPORT void JNICALL
Java_com_oney_WebRTCModule_CustomVideoFrameDelivery_nativeAcquireAhb(
        JNIEnv* /* env */, jclass /* clazz */, jlong ahbHandle) {
    if (ahbHandle == 0) {
        return;
    }
    if (__builtin_available(android 26, *)) {
        AHardwareBuffer_acquire(reinterpret_cast<AHardwareBuffer*>(ahbHandle));
    }
}

// Releases one reference taken by nativeAcquireAhb. Thread-safe; no-op on 0.
JNIEXPORT void JNICALL
Java_com_oney_WebRTCModule_CustomVideoFrameDelivery_nativeReleaseAhb(
        JNIEnv* /* env */, jclass /* clazz */, jlong ahbHandle) {
    if (ahbHandle == 0) {
        return;
    }
    if (__builtin_available(android 26, *)) {
        AHardwareBuffer_release(reinterpret_cast<AHardwareBuffer*>(ahbHandle));
    }
}

// Reads a forwarded AHB's pixel dimensions (external buffers carry their own size,
// unlike pooled buffers whose geometry is known up front). Returns a jintArray
// {width, height}, or null on failure / zero size.
JNIEXPORT jintArray JNICALL
Java_com_oney_WebRTCModule_CustomVideoFrameDelivery_nativeDescribeAhb(
        JNIEnv* env, jclass /* clazz */, jlong ahbHandle) {
    if (ahbHandle == 0) {
        return nullptr;
    }
    if (__builtin_available(android 26, *)) {
        AHardwareBuffer_Desc desc = {};
        AHardwareBuffer_describe(reinterpret_cast<AHardwareBuffer*>(ahbHandle), &desc);
        if (desc.width == 0 || desc.height == 0) {
            return nullptr;
        }
        jintArray result = env->NewIntArray(2);
        if (result == nullptr) {
            return nullptr;
        }
        jint dimensions[2] = {static_cast<jint>(desc.width), static_cast<jint>(desc.height)};
        env->SetIntArrayRegion(result, 0, 2, dimensions);
        return result;
    }
    return nullptr;
}

}  // extern "C"
