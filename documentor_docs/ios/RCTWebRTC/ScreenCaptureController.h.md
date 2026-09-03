# Technical Documentation: `ScreenCaptureController.h`

## Overview

The `ScreenCaptureController.h` header file defines the Objective-C interface for `ScreenCaptureController`. This class inherits from `CaptureController` and provides public methods to manage screen capture session lifecycles (starting and stopping capture) within an iOS WebRTC integration.

---

## File Details

* **File Path:** `ios/RCTWebRTC/ScreenCaptureController.h`
* **Language:** Objective-C
* **Nullability:** Macros `NS_ASSUME_NONNULL_BEGIN` and `NS_ASSUME_NONNULL_END` are used to default non-pointer type annotations to `nonnull`.

---

## Dependencies & Imports

### Frameworks & Headers
* `<Foundation/Foundation.h>`: Provides core Cocoa foundation structures and types.
* `"CaptureController.h"`: Defines the base class `CaptureController` from which `ScreenCaptureController` inherits.
* `"CapturerEventsDelegate.h"`: Imports event delegation interfaces used by capturer components.

### Forward Declarations
* `@class ScreenCapturer;`: Forward-declares the `ScreenCapturer` class to avoid circular dependencies in header inclusions.

---

## Global Constants

The header exposes two external `NSString` constants:

| Constant | Type | Description |
| :--- | :--- | :--- |
| `kRTCScreensharingSocketFD` | `NSString *const` | External constant key representing the socket file descriptor used during screen sharing communication. |
| `kRTCAppGroupIdentifier` | `NSString *const` | External constant key representing the App Group identifier used for cross-process communication (e.g., between the main app and a broadcast extension). |

---

## Class Interface

### Inheritance
`ScreenCaptureController` : `CaptureController`

### Methods

#### `initWithCapturer:`
```objc
- (instancetype)initWithCapturer:(nonnull ScreenCapturer *)capturer;
```
* **Description:** Initializes a new instance of `ScreenCaptureController` associated with a specific `ScreenCapturer`.
* **Parameters:**
  * `capturer`: A non-null instance of `ScreenCapturer`.
* **Returns:** An initialized `ScreenCaptureController` object instance.

---

#### `startCapture`
```objc
- (void)startCapture;
```
* **Description:** Triggers the process to start capturing the device screen.
* **Parameters:** None.
* **Return Value:** `void`

---

#### `stopCapture`
```objc
- (void)stopCapture;
```
* **Description:** Triggers the process to stop the ongoing screen capture session.
* **Parameters:** None.
* **Return Value:** `void`