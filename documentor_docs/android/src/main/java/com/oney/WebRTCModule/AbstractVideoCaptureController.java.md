# Technical Documentation: `AbstractVideoCaptureController`

**File Path:** `android/src/main/java/com/oney/WebRTCModule/AbstractVideoCaptureController.java`  
**Package:** `com.oney.WebRTCModule`

---

## 1. Overview

`AbstractVideoCaptureController` is an abstract base class that manages the lifecycle, configuration, and capture operations of an underlying WebRTC `VideoCapturer`. It serves as a bridge between WebRTC's video capturing mechanisms and React Native's bridge constructs (such as `ReadableMap` and `WritableMap`).

Subclasses must implement specific video capturing mechanisms (e.g., camera or screen capture) by overriding the abstract methods `createVideoCapturer()` and `getDeviceId()`.

---

## 2. Fields

| Field Name | Type | Visibility | Description |
| :--- | :--- | :--- | :--- |
| `TAG` | `String` | `private static final` | Log tag derived from the simple class name (`AbstractVideoCaptureController.class.getSimpleName()`). |
| `targetWidth` | `int` | `protected` | Target width requested for video capture. |
| `targetHeight` | `int` | `protected` | Target height requested for video capture. |
| `targetFps` | `int` | `protected` | Target frames per second (FPS) requested for video capture. |
| `actualWidth` | `int` | `protected` | Current actual width of the video track. |
| `actualHeight` | `int` | `protected` | Current actual height of the video track. |
| `actualFps` | `int` | `protected` | Current actual frame rate of the video track. |
| `videoCapturer` | `VideoCapturer` | `protected` | The underlying org.webrtc.`VideoCapturer` managed by this controller instance. |
| `capturerEventsListener` | `CapturerEventsListener` | `protected` | Listener for receiving events regarding the capturer state. |

---

## 3. Constructor

### `AbstractVideoCaptureController(int width, int height, int fps)`

Initializes the target and actual dimension and frame rate variables with the specified parameters.

* **Parameters:**
  * `width` (`int`): Initial target and actual width.
  * `height` (`int`): Initial target and actual height.
  * `fps` (`int`): Initial target and actual frame rate.

---

## 4. Method Documentation

### Initialization & Lifecycle Methods

#### `initializeVideoCapturer()`
* **Signature:** `public void initializeVideoCapturer()`
* **Description:** Initializes the `videoCapturer` field by invoking the abstract factory method `createVideoCapturer()`.

#### `dispose()`
* **Signature:** `public void dispose()`
* **Description:** Disposes of the underlying `VideoCapturer` instance if it is not `null`, releasing its resources, and resets `videoCapturer` to `null`.

#### `startCapture()`
* **Signature:** `public void startCapture()`
* **Description:** Triggers capture on the internal `videoCapturer` using `targetWidth`, `targetHeight`, and `targetFps`.
* **Exception Handling:** Catches `RuntimeException` during `videoCapturer.startCapture(...)` and logs an error message via `Log.e`.

#### `stopCapture()`
* **Signature:** `public boolean stopCapture()`
* **Description:** Stops video capture on the internal `videoCapturer`.
* **Returns:** 
  * `true` if `videoCapturer.stopCapture()` completes successfully.
  * `false` if an `InterruptedException` occurs during execution.

---

### Abstract Methods

#### `createVideoCapturer()`
* **Signature:** `protected abstract VideoCapturer createVideoCapturer()`
* **Description:** Factory method that subclasses must implement to create and return the concrete `VideoCapturer` instance.

#### `getDeviceId()`
* **Signature:** `@Nullable public abstract String getDeviceId()`
* **Description:** Returns the unique identifier for the video capture device, or `null` if not applicable/available.

---

### Getters and Configuration

#### `getHeight()`
* **Signature:** `public int getHeight()`
* **Returns:** `actualHeight` (`int`).

#### `getWidth()`
* **Signature:** `public int getWidth()`
* **Returns:** `actualWidth` (`int`).

#### `getFrameRate()`
* **Signature:** `public int getFrameRate()`
* **Returns:** `actualFps` (`int`).

#### `getVideoCapturer()`
* **Signature:** `public VideoCapturer getVideoCapturer()`
* **Returns:** The managed `VideoCapturer` object (`org.webrtc.VideoCapturer`).

#### `getSettings()`
* **Signature:** `public WritableMap getSettings()`
* **Description:** Constructs a React Native `WritableMap` containing the current settings of the capturer.
* **Returns:** A `WritableMap` populated with:
  * `"deviceId"`: Result of `getDeviceId()`
  * `"groupId"`: `""` (empty string)
  * `"height"`: Result of `getHeight()`
  * `"width"`: Result of `getWidth()`
  * `"frameRate"`: Result of `getFrameRate()`

#### `applyConstraints(ReadableMap constraints, @Nullable Consumer<Exception> onFinishedCallback)`
* **Signature:** `public void applyConstraints(ReadableMap constraints, @Nullable Consumer<Exception> onFinishedCallback)`
* **Description:** Default implementation for applying media constraints. 
* **Behavior:** By default, this class does not support dynamic constraint application. If `onFinishedCallback` is provided, it immediately passes an `UnsupportedOperationException` with the message `"This video track does not support applyConstraints."` to `onFinishedCallback.accept(...)`.

#### `setCapturerEventsListener(CapturerEventsListener listener)`
* **Signature:** `public void setCapturerEventsListener(CapturerEventsListener listener)`
* **Description:** Attaches a `CapturerEventsListener` to receive notifications regarding capturer state changes.

---

## 5. Inner Interfaces

### `CapturerEventsListener`

* **Definition:** `public interface CapturerEventsListener`
* **Description:** Callback interface used to report critical state events from the capturer.

#### Interface Methods:
* `void onCapturerEnded()`: Callback invoked when the capturer stops unexpectedly and enters an irrecoverable state.

---

## 6. How It Works Workflow

1. **Instantiation:** A concrete subclass inherits from `AbstractVideoCaptureController` and supplies the initial width, height, and frame rate through `super(width, height, fps)`.
2. **Setup:** The caller calls `initializeVideoCapturer()`, which calls the subclass implementation of `createVideoCapturer()` to instantiate the `videoCapturer`.
3. **Capture Execution:**
   * Calling `startCapture()` invokes `videoCapturer.startCapture(targetWidth, targetHeight, targetFps)`.
   * Calling `stopCapture()` halts `videoCapturer.stopCapture()`, returning `true` on successful termination or `false` on interruption.
4. **Settings Reporting:** React Native layers query `getSettings()` to retrieve a map formatted for JavaScript, including device ID, height, width, frame rate, and group ID.
5. **Teardown:** Calling `dispose()` stops and cleans up the native `videoCapturer` reference safely.