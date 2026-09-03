# Technical Documentation: `VideoCaptureController.h`

## Overview

The `VideoCaptureController.h` header file defines the interface for `VideoCaptureController`, an Objective-C class within the `react-native-webrtc` iOS codebase (`ios/RCTWebRTC/`). This class inherits from `CaptureController` and manages video capture operations using WebRTC's `RTCCameraVideoCapturer`.

The header also wraps the entire class interface in a platform check (`#if !TARGET_OS_TV`), ensuring that camera video capture functionality is excluded when building for tvOS targets.

---

## Dependencies and Imports

* **`Foundation/Foundation.h`**: Standard Cocoa Foundation framework.
* **`<WebRTC/RTCCameraVideoCapturer.h>`**: WebRTC framework header providing `RTCCameraVideoCapturer`, which interfaces with iOS camera hardware.
* **`"CaptureController.h"`**: Header declaring the `CaptureController` base class from which `VideoCaptureController` inherits.

---

## Class Declaration

```objc
@interface VideoCaptureController : CaptureController
```

`VideoCaptureController` extends `CaptureController` to provide camera-specific video capture functionality.

---

## Properties

| Property | Type | Attributes | Description |
| :--- | :--- | :--- | :--- |
| `capturer` | `RTCCameraVideoCapturer *` | `nonatomic, readonly, strong` | Holds the underlying WebRTC camera video capturer instance used to capture video frames from the device's camera. |
| `selectedFormat` | `AVCaptureDeviceFormat *` | `nonatomic, readonly, strong` | Represents the currently active `AVCaptureDeviceFormat` selected for the camera (e.g., resolution and media type configuration). |
| `frameRate` | `int` | `nonatomic, readonly, assign` | The current video capture frame rate (FPS). |
| `enableMultitaskingCameraAccess` | `BOOL` | `nonatomic, assign` | A boolean flag indicating whether camera access during iOS multitasking mode is enabled. |

---

## Methods

### Initialization

```objc
- (instancetype)initWithCapturer:(RTCCameraVideoCapturer *)capturer andConstraints:(NSDictionary *)constraints;
```
* **Purpose**: Initializes a new instance of `VideoCaptureController`.
* **Parameters**:
  * `capturer`: An instance of `RTCCameraVideoCapturer` used for capturing video.
  * `constraints`: A dictionary containing initial configuration constraints for the video capture stream (such as resolution or frame rate specifications).

---

### Capture Control Methods

#### `startCapture`
```objc
- (void)startCapture;
```
* **Purpose**: Starts the camera video capture session.

#### `stopCapture`
```objc
- (void)stopCapture;
```
* **Purpose**: Stops the active camera video capture session.

#### `switchCamera`
```objc
- (void)switchCamera;
```
* **Purpose**: Toggles/switches between available camera devices (e.g., front-facing and rear-facing cameras).

---

### Configuration Methods

#### `applyConstraints:error:`
```objc
- (void)applyConstraints:(NSDictionary *)constraints error:(NSError **)outError;
```
* **Purpose**: Applies a new set of constraints to the video capture session dynamically.
* **Parameters**:
  * `constraints`: A dictionary containing updated capture constraints.
  * `outError`: A pointer to an `NSError` object that will be populated if applying constraints fails.

---

## Target Platform Conditional

```objc
#if !TARGET_OS_TV
...
#endif
```

The entire file contents are conditionally compiled using `#if !TARGET_OS_TV`. This ensures that camera capture functionality is only available on supported platforms (such as iOS) and is disabled for tvOS builds where camera support is absent or restricted.