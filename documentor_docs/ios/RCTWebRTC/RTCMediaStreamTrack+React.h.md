# Technical Documentation: `RTCMediaStreamTrack+React.h`

## Overview

The `RTCMediaStreamTrack+React.h` file is an Objective-C header in the `react-native-webrtc` iOS codebase (`ios/RCTWebRTC/`). It declares an Objective-C **category** named `React` on the native WebRTC framework's `RTCMediaStreamTrack` class.

Its purpose is to extend `RTCMediaStreamTrack` by attaching a reference to a `CaptureController` object, bridging native WebRTC track management with custom capture handling required by the React Native library wrapper.

---

## File Metadata

* **File Path:** `ios/RCTWebRTC/RTCMediaStreamTrack+React.h`
* **Language:** Objective-C
* **Framework Dependencies:** `WebRTC` (`WebRTC/RTCMediaStreamTrack.h`)

---

## Key Components

### 1. Imports and Forward Declarations

```objc
#import <WebRTC/RTCMediaStreamTrack.h>

@class CaptureController;
```

* **`#import <WebRTC/RTCMediaStreamTrack.h>`**: Imports the definition of the `RTCMediaStreamTrack` class provided by the core Google WebRTC iOS SDK.
* **`@class CaptureController;`**: A forward declaration telling the compiler that `CaptureController` exists as a class type, avoiding the need to import `CaptureController.h` directly in this header file.

---

### 2. Category Declaration

```objc
@interface RTCMediaStreamTrack (React)
```

* **Objective-C Category:** `RTCMediaStreamTrack (React)`
* Expands the core `RTCMediaStreamTrack` interface with custom React Native wrapper functionality without subclassing or modifying the underlying WebRTC framework source directly.

---

### 3. Properties

```objc
@property(strong, nonatomic) CaptureController *captureController;
```

* **`captureController`**: A property holding a reference to a `CaptureController` instance associated with this specific media stream track.
* **Property Attributes:**
  * `strong`: Defines a strong reference ownership model, ensuring the `CaptureController` instance is retained in memory as long as the track holds onto it.
  * `nonatomic`: Specifies that property accessors are non-thread-safe for performance optimization (standard pattern in iOS wrapper classes).

---

## How It Works

1. **Extension via Category:** By introducing the `React` category on `RTCMediaStreamTrack`, any instance of `RTCMediaStreamTrack` within the module can store and access a reference to its corresponding `CaptureController`.
2. **Controller Association:** This property establishes a direct link between a WebRTC media stream track (`RTCMediaStreamTrack`) and the media capture controller (`CaptureController`) responsible for managing media input sources (such as video or audio capture hardware).