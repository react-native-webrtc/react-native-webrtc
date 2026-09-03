# Technical Documentation: `CaptureController.h`

**File Path:** `ios/RCTWebRTC/CaptureController.h`  
**Module:** `RCTWebRTC` (iOS)

---

## Overview

The `CaptureController.h` header file defines the interface for the `CaptureController` class, an Objective-C class inheriting from `NSObject`. It serves as a controller interface for managing media capture devices within the `RCTWebRTC` library. 

It exposes properties to manage capture device identification and event delegates, along with instance methods to control the capture lifecycle (start/stop), retrieve current configuration settings, and apply runtime constraints.

---

## File Dependencies & Macro Declarations

### Imports
* `#import <Foundation/Foundation.h>`: Provides standard Cocoa Foundation types (`NSObject`, `NSString`, `NSDictionary`, `NSError`).
* `#import "CapturerEventsDelegate.h"`: Provides the protocol definition for the capture event delegate (`CapturerEventsDelegate`).

### Nullability Annotations
* `NS_ASSUME_NONNULL_BEGIN` / `NS_ASSUME_NONNULL_END`: Encloses the interface declaration. By default, all pointers are assumed to be `nonnull` unless explicitly declared with `nullable`.

---

## Class Interface: `CaptureController`

```objc
@interface CaptureController : NSObject
```

### Properties

| Property | Type | Attributes | Description |
| :--- | :--- | :--- | :--- |
| `eventsDelegate` | `id<CapturerEventsDelegate>` | `nonatomic, strong` | An object conforming to the `CapturerEventsDelegate` protocol that receives event notifications from the capture controller. |
| `deviceId` | `NSString *` | `nonatomic, copy, nullable` | An optional identifier string representing the specific capture device being controlled. Can be `nil`. |

---

## Instance Methods

### 1. `startCapture`
```objc
- (void)startCapture;
```
* **Description:** Triggers the initialization and starting of the media capture session.
* **Parameters:** None.
* **Return Value:** None (`void`).

---

### 2. `stopCapture`
```objc
- (void)stopCapture;
```
* **Description:** Stops or terminates the active media capture session.
* **Parameters:** None.
* **Return Value:** None (`void`).

---

### 3. `getSettings`
```objc
- (NSDictionary *)getSettings;
```
* **Description:** Fetches the current operating settings of the capture controller.
* **Parameters:** None.
* **Return Value:** `NSDictionary *` — A dictionary containing the key-value pairs representing current capture settings.

---

### 4. `applyConstraints:error:`
```objc
- (void)applyConstraints:(NSDictionary *)constraints error:(NSError **)outError;
```
* **Description:** Applies a set of requested media constraints to the capture controller session.
* **Parameters:**
  * `constraints` (`NSDictionary *`): A dictionary containing constraint settings to apply.
  * `outError` (`NSError **`): An output parameter pointer to an `NSError` object that will be populated if an error occurs while applying the constraints.
* **Return Value:** None (`void`).

---

## Summary of Operation Flow

1. **Initialization & Assignment:** A `CaptureController` instance is configured by setting its `deviceId` (if targeting a specific hardware/media device) and assigning an `eventsDelegate` conforming to `CapturerEventsDelegate`.
2. **Lifecycle Management:** Media capture is initiated via `startCapture` and halted via `stopCapture`.
3. **Configuration & Inspection:** 
   * Active settings can be retrieved as a dictionary using `getSettings`.
   * Dynamic constraints can be applied at runtime using `applyConstraints:error:`, with error handling managed via the Objective-C `NSError **` pattern.