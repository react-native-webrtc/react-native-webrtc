// JSI channel for pushing custom video frames from JS to native.
//
// Installs `__fishjamWebrtcGetCustomVideoSink(trackId)` on the JS runtime, which
// returns a per-track `CustomVideoSink` host object with a `push(frame)` method.
// Because react-native-worklets serializes host objects *by reference*, this sink
// can be captured into a frame-processor worklet and its `push` dispatches
// synchronously on the worklet thread to the same native instance — no hop. Each
// push is routed to a platform-registered delivery callback that resolves the
// bound `trackId` to its native capture controller.
//
// Pure C++20; the jsi::Runtime is only touched on the JS/worklet thread.
#pragma once

#include <atomic>
#include <cstdint>
#include <functional>
#include <memory>
#include <mutex>
#include <string>

#include <ReactCommon/CallInvoker.h>
#include <jsi/jsi.h>

class FJVideoPush;

// Per-track push handle handed to JS on `track.sink`. Holds only the bound
// `trackId` and a weak reference to the owning FJVideoPush; every `push` forwards
// to the shared platform delivery callback with this sink's `trackId`. Shared by
// reference into worklet runtimes, so `push` runs synchronously wherever it is
// called (worklet or main JS).
class CustomVideoSink : public facebook::jsi::HostObject {
   public:
    CustomVideoSink(std::weak_ptr<FJVideoPush> owner, std::string trackId)
        : owner_(std::move(owner)), trackId_(std::move(trackId)) {}

    facebook::jsi::Value get(facebook::jsi::Runtime &rt, const facebook::jsi::PropNameID &name) override;

   private:
    std::weak_ptr<FJVideoPush> owner_;
    std::string trackId_;
};

// Owns the JS-facing push channel and forwards each frame to the platform's
// registered delivery callback. The platform layer wires up frame delivery via
// setDeliver.
class FJVideoPush : public std::enable_shared_from_this<FJVideoPush> {
   public:
    // Called for every frame pushed from JS, resolved to primitive values.
    //   * `nativeBuffer` non-zero  -> forwarding: a retainable CVPixelBufferRef /
    //     AHardwareBuffer* to wrap and deliver (bufferIndex/fence unused).
    //   * `nativeBuffer` zero      -> pooled: deliver `bufferIndex` with the
    //     resolved fence (`0`/`0` when no fence was supplied).
    using DeliverFn = std::function<void(const std::string &trackId,
                                         int bufferIndex,
                                         uint64_t nativeBuffer,
                                         uint64_t timestampNs,
                                         int rotation,
                                         uint64_t fenceHandle,
                                         uint64_t fenceSignaledValue)>;

    explicit FJVideoPush(std::shared_ptr<facebook::react::CallInvoker> jsInvoker) : jsInvoker_(std::move(jsInvoker)) {}

    // Installs the `__fishjamWebrtcGetCustomVideoSink` global; invokes
    // onInstalled on the JS thread once ready.
    void install(std::function<void()> onInstalled);

    bool isInstalled() const { return installed_.load(); }

    // Registers the platform delivery callback that each pushed frame is
    // forwarded to. Thread-safe: may be re-registered (e.g. on a JS reload)
    // while pushes are in flight; concurrent deliverFrame calls see either the
    // old or the new callback.
    void setDeliver(DeliverFn deliver);

    // Parses a JS frame object and forwards it to the delivery callback under
    // `trackId`. Shared by every CustomVideoSink. Malformed frames are dropped
    // (never throws back into JS on the hot path).
    void deliverFrame(facebook::jsi::Runtime &rt, const std::string &trackId, const facebook::jsi::Object &frame);

   private:
    // Returns a fresh sink host object bound to `trackId`. Called on the JS runtime
    // inside the get-sink global. Not retained natively: JS fetches a track's sink
    // exactly once and owns its lifetime (the host object is freed when the last
    // runtime referencing it drops it), so there is nothing to cache or leak.
    facebook::jsi::Value getSink(facebook::jsi::Runtime &rt, const std::string &trackId);

    std::shared_ptr<facebook::react::CallInvoker> jsInvoker_;
    // Swapped under deliverMutex_ (setDeliver may race in-flight worklet pushes
    // on a reload); deliverFrame copies the shared_ptr under the lock and calls
    // outside it.
    std::mutex deliverMutex_;
    std::shared_ptr<const DeliverFn> deliver_;
    std::atomic<bool> installed_{false};
};
