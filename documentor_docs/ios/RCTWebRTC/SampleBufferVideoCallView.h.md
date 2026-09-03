# Technical Documentation: `SampleBufferVideoCallView.h`

**File Path:** `ios/RCTWebRTC/SampleBufferVideoCallView.h`  
**Language:** Objective-C  
**Framework Dependencies:** `AVKit`, `Foundation`, `React`, `WebRTC`

---

## Overview

The `SampleBufferVideoCallView.h` header file defines the interface for `SampleBufferVideoCallView`, a custom iOS `UIView` subclass designed to render WebRTC video frames. By conforming to the `RTCVideoRenderer` protocol and utilizing an `AVSampleBufferDisplayLayer`, this class provides a bridge for displaying WebRTC video within a native iOS view component used by React Native (`RCTWebRTC`).

---

## Headers & Imports

The file imports four primary frameworks/modules:

*   `<AVKit/AVKit.h>`: Provides media playback capabilities and native AV components.
*   `<Foundation/Foundation.h>`: Provides fundamental Objective-C data types and primitives.
*   `<React/RCTViewManager.h>`: Imports React Native view management dependencies.
*   `<WebRTC/RTCVideoRenderer.h>`: Provides the `RTCVideoRenderer` protocol from the WebRTC framework required to receive and render video frames.

---

## Class Interface

```objc
@interface SampleBufferVideoCallView : UIView <RTCVideoRenderer>
```

*   **Superclass:** `UIView` — Inherits standard iOS UI view behaviors and layout mechanics.
*   **Protocol Conformance:** `<RTCVideoRenderer>` — Conforms to WebRTC's video renderer interface, enabling it to act as a target destination for WebRTC video tracks.

---

## Properties

### `sampleBufferLayer`
```objc
@property(nonnull, nonatomic, readonly) AVSampleBufferDisplayLayer *sampleBufferLayer;
```
*   **Type:** `AVSampleBufferDisplayLayer *`
*   **Attributes:** `nonnull`, `nonatomic`, `readonly`
*   **Description:** A read-only reference to the underlying `AVSampleBufferDisplayLayer` responsible for rendering decompressed video sample buffers (`CMSampleBufferRef`).

---

### `shouldRender`
```objc
@property(nonatomic, assign) BOOL shouldRender;
```
*   **Type:** `BOOL`
*   **Attributes:** `nonatomic`, `assign`
*   **Description:** A primitive boolean property used to enable or disable frame rendering logic for the view instance.

---

## Instance Methods

### `requestScaleRecalculation`
```objc
- (void)requestScaleRecalculation;
```
*   **Return Type:** `void`
*   **Parameters:** None
*   **Description:** Triggers a request to recalculate the video scaling and layout dimensions within the view's layer bounds.