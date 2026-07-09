#include "FJVideoPushInstaller.h"

// facebook::jni::ThreadScope is declared in <fbjni/detail/Environment.h>, pulled
// in transitively by <fbjni/fbjni.h> (included from FJVideoPushInstaller.h).

namespace jni = facebook::jni;

namespace fishjam {

FJVideoPushInstaller::FJVideoPushInstaller(jni::alias_ref<jhybridobject> javaThis,
                                           std::shared_ptr<FJVideoPush> push)
    : javaPart_(jni::make_global(javaThis)), push_(std::move(push)) {
    // Route every JS-pushed frame back to the Java peer, which forwards it to the
    // matching CustomVideoFrameDelivery. `nativeBuffer` is the forwarding
    // discriminator: non-zero => a finished AHardwareBuffer* to forward
    // (bufferIndex/fence unused); zero => pooled delivery of `bufferIndex` with the
    // sync-fd carried in fenceHandle (fenceSignaledValue unused on Android — the fd
    // already encodes the signal — but forwarded for parity with the shared
    // DeliverFn contract).
    //
    // deliver_ now runs on whatever thread called sink.push. For a VisionCamera
    // frame-processor that is a worklet thread NOT attached to the JVM, so we
    // establish a ThreadScope (attach the current thread for the duration of the
    // JNI dispatch) before touching any JNIEnv. ThreadScope is a no-op on threads
    // already attached (e.g. the RN JS thread), so the pooled path pays nothing.
    // Capturing the global_ref keeps the Java peer alive for the callback lifetime.
    auto javaPart = javaPart_;
    push_->setDeliver([javaPart](const std::string &trackId, int bufferIndex, uint64_t nativeBuffer,
                                 uint64_t timestampNs, int rotation, uint64_t fenceHandle,
                                 uint64_t fenceSignaledValue) {
        facebook::jni::ThreadScope threadScope;
        static const auto deliverFrame =
            javaPart->getClass()
                ->getMethod<void(jni::alias_ref<jstring>, jint, jlong, jlong, jint, jlong, jlong)>(
                    "deliverFrame");
        deliverFrame(javaPart, jni::make_jstring(trackId), static_cast<jint>(bufferIndex),
                     static_cast<jlong>(nativeBuffer), static_cast<jlong>(timestampNs),
                     static_cast<jint>(rotation), static_cast<jlong>(fenceHandle),
                     static_cast<jlong>(fenceSignaledValue));
    });
}

jni::local_ref<FJVideoPushInstaller::jhybriddata> FJVideoPushInstaller::initHybrid(
    jni::alias_ref<jhybridobject> javaThis,
    jni::alias_ref<facebook::react::CallInvokerHolder::javaobject> callInvokerHolder) {
    // FJVideoPush only needs the JS CallInvoker; it acquires the jsi::Runtime
    // itself inside invokeAsync, so no runtime pointer is required here.
    auto callInvoker = callInvokerHolder->cthis()->getCallInvoker();
    return makeCxxInstance(javaThis, std::make_shared<FJVideoPush>(callInvoker));
}

void FJVideoPushInstaller::installPush() {
    // FJVideoPush::install sets the global on the JS thread and then runs this
    // callback (also on the JS thread). We notify the Java peer there so the
    // Promise resolves strictly after the global exists. Capturing the global_ref
    // keeps the Java peer alive until the callback runs.
    auto javaPart = javaPart_;
    push_->install([javaPart] {
        static const auto onPushInstalled = javaPart->getClass()->getMethod<void()>("onPushInstalled");
        onPushInstalled(javaPart);
    });
}

void FJVideoPushInstaller::registerNatives() {
    registerHybrid({
        makeNativeMethod("initHybrid", FJVideoPushInstaller::initHybrid),
        makeNativeMethod("installPush", FJVideoPushInstaller::installPush),
    });
}

}  // namespace fishjam

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *) {
    return facebook::jni::initialize(vm, [] { fishjam::FJVideoPushInstaller::registerNatives(); });
}
