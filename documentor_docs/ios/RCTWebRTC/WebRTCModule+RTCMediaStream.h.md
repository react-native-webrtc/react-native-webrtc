# Technical Documentation: `ios/RCTWebRTC/WebRTCModule+RTCMediaStream.h`

## Overview

The `WebRTCModule+RTCMediaStream.h` file is an Objective-C header file that defines a category named `RTCMediaStream` on the `WebRTCModule` class. This interface extends `WebRTCModule` with declarations for managing video effect processing, instantiating video tracks using a custom capture controller block, and constructing media streams from media tracks.

---

## Header Imports

The file relies on three imported header files:

```objc
#import "CaptureController.h"
#import "VideoEffectProcessor.h"
#import "WebRTCModule.h"
```

* **`CaptureController.h`**: Imports the `CaptureController` type used as a return type in the track creation block.
* **`VideoEffectProcessor.h`**: Imports the `VideoEffectProcessor` type used for the `videoEffectProcessor` property.
* **`WebRTCModule.h`**: Imports the primary `WebRTCModule` class interface to which this category adds functionality.

---

## Class Category: `WebRTCModule (RTCMediaStream)`

This category adds media stream and video track creation capabilities to the `WebRTCModule` class.

### Properties

#### `videoEffectProcessor`
```objc
@property(nonatomic, strong) VideoEffectProcessor *videoEffectProcessor;
```
* **Type:** `VideoEffectProcessor *`
* **Attributes:** `nonatomic`, `strong`
* **Description:** A strong reference to a `VideoEffectProcessor` instance, allowing the module to retain and manage video effect processing logic.

---

### Method Declarations

#### 1. `createVideoTrackWithCaptureController:`

```objc
- (RTCVideoTrack *)createVideoTrackWithCaptureController:
    (CaptureController * (^)(RTCVideoSource *))captureControllerCreator;
```

* **Description:** Declares a method that creates and returns an `RTCVideoTrack` using a provided block callback.
* **Parameters:**
  * `captureControllerCreator`: A block that takes an `RTCVideoSource *` as an input parameter and returns a `CaptureController *` instance.
* **Return Value:** `RTCVideoTrack *` — An instance of a WebRTC video track.

---

#### 2. `createMediaStream:`

```objc
- (NSArray *)createMediaStream:(NSArray<RTCMediaStreamTrack *> *)tracks;
```

* **Description:** Declares a method that creates a media stream using an array of media stream tracks.
* **Parameters:**
  * `tracks`: An `NSArray` containing instances of `RTCMediaStreamTrack *`.
* **Return Value:** `NSArray *` — Returns an array containing the created stream representation/metadata.

---

## Key Components Summary

| Component | Type | Functionality |
| :--- | :--- | :--- |
| `videoEffectProcessor` | Property (`VideoEffectProcessor *`) | Holds a reference to the video effect processor. |
| `createVideoTrackWithCaptureController:` | Method | Creates an `RTCVideoTrack` using a block that yields a `CaptureController`. |
| `createMediaStream:` | Method | Constructs a media stream from a provided list of `RTCMediaStreamTrack` objects. |