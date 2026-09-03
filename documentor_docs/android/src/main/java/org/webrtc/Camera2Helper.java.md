# Technical Documentation: `Camera2Helper.java`

## Overview

The `Camera2Helper` class is a utility class within the `org.webrtc` package. Its primary purpose is to expose helper functions that bridge Android's `CameraManager` API with WebRTC's internal camera enumeration utilities (`Camera2Enumerator` and `CameraEnumerationAndroid`).

It provides static utility methods to retrieve supported camera capture formats and to locate the closest matching frame size for a target resolution on a given camera device.

---

## File Information

* **Path:** `android/src/main/java/org/webrtc/Camera2Helper.java`
* **Package:** `org.webrtc`
* **License:** Apache License 2.0 (Copyright 2023-2024 LiveKit, Inc.)

---

## Class Details

### `Camera2Helper`

```java
public class Camera2Helper
```

A public helper class containing static methods for camera format querying and size matching.

---

## Methods

### 1. `getSupportedFormats`

Retrieves a list of capture formats supported by a specific camera device using `Camera2Enumerator`.

#### Signature
```java
@Nullable
public static List<CameraEnumerationAndroid.CaptureFormat> getSupportedFormats(
        CameraManager cameraManager, 
        @Nullable String cameraId
)
```

#### Parameters
* **`cameraManager`** (`CameraManager`): The system `CameraManager` service instance used to access camera device capabilities.
* **`cameraId`** (`@Nullable String`): The unique identifier string for the camera device (as returned by `CameraManager.getCameraIdList()`). Can be `null`.

#### Return Value
* **`List<CameraEnumerationAndroid.CaptureFormat>`** (Nullable): A list of supported capture formats for the specified camera ID, or `null` if formats cannot be retrieved.

#### Internal Logic
Delegates the query directly to `Camera2Enumerator.getSupportedFormats(cameraManager, cameraId)`.

---

### 2. `findClosestCaptureFormat`

Finds the closest supported `Size` (width and height) for a given target resolution on a specific camera device.

#### Signature
```java
public static Size findClosestCaptureFormat(
        CameraManager cameraManager, 
        @Nullable String cameraId, 
        int width, 
        int height
)
```

#### Parameters
* **`cameraManager`** (`CameraManager`): The system `CameraManager` service instance.
* **`cameraId`** (`@Nullable String`): The unique identifier string for the target camera device.
* **`width`** (`int`): The desired target width in pixels.
* **`height`** (`int`): The desired target height in pixels.

#### Return Value
* **`Size`**: The `Size` object representing the closest supported resolution matching the target `width` and `height`.

#### Internal Logic
1. Calls `getSupportedFormats(cameraManager, cameraId)` to fetch the list of supported `CaptureFormat` objects for the camera.
2. Constructs a `List<Size>` named `sizes`.
3. If the returned formats list is not `null`, iterates through each `CaptureFormat` and extracts its `width` and `height`, creating a new `Size` object for each and appending it to `sizes`.
4. Delegates to `CameraEnumerationAndroid.getClosestSupportedSize(sizes, width, height)` to compute and return the closest matching size.

---

## Dependencies

* **Android Framework:**
  * `android.hardware.camera2.CameraManager`
* **AndroidX Annotations:**
  * `androidx.annotation.Nullable`
* **Java Standard Library:**
  * `java.util.ArrayList`
  * `java.util.List`
* **WebRTC Classes:**
  * `org.webrtc.Camera2Enumerator`
  * `org.webrtc.CameraEnumerationAndroid`
  * `org.webrtc.Size`