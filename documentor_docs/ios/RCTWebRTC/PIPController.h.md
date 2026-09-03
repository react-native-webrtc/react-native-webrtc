# Developer Documentation: `PIPController.h`

## Overview

The `PIPController.h` header file defines the interface for `PIPController`, an Objective-C class designed to manage iOS **Picture-in-Picture (PiP)** functionality for WebRTC video streams in the `react-native-webrtc` module. 

It conforms to `AVPictureInPictureControllerDelegate` and requires **iOS 15.0 or later**.

---

## File Identification
* **File Path:** `ios/RCTWebRTC/PIPController.h`
* **Target OS/Version:** iOS 15.0+ (`API_AVAILABLE(ios(15.0))`)

---

## System Imports

| Header | Description |
| :--- | :--- |
| `<AVKit/AVKit.h>` | Provides access to AVKit framework, including standard Picture-in-Picture controller delegates. |
| `<UIKit/UIKit.h>` | Provides core iOS UI elements (`UIView`, `CGSize`). |
| `<WebRTC/RTCVideoTrack.h>` | Provides WebRTC framework class for managing video tracks. |
| `"RTCVideoViewManager.h"` | Custom header providing definitions such as `RTCVideoViewObjectFit`. |

---

## Class Interface Declaration

```objc
API_AVAILABLE(ios(15.0))
@interface PIPController : NSObject <AVPictureInPictureControllerDelegate>
```

* **Superclass:** `NSObject`
* **Protocols Standardized:** `<AVPictureInPictureControllerDelegate>`

---

## Properties

| Property | Type / Modifiers | Description |
| :--- | :--- | :--- |
| `sourceView` | `UIView *` (`nonatomic`, `weak`) | The source view used as the anchor/origin for the Picture-in-Picture controller. |
| `videoTrack` | `RTCVideoTrack *` (`nonatomic`, `strong`) | The WebRTC video track instance rendered inside the Picture-in-Picture window. |
| `startAutomatically` | `BOOL` (`nonatomic`, `assign`) | Flag controlling whether Picture-in-Picture mode should start automatically. |
| `stopAutomatically` | `BOOL` (`nonatomic`, `assign`) | Flag controlling whether Picture-in-Picture mode should stop automatically. |
| `preferredSize` | `CGSize` (`nonatomic`, `assign`) | Defines the preferred dimensions (width and height) of the PiP window. |

---

## Public Methods

### Initialization

```objc
- (instancetype)initWithSourceView:(UIView *)sourceView;
```
* **Description:** Initializes a new `PIPController` instance bound to a specified source view.
* **Parameters:**
  * `sourceView`: The target `UIView` from which PiP will be presented.

---

### Control Methods

```objc
- (void)togglePIP;
```
* **Description:** Toggles the Picture-in-Picture state (starts PiP if inactive, or stops PiP if active).

```objc
- (void)startPIP;
```
* **Description:** Manually initiates Picture-in-Picture mode.

```objc
- (void)stopPIP;
```
* **Description:** Manually terminates Picture-in-Picture mode.

---

### View & Layout Customization

```objc
- (void)insertFallbackView:(UIView *)subview;
```
* **Description:** Inserts a fallback view subview into the controller layout.
* **Parameters:**
  * `subview`: The `UIView` to be inserted as a fallback view.

```objc
- (void)setObjectFit:(RTCVideoViewObjectFit)fit;
```
* **Description:** Sets the scaling or fit mode of the video stream within the PiP interface.
* **Parameters:**
  * `fit`: An `RTCVideoViewObjectFit` value determining how the video scales inside the container view.