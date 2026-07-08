// fbjni HybridClass backing com.oney.WebRTCModule.FJVideoPushInstaller.
//
// Installs the JS global `__fishjamWebrtcGetCustomVideoSink` on the JS thread
// (via the CallInvoker), then notifies the Java peer so the install Promise
// resolves only once the global actually exists. Each frame pushed through a
// sink obtained from that global is forwarded back to the Java peer's
// deliverFrame(...), which routes it to the matching CustomVideoFrameDelivery.
#pragma once

#include <ReactCommon/CallInvokerHolder.h>
#include <fbjni/fbjni.h>

#include <memory>

#include "FJVideoPushJSI.h"

namespace fishjam {

class FJVideoPushInstaller : public facebook::jni::HybridClass<FJVideoPushInstaller> {
   public:
    static constexpr auto kJavaDescriptor = "Lcom/oney/WebRTCModule/FJVideoPushInstaller;";

    static facebook::jni::local_ref<jhybriddata> initHybrid(
        facebook::jni::alias_ref<jhybridobject> javaThis,
        facebook::jni::alias_ref<facebook::react::CallInvokerHolder::javaobject> callInvokerHolder);

    static void registerNatives();

    // Sets the JS global on the JS thread, then calls the Java peer's
    // onPushInstalled() once it is in place.
    void installPush();

   private:
    friend HybridBase;

    facebook::jni::global_ref<javaobject> javaPart_;
    std::shared_ptr<FJVideoPush> push_;

    FJVideoPushInstaller(facebook::jni::alias_ref<jhybridobject> javaThis, std::shared_ptr<FJVideoPush> push);
};

}  // namespace fishjam
