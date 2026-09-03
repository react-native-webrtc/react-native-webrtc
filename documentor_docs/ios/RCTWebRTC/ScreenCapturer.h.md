# Technical Documentation: `ScreenCapturer.h`

**File Path:** `ios/RCTWebRTC/ScreenCapturer.h`

## Overview

The `ScreenCapturer.h` header file declares the interface for the `ScreenCapturer` class, an Objective-C class within the `react-native-webrtc` iOS module (`RCTWebRTC`). 

`ScreenCapturer` inherits from WebRTC's `RTCVideoCapturer` and provides an interface for starting and stopping a screen capture session utilizing a `SocketConnection` and communicating events back via delegate protocols.

---

## Dependencies and Imports

- **`<AVFoundation/AVFoundation.h>`**: Provides access to Apple's AVFoundation framework for media handling.
- **`<WebRTC/RTCVideoCapturer.h>`**: Provides the `RTCVideoCapturer` base class from the native WebRTC framework.
- **`"CapturerEventsDelegate.h"`**: Defines the protocol `CapturerEventsDelegate` used to report capturer lifecycle events.
- **`@class SocketConnection;`**: Forward declaration for the `SocketConnection` class, which is passed as a parameter during capture initialization.

---

## Class Interface

```objc
@interface ScreenCapturer : RTCVideoCapturer
```

### Inheritance
- **Base Class:** `RTCVideoCapturer`

---

## Properties

### `eventsDelegate`
```objc
@property(nonatomic, weak) id<CapturerEventsDelegate> eventsDelegate;
```
* **Type:** `id<CapturerEventsDelegate>`
* **Attributes:** `nonatomic`, `weak`
* **Description:** A weak reference to an object implementing the `CapturerEventsDelegate` protocol. Used to notify a delegate of events related to the screen capturer.

---

## Methods

### `initWithDelegate:`
```objc
- (instancetype)initWithDelegate:(__weak id<RTCVideoCapturerDelegate>)delegate;
```
* **Parameters:**
  * `delegate`: A weak reference to an object conforming to the `RTCVideoCapturerDelegate` protocol (defined by the WebRTC framework).
* **Return Value:** An initialized instance of `ScreenCapturer`.
* **Description:** Initializes a new instance of `ScreenCapturer` and attaches the provided WebRTC capturer delegate.

---

### `startCaptureWithConnection:`
```objc
- (void)startCaptureWithConnection:(nonnull SocketConnection *)connection;
```
* **Parameters:**
  * `connection`: A non-null instance of `SocketConnection`.
* **Return Value:** `void`
* **Description:** Initiates the screen capture process using the specified `SocketConnection`.

---

### `stopCapture`
```objc
- (void)stopCapture;
```
* **Parameters:** None.
* **Return Value:** `void`
* **Description:** Stops the active screen capture session.

---

## Nullability Macro Usage

The entire interface is wrapped in standard Objective-C nullability macros:
* `NS_ASSUME_NONNULL_BEGIN`
* `NS_ASSUME_NONNULL_END`

This specifies that pointer types within the block are assumed to be `nonnull` by default unless explicitly specified otherwise (e.g., using `__weak` or explicitly marked annotations).