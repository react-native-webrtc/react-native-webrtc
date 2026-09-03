# Technical Documentation: `CapturerEventsDelegate.h`

**File Path:** `ios/RCTWebRTC/CapturerEventsDelegate.h`  
**Language:** Objective-C  
**Framework:** React Native WebRTC (`RCTWebRTC`) / WebRTC iOS SDK

---

## Overview

The `CapturerEventsDelegate.h` header file defines an Objective-C protocol named `CapturerEventsDelegate`. The purpose of this protocol is to define an interface for receiving lifecycle event notifications from a video capturer (`RTCVideoCapturer`). Specifically, it provides a callback mechanism to inform conforming delegate objects when a video capturer has stopped operating and entered an irrecoverable state.

---

## Dependencies

* `<WebRTC/RTCVideoCapturer.h>`  
  Imports the native WebRTC framework's video capturer base class (`RTCVideoCapturer`), which is used as a parameter type within the protocol's delegate method.

---

## Protocol Specification

### `CapturerEventsDelegate`

```objc
@protocol CapturerEventsDelegate
```

The protocol that objects must conform to if they need to listen for terminal events emitted by an `RTCVideoCapturer` instance.

---

## Protocol Methods

### `capturerDidEnd:`

```objc
- (void)capturerDidEnd:(RTCVideoCapturer *)capturer;
```

* **Description:**  
  Invoked when the associated video capturer has ended and is in an irrecoverable state.

* **Parameters:**  
  * `capturer` (`RTCVideoCapturer *`): The instance of the video capturer that has ended. This parameter cannot be `nil` (enforced by the `NS_ASSUME_NONNULL` context).

* **Return Value:**  
  `void`

---

## Compiler Directives & Annotations

### `NS_ASSUME_NONNULL_BEGIN` / `NS_ASSUME_NONNULL_END`

The entire protocol definition is enclosed within Objective-C nullability macro blocks:

* `NS_ASSUME_NONNULL_BEGIN`
* `NS_ASSUME_NONNULL_END`

This guarantees that all pointer parameters and return values within the block are implicitly treated as non-nullable (`nonnull`) by the compiler unless explicitly specified otherwise. In this file, it ensures that the `capturer` argument passed to `capturerDidEnd:` is always guaranteed to be a valid, non-null pointer.

---

## How It Works

1. **Interface Definition:** `CapturerEventsDelegate` acts as a contract between a video capturer component and a listener/manager object inside the `RCTWebRTC` module.
2. **Event Delegation:** When a class managing an `RTCVideoCapturer` encounters a condition where the capturer terminates and cannot recover, it invokes `capturerDidEnd:` on its designated delegate.
3. **State Handling:** The object conforming to `CapturerEventsDelegate` receives the reference to the affected `RTCVideoCapturer` and can react accordingly (e.g., updating internal module state or cleaning up resources associated with that capturer).