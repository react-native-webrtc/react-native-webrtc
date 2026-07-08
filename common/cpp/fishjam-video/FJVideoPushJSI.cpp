#include "FJVideoPushJSI.h"

namespace jsi = facebook::jsi;

jsi::Value CustomVideoSink::get(jsi::Runtime &rt, const jsi::PropNameID &name) {
    if (name.utf8(rt) != "push") {
        return jsi::Value::undefined();
    }
    // Capture the bound trackId + owner by value so the returned push function is
    // self-contained and safe to call on whatever runtime `get` ran on (worklet
    // or main JS). `get` is invoked lazily by the runtime that captured the sink.
    std::weak_ptr<FJVideoPush> owner = owner_;
    std::string trackId = trackId_;
    return jsi::Function::createFromHostFunction(
        rt,
        jsi::PropNameID::forAscii(rt, "push"),
        1,
        [owner, trackId](jsi::Runtime &rt, const jsi::Value &, const jsi::Value *args, size_t count) -> jsi::Value {
            auto push = owner.lock();
            if (!push || count == 0 || !args[0].isObject()) {
                return jsi::Value::undefined();
            }
            jsi::Object frame = args[0].getObject(rt);
            push->deliverFrame(rt, trackId, frame);
            return jsi::Value::undefined();
        });
}

void FJVideoPush::deliverFrame(jsi::Runtime &rt, const std::string &trackId, const jsi::Object &frame) {
    // Cheap refcount copy under the lock; the callback itself runs outside it so
    // a slow delivery never blocks setDeliver (and vice versa).
    std::shared_ptr<const DeliverFn> deliver;
    {
        std::lock_guard<std::mutex> lock(deliverMutex_);
        deliver = deliver_;
    }
    if (!deliver || !*deliver) {
        return;
    }

    // Per-frame hot path: validate every field and silently drop a malformed
    // frame rather than throwing a jsi::JSError back into JS.

    // nativeBuffer is the forwarding discriminator: a retainable CVPixelBufferRef
    // (iOS) / AHardwareBuffer* (Android), carried as a bigint. Non-zero => forward.
    uint64_t nativeBuffer = 0;
    jsi::Value nativeBufferValue = frame.getProperty(rt, "nativeBuffer");
    if (nativeBufferValue.isBigInt()) {
        nativeBuffer = nativeBufferValue.asBigInt(rt).getUint64(rt);
    }

    // bufferIndex identifies the pooled surface; required when not forwarding.
    int bufferIndex = 0;
    jsi::Value bufferIndexValue = frame.getProperty(rt, "bufferIndex");
    bool hasBufferIndex = bufferIndexValue.isNumber();
    if (hasBufferIndex) {
        bufferIndex = static_cast<int>(bufferIndexValue.asNumber());
    }
    if (nativeBuffer == 0 && !hasBufferIndex) {
        // Neither a forward buffer nor a pool index -> nothing to deliver.
        return;
    }

    // timestampNs is optional; 0 tells the native layer to stamp at delivery
    // (the raw buffer pointer carries no presentation time).
    uint64_t timestampNs = 0;
    jsi::Value timestampValue = frame.getProperty(rt, "timestampNs");
    if (timestampValue.isNumber()) {
        timestampNs = static_cast<uint64_t>(timestampValue.asNumber());
    }

    // rotation is optional; accept only the four valid quarter turns, defaulting
    // to 0 for absent / non-numeric / out-of-domain values.
    int rotation = 0;
    jsi::Value rotationValue = frame.getProperty(rt, "rotation");
    if (rotationValue.isNumber()) {
        int requested = static_cast<int>(rotationValue.asNumber());
        if (requested == 90 || requested == 180 || requested == 270) {
            rotation = requested;
        }
    }

    // fence is optional (pooled only); absent/null/malformed means no fence ->
    // 0/0 (immediate delivery). Only read handle/value when both are bigints.
    uint64_t fenceHandle = 0;
    uint64_t fenceSignaledValue = 0;
    jsi::Value fenceValue = frame.getProperty(rt, "fence");
    if (fenceValue.isObject()) {
        jsi::Object fenceObject = fenceValue.getObject(rt);
        jsi::Value handleValue = fenceObject.getProperty(rt, "handle");
        jsi::Value signaledValue = fenceObject.getProperty(rt, "signaledValue");
        if (handleValue.isBigInt() && signaledValue.isBigInt()) {
            fenceHandle = handleValue.asBigInt(rt).getUint64(rt);
            fenceSignaledValue = signaledValue.asBigInt(rt).getUint64(rt);
        }
    }

    (*deliver)(trackId, bufferIndex, nativeBuffer, timestampNs, rotation, fenceHandle, fenceSignaledValue);
}

jsi::Value FJVideoPush::getSink(jsi::Runtime &rt, const std::string &trackId) {
    // Fresh sink per request: JS fetches a track's sink once and holds it, so the
    // runtime owns the host object's lifetime. Keeping a native map keyed by
    // trackId only pins a shared_ptr per track ever created (an unbounded leak) and
    // buys nothing — the sink carries only its trackId + a weak owner.
    auto sink = std::make_shared<CustomVideoSink>(weak_from_this(), trackId);
    return jsi::Object::createFromHostObject(rt, sink);
}

void FJVideoPush::install(std::function<void()> onInstalled) {
    // Reset for re-install on JS reload: the same FJVideoPush instance may be
    // reused across reloads (iOS associated object), so the flag must be cleared.
    installed_.store(false);
    std::weak_ptr<FJVideoPush> weakSelf = shared_from_this();
    jsInvoker_->invokeAsync([weakSelf, onInstalled](jsi::Runtime &rt) {
        auto self = weakSelf.lock();
        if (!self) {
            return;
        }

        // Per-track sink accessor: returns a CustomVideoSink host object bound to
        // the given trackId. Shared by reference into worklets for hop-free push.
        auto getSink = jsi::Function::createFromHostFunction(
            rt,
            jsi::PropNameID::forAscii(rt, "__fishjamWebrtcGetCustomVideoSink"),
            1,
            [weakSelf](jsi::Runtime &rt, const jsi::Value &, const jsi::Value *args, size_t count) -> jsi::Value {
                auto self = weakSelf.lock();
                if (!self || count == 0 || !args[0].isString()) {
                    return jsi::Value::undefined();
                }
                return self->getSink(rt, args[0].asString(rt).utf8(rt));
            });
        rt.global().setProperty(rt, "__fishjamWebrtcGetCustomVideoSink", getSink);

        self->installed_.store(true);
        if (onInstalled) {
            onInstalled();
        }
    });
}

void FJVideoPush::setDeliver(DeliverFn deliver) {
    auto boxed = std::make_shared<const DeliverFn>(std::move(deliver));
    std::lock_guard<std::mutex> lock(deliverMutex_);
    deliver_ = std::move(boxed);
}
