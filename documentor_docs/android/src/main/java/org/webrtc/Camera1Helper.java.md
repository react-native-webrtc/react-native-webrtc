# Camera1Helper Documentation

**File Path:** `android/src/main/java/org/webrtc/Camera1Helper.java`  
**Package:** `org.webrtc`  
**License:** Apache License 2.0 (Copyright 2023-2024 LiveKit, Inc.)

---

## Overview

`Camera1Helper` is a utility class designed to expose package-protected or helper methods from WebRTC's Android Camera1 enumeration utilities. It provides static helper functions to bridge access to underlying camera index resolution, supported format retrieval, and resolution selection.

In the context of the `Camera1XXX` classes, `cameraId` refers specifically to the integer index of a camera within the enumerated list of available devices on an Android system.

---

## Key Components & Methods

### 1. `getCameraId`

```java
public static int getCameraId(String deviceName)
```

* **Purpose:** Translates a string-based camera device name into an integer index usable by Camera1 API functions.
* **Parameters:**
  * `deviceName` (`String`): The unique identifier/name string representing a camera device.
* **Returns:** `int` — The integer index corresponding to the given device name.
* **Internal Logic:** Delegates directly to `Camera1Enumerator.getCameraIndex(deviceName)`.

---

### 2. `getSupportedFormats`

```java
@Nullable
public static List<CameraEnumerationAndroid.CaptureFormat> getSupportedFormats(int cameraId)
```

* **Purpose:** Retrieves a list of hardware-supported capture formats for a specific camera index.
* **Parameters:**
  * `cameraId` (`int`): The integer index of the camera device.
* **Returns:** `List<CameraEnumerationAndroid.CaptureFormat>` (Nullable) — A list of supported `CaptureFormat` objects, or `null` if formats cannot be retrieved.
* **Internal Logic:** Delegates directly to `Camera1Enumerator.getSupportedFormats(cameraId)`.

---

### 3. `findClosestCaptureFormat`

```java
public static Size findClosestCaptureFormat(int cameraId, int width, int height)
```

* **Purpose:** Finds the hardware-supported capture `Size` (resolution) that best matches or is closest to a requested target width and height.
* **Parameters:**
  * `cameraId` (`int`): The integer index of the camera device.
  * `width` (`int`): The desired frame width in pixels.
  * `height` (`int`): The desired frame height in pixels.
* **Returns:** `Size` — The closest supported `Size` object available for the given camera.
* **Internal Logic:**
  1. Calls `getSupportedFormats(cameraId)` to retrieve available capture formats.
  2. Extracts resolution dimensions into a `List<Size>` collection (`new Size(format.width, format.height)`).
  3. Calls `CameraEnumerationAndroid.getClosestSupportedSize(...)` using the generated size list and target `width`/`height` to compute and return the optimal size.

---

## Internal Dependencies

The class strictly references the following WebRTC framework types:
* `org.webrtc.Camera1Enumerator`: Performs the underlying enumeration and index mappings for legacy Camera1 devices.
* `org.webrtc.CameraEnumerationAndroid`: Provides data structures (`CaptureFormat`) and static helper utilities (`getClosestSupportedSize`).
* `org.webrtc.Size`: Represents width and height dimensions.