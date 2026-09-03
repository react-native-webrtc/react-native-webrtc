# CameraCaptureController Technical Documentation

The `CameraCaptureController` class manages the lifecycle, selection, and constraint updates for video capture devices (cameras) within the WebRTC Android module. It extends `AbstractVideoCaptureController` and leverages WebRTC's `CameraEnumerator` and capturer classes (`Camera1Capturer`, `Camera2Capturer`) to interface with Android device cameras.

---

## Class Overview

* **Package:** `com.oney.WebRTCModule`
* **Extends:** `AbstractVideoCaptureController`
* **Primary Responsibilities:**
  * Initializing and instantiating WebRTC camera video capturers.
  * Selecting cameras based on constraints (`deviceId`, `facingMode`).
  * Applying dynamic constraints (switching cameras or changing capture format like resolution and frame rate).
  * Calculating actual physical capture dimensions supported by the camera hardware.

---

## Member Variables

| Variable | Type | Description |
| :--- | :--- | :--- |
| `TAG` | `String` | Static log tag initialized to the class's simple name (`CameraCaptureController`). |
| `isFrontFacing` | `boolean` | Tracks whether the currently active camera is front-facing. |
| `currentDeviceId` | `@Nullable String` | Stores the index of the currently active camera device as a string, or `null` if none. |
| `context` | `Context` | Android context used for system services (e.g., `CameraManager`). |
| `cameraEnumerator` | `CameraEnumerator` | WebRTC utility used to query available cameras and instantiate capturers. |
| `constraints` | `ReadableMap` | Stores constraints passed from React Native (`width`, `height`, `frameRate`, `deviceId`, `facingMode`). |
| `cameraEventsHandler` | `CameraEventsHandler` | Anonymous class implementation listening to camera events (e.g., `onCameraOpening`). |

---

## Constructor

```java
public CameraCaptureController(Context context, CameraEnumerator cameraEnumerator, ReadableMap constraints)
```

### Parameters
* `context`: Android `Context`.
* `cameraEnumerator`: An instance of `CameraEnumerator` (WebRTC).
* `constraints`: A React Native `ReadableMap` containing `width`, `height`, and `frameRate`.

### Behavior
Passes `width`, `height`, and `frameRate` extracted from `constraints` to the parent constructor (`AbstractVideoCaptureController`), then stores local references to `context`, `cameraEnumerator`, and `constraints`.

---

## Core Methods

### `getDeviceId()`
```java
@Nullable
@Override
public String getDeviceId()
```
Returns `currentDeviceId`, which represents the current camera index as a String.

---

### `getSettings()`
```java
@Override
public WritableMap getSettings()
```
Retrieves base settings settings map from `super.getSettings()` and populates the `facingMode` property:
* Sets `"facingMode"` to `"user"` if `isFrontFacing` is `true`.
* Sets `"facingMode"` to `"environment"` if `isFrontFacing` is `false`.

---

### `applyConstraints()`
```java
@Override
public void applyConstraints(ReadableMap constraints, @Nullable Consumer<Exception> onFinishedCallback)
```
Applies updated constraints (resolution, frame rate, device ID, or facing mode) to an active capture session.

#### Execution Flow:
1. **Initial Check:** If no active `videoCapturer` exists, updates saved target values (`targetWidth`, `targetHeight`, `targetFps`, `constraints`) and invokes `onFinishedCallback.accept(null)`.
2. **Camera Identification:**
   * Reads `deviceId` and `facingMode` from new constraints.
   * If `deviceId` is provided, attempts to parse it as an integer index into device names list.
   * If `deviceId` fails or is not provided, falls back to `facingMode` matching (defaults to `"user"` facing camera).
   * If no valid camera matches are found, passes an `OverconstrainedError` exception to `onFinishedCallback`.
3. **Switch Logic:**
   * Compares the target camera index with `currentDeviceId` to determine if a camera switch is needed (`shouldSwitchCamera`).
4. **Execution:**
   * **If camera switch is required:** Calls `capturer.switchCamera(...)`. Upon completion, updates `isFrontFacing`, updates capture target dimensions, applies capture format changes via `changeCaptureFormat()`, and signals completion via `onFinishedCallback`. If switching fails, logs error and passes exception to `onFinishedCallback`.
   * **If camera switch is NOT required:** Updates target dimensions and applies format changes directly via `capturer.changeCaptureFormat()`, then triggers `onFinishedCallback`.

---

### `createVideoCapturer()`
```java
@Override
protected VideoCapturer createVideoCapturer()
```
Implementation of the abstract method in `AbstractVideoCaptureController`.
1. Fetches `deviceId` and `facingMode` from local `constraints`.
2. Calls the private overloaded `createVideoCapturer(deviceId, facingMode)`.
3. If successful, updates actual capture size via `updateActualSize()` and returns the instantiated `VideoCapturer`.

---

### `createVideoCapturer(String deviceId, String facingMode)` (Private)
```java
@Nullable
private CreateCapturerResult createVideoCapturer(String deviceId, String facingMode)
```
Constructs a new `VideoCapturer` by evaluating device availability in priority order:

1. **Explicit Device ID Selection:** If `deviceId` is valid, attempts to create a capturer for that index.
2. **Facing Mode Fallback:** If `deviceId` is invalid or creation fails, filters available cameras matching `facingMode` (`"user"` or `"environment"`) and attempts creation.
3. **General Fallback:** If facing mode matching fails, iterates over all remaining available cameras until one successfully creates a capturer.

Updates `isFrontFacing` and `currentDeviceId` on success. Returns a `CreateCapturerResult` object containing the `cameraIndex`, `cameraName`, and `videoCapturer`, or `null` if all attempts fail.

---

### `updateActualSize()` (Private)
```java
private void updateActualSize(int cameraIndex, String cameraName, VideoCapturer videoCapturer)
```
Queries the physical camera hardware to find the closest supported capture format relative to `targetWidth` and `targetHeight`:
* For **`Camera1Capturer`**: Calls `Camera1Helper.findClosestCaptureFormat(cameraIndex, targetWidth, targetHeight)`.
* For **`Camera2Capturer`**: Obtains `CameraManager` system service and calls `Camera2Helper.findClosestCaptureFormat(cameraManager, cameraName, targetWidth, targetHeight)`.

Sets `actualWidth` and `actualHeight` inherited from `AbstractVideoCaptureController` to the returned dimensions.

---

### `findCameraIndex()` (Private)
```java
private int findCameraIndex(String cameraName)
```
Iterates through `cameraEnumerator.getDeviceNames()` to match `cameraName` and return its array index. Returns `-1` if not found.

---

## Inner Classes & Event Handlers

### `cameraEventsHandler`
An anonymous instance of `CameraEventsHandler` attached when creating capturers via `cameraEnumerator.createCapturer(...)`.

```java
private final CameraEventsHandler cameraEventsHandler = new CameraEventsHandler() {
    @Override
    public void onCameraOpening(String cameraName) {
        super.onCameraOpening(cameraName);
        int cameraIndex = findCameraIndex(cameraName);
        updateActualSize(cameraIndex, cameraName, videoCapturer);
        CameraCaptureController.this.currentDeviceId = cameraIndex == -1 ? null : String.valueOf(cameraIndex);
    }
};
```
* **`onCameraOpening`**: Triggered when a camera starts opening. Finds the camera index, updates actual physical size parameters, and updates `currentDeviceId`.

---

### `CreateCapturerResult`
A static helper class used to encapsulate the result of a camera instantiation attempt.

```java
private static class CreateCapturerResult {
    public final int cameraIndex;
    public final String cameraName;
    public final VideoCapturer videoCapturer;

    public CreateCapturerResult(int cameraIndex, String cameraName, VideoCapturer videoCapturer) {
        this.cameraIndex = cameraIndex;
        this.cameraName = cameraName;
        this.videoCapturer = videoCapturer;
    }
}
```

* **Fields:**
  * `cameraIndex` (`int`): Index of the created camera.
  * `cameraName` (`String`): Identifier name of the created camera device.
  * `videoCapturer` (`VideoCapturer`): Instantiated WebRTC capturer instance.