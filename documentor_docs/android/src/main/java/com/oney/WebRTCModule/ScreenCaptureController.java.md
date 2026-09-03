# Technical Documentation: `ScreenCaptureController.java`

## Overview

The `ScreenCaptureController` class is an Android component within the `com.oney.WebRTCModule` package that handles screen capture functionality for WebRTC. It extends `AbstractVideoCaptureController` and leverages Android's `MediaProjection` API (via WebRTC's `ScreenCapturerAndroid`) to capture the device's screen content. 

It manages screen orientation changes dynamically by adjusting capture dimensions and ensures proper cleanup when media projection stops or the controller is disposed.

---

## Class Hierarchy

```
AbstractVideoCaptureController
  └── ScreenCaptureController
```

---

## Constants & Class Variables

### Constants
- **`TAG`** (`String`): Set to `ScreenCaptureController.class.getSimpleName()`. Used for logging.
- **`DEFAULT_FPS`** (`int`): Value is `30`. Defines the default target frame rate for the screen capture.

### Instance Fields
- **`mediaProjectionPermissionResultData`** (`Intent`): The `Intent` containing the result data obtained from the user's consent prompt for screen capture (`MediaProjection`).
- **`orientationListener`** (`OrientationEventListener`): Listens for changes in the physical orientation of the device and recalculates capture dimensions accordingly.
- **`context`** (`Context`): The Android `Context` associated with this controller instance.

---

## Constructor

```java
public ScreenCaptureController(
    Context context, 
    int width, 
    int height, 
    Intent mediaProjectionPermissionResultData, 
    float resolutionScale
)
```

### Parameters
- **`context`** (`Context`): Android context used for display metric calculations and `MediaProjectionService` calls. Cast to `Activity` during display metric queries.
- **`width`** (`int`): Base width for screen capture before scaling.
- **`height`** (`int`): Base height for screen capture before scaling.
- **`mediaProjectionPermissionResultData`** (`Intent`): Permission result `Intent` from the `MediaProjection` prompt.
- **`resolutionScale`** (`float`): Scale factor applied to the target dimensions (width and height).

### Behavior
1. Calls the superclass (`AbstractVideoCaptureController`) constructor with the initial scaled width (`width * resolutionScale`), scaled height (`height * resolutionScale`), and `DEFAULT_FPS` (30).
2. Initializes instance variables (`mediaProjectionPermissionResultData`, `context`).
3. Instantiates an `OrientationEventListener` that executes the following on orientation changes:
   - Queries current display metrics using `DisplayUtils.getDisplayMetrics((Activity) context)`.
   - Computes updated target dimensions by multiplying display pixel dimensions by `resolutionScale`.
   - Dispatches a runnable to an executor thread via `ThreadUtils.runOnExecutor(...)` to call `videoCapturer.changeCaptureFormat(width, height, DEFAULT_FPS)`. Exceptions thrown during format changes are caught and ignored.
4. Checks `canDetectOrientation()` on the listener and enables it if supported.

---

## Methods

### `getDeviceId()`
```java
@Override
public String getDeviceId()
```
- **Returns**: `"screen-capture"`
- **Purpose**: Identifies the capture device source type for this controller.

---

### `dispose()`
```java
@Override
public void dispose()
```
- **Purpose**: Cleans up resources allocated for screen capture.
- **Behavior**:
  1. Aborts the active media projection session via `MediaProjectionService.abort(context)`.
  2. Calls `super.dispose()` to perform superclass disposal logic.

---

### `createVideoCapturer()`
```java
@Override
protected VideoCapturer createVideoCapturer()
```
- **Returns**: `VideoCapturer` (An instance of `org.webrtc.ScreenCapturerAndroid`).
- **Purpose**: Instantiates and returns the WebRTC `ScreenCapturerAndroid` configured with the permission intent and a `MediaProjection.Callback`.
- **Callback Logic (`MediaProjection.Callback.onStop()`)**:
  When the underlying media projection stops:
  1. Logs a warning: `"Media projection stopped."`
  2. Disables the `orientationListener`.
  3. Calls `stopCapture()` (inherited from superclass).
  4. If `capturerEventsListener` is not `null`, invokes `capturerEventsListener.onCapturerEnded()`.

---

## Key Workflows & Internal Logic

### Dynamic Orientation Handling
When the device orientation changes:
1. `OrientationEventListener.onOrientationChanged` is triggered.
2. Fresh display metrics are retrieved using `DisplayUtils.getDisplayMetrics((Activity) context)`.
3. Width and height are scaled by `resolutionScale`.
4. The call to `videoCapturer.changeCaptureFormat(...)` is offloaded to an executor thread via `ThreadUtils.runOnExecutor(...)` to avoid potential deadlocks with WebRTC's main thread processing.

### Lifetime and Termination
- **User or System Termination**: If the user or OS revokes/stops media projection, `MediaProjection.Callback.onStop()` triggers automatically, shutting down orientation monitoring, stopping capture, and notifying `capturerEventsListener`.
- **Explicit Cleanup**: Calling `dispose()` aborts the `MediaProjectionService` instance and delegates remaining cleanup to `AbstractVideoCaptureController`.