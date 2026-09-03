# Technical Documentation: `VideoEffectProcessor.h`

**File Path:** `ios/RCTWebRTC/videoEffects/VideoEffectProcessor.h`

---

## Overview

The `VideoEffectProcessor` class is an Objective-C interface that manages a pipeline of video frame processors and connects them to a WebRTC video source. By conforming to the `RTCVideoCapturerDelegate` protocol, `VideoEffectProcessor` acts as a delegate capable of receiving captured video frames, applying a series of frame processing delegates, and delivering frames to an `RTCVideoSource`.

---

## Dependencies & Imports

* **`<WebRTC/RTCVideoSource.h>`**: Provides the `RTCVideoSource` class, which represents a WebRTC video track source.
* **`"VideoFrameProcessor.h"`**: Provides the definition for the `VideoFrameProcessorDelegate` protocol, which defines the interface for objects that process individual video frames.

---

## Interface & Protocol Conformance

```objc
@interface VideoEffectProcessor : NSObject <RTCVideoCapturerDelegate>
```

* **Superclass:** `NSObject`
* **Conformed Protocol:** `<RTCVideoCapturerDelegate>`
  * Conforming to `RTCVideoCapturerDelegate` allows instances of `VideoEffectProcessor` to act as delegates for WebRTC video capturers and receive captured `RTCVideoFrame` objects.

---

## Properties

### `videoFrameProcessors`
```objc
@property(nonatomic, strong) NSArray<NSObject<VideoFrameProcessorDelegate> *> *videoFrameProcessors;
```
* **Type:** `NSArray<NSObject<VideoFrameProcessorDelegate> *> *`
* **Attributes:** `nonatomic`, `strong`
* **Description:** An ordered array of objects conforming to the `VideoFrameProcessorDelegate` protocol. These processor objects define the video effects or modifications applied to captured frames.

### `videoSource`
```objc
@property(nonatomic, strong) RTCVideoSource *videoSource;
```
* **Type:** `RTCVideoSource *`
* **Attributes:** `nonatomic`, `strong`
* **Description:** A reference to the WebRTC `RTCVideoSource` object associated with this processor.

---

## Initializer

### `initWithProcessors:videoSource:`

```objc
- (instancetype)initWithProcessors:(NSArray<NSObject<VideoFrameProcessorDelegate> *> *)videoFrameProcessors
                       videoSource:(RTCVideoSource *)videoSource;
```

#### Parameters:
* **`videoFrameProcessors`**: An `NSArray` containing one or more objects conforming to `VideoFrameProcessorDelegate`.
* **`videoSource`**: The target `RTCVideoSource` instance that receives or manages the processed video.

#### Return Value:
* An initialized instance of `VideoEffectProcessor`.

---

## Functional Role Summary

Based on the header declaration:
1. **Receives Captured Frames:** Adopts `RTCVideoCapturerDelegate` to listen for incoming video frames from a capturer.
2. **Holds Effect Delegates:** Stores a list of processors (`videoFrameProcessors`) that conform to `VideoFrameProcessorDelegate`.
3. **Integrates with WebRTC:** Maintains a reference to an `RTCVideoSource` instance to link frame processing directly into the WebRTC pipeline.