# Technical Documentation Guide: `GetUserMediaImpl.java`

## Overview

The `GetUserMediaImpl` class in `com.oney.WebRTCModule` implements local media capture operations (`getUserMedia` and `getDisplayMedia`) for the React Native WebRTC Android module. It manages camera enumeration, audio/video track instantiation, screen capture authorization via Android’s `MediaProjection` API, video effect pipeline integration, and resource disposal for local media streams.

---

## Key Components & Architecture

### Core Responsibilities
1. **Device Enumeration**: Lists available camera and audio devices using `Camera2Enumerator` or `Camera1Enumerator`.
2. **Standard Media Stream Creation (`getUserMedia`)**: Creates local audio tracks (`AudioTrack`) and video tracks (`VideoTrack`) based on constraints provided by the JavaScript layer.
3. **Screen Capture Stream Creation (`getDisplayMedia`)**: Requests screen capture permissions via `MediaProjectionManager`, starts `MediaProjectionService`, and captures display output using `ScreenCaptureController`.
4. **Track & Source Management**: Tracks created media sources, capturers, and surface helpers using the internal `TrackPrivate` class, managing their lifecycle and disposal.
5. **Video Effects**: Connects custom frame processing chains (`VideoEffectProcessor`) to camera-backed video sources.

---

## Fields & Member Variables

| Field | Type | Description |
| :--- | :--- | :--- |
| `TAG` | `String` | Logging tag (`WebRTCModule.TAG`). |
| `PERMISSION_REQUEST_CODE` | `int` | Random integer used as the `requestCode` when launching screen capture permission activities. |
| `cameraEnumerator` | `CameraEnumerator` | Interface used to query device cameras. Uses `Camera2Enumerator` if supported, falling back to `Camera1Enumerator`. |
| `reactContext` | `ReactApplicationContext` | React Native application context. |
| `tracks` | `Map<String, TrackPrivate>` | In-memory registry mapping track IDs to `TrackPrivate` instances containing their native resources. |
| `webRTCModule` | `WebRTCModule` | Parent module reference providing access to the `PeerConnectionFactory` (`mFactory`) and local stream list. |
| `displayMediaPromise` | `Promise` | Active Promise for an ongoing `getDisplayMedia()` request. |
| `mediaProjectionPermissionResultData` | `Intent` | Intent data received after user grants screen projection permission. |
| `createConfigForDefaultDisplay` | `boolean` | Flag for Android 14+ (API 34) determining whether screen capture is locked to the default display. |
| `resolutionScale` | `float` | Scale factor for screen capture resolution (clamped between `0.0f` and `1.0f`). |

---

## Method Breakdown

### Public & Package-Private Core Methods

#### `GetUserMediaImpl(WebRTCModule webRTCModule, ReactApplicationContext reactContext)`
Constructor that registers an `ActivityEventListener` on `reactContext` to intercept activity results matching `PERMISSION_REQUEST_CODE`.
* **Screen Projection Permission Handling**:
  * Rejects duplicate activity callbacks if `displayMediaPromise` is `null`.
  * If `resultCode != Activity.RESULT_OK`, rejects `displayMediaPromise` with `DOMException: NotAllowedError`.
  * On success, launches `MediaProjectionService` asynchronously with a 10-second timeout.
  * Triggers `createScreenStream()` upon successful service initialization.

#### `enumerateDevices()`
* **Returns**: `ReadableArray` containing maps of available devices.
* Queries hardware cameras using `getCameraEnumerator()`.
* Formats device descriptors into React Native map structures with keys: `facing`, `deviceId`, `groupId`, `label`, and `kind` (`"videoinput"`).
* Appends a default audio input entry (`deviceId: "audio-1"`, `kind: "audioinput"`).

#### `getUserMedia(ReadableMap constraints, Callback successCallback, Callback errorCallback)`
Executes standard camera and microphone capture requests.
* **Audio Track**: Created if `constraints` contains an `"audio"` key.
* **Video Track**: Created if `constraints` contains a `"video"` key using `CameraCaptureController`.
* **Error Handling**: Invokes `errorCallback` with `("DOMException", "AbortError")` if both audio and video tracks fail to initialize or if current Activity is `null`.
* **Success**: Creates a `MediaStream` containing created tracks and invokes `successCallback(streamId, tracksInfo)`.

#### `getDisplayMedia(ReadableMap constraints, Promise promise)`
Initiates screen capture permission request flow.
* Rejects if a screen capture operation is already pending (`displayMediaPromise != null`).
* Parses Android-specific constraints via `initializeConstraints()`.
* Requests user authorization via `MediaProjectionManager.createScreenCaptureIntent()`.
* On Android 14+ (API Level 34/`UPSIDE_DOWN_CAKE`), if `createConfigForDefaultDisplay` is set, uses `MediaProjectionConfig.createConfigForDefaultDisplay()`.

#### `mediaStreamTrackSetEnabled(String trackId, boolean enabled)`
Toggles video capture state (`startCapture()` / `stopCapture()`) on the `AbstractVideoCaptureController` associated with the specified track ID.

#### `applyConstraints(String trackId, ReadableMap constraints, Promise promise)`
Applies runtime video constraints to the `AbstractVideoCaptureController` of the given track ID. Resolves the `promise` with updated settings or rejects on failure.

#### `setVideoEffects(String trackId, ReadableArray names)`
Attaches or detaches video effect processors to a camera track.
* Resolves processor names using `ProcessorProvider.getProcessor(name)`.
* Instantiates a `VideoEffectProcessor` with the resolved frame processors and assigns it to the `VideoSource`.
* Clears video processors if `names` is `null`.

#### `disposeTrack(String id)`
Removes the specified track from the internal `tracks` map and invokes `dispose()` on its `TrackPrivate` instance.

#### `getTrack(String id)`
* **Returns**: `MediaStreamTrack` associated with `id`, or `null` if not found.

---

### Internal Helper Methods

#### `createAudioTrack(ReadableMap constraints)`
* Extracts audio constraints and converts them to native WebRTC `MediaConstraints`.
* Removes null-valued mandatory key-value pairs via `checkMandatoryConstraints()`.
* Creates `AudioSource` and `AudioTrack` via `PeerConnectionFactory`.
* Registers the track in the `tracks` map wrapped inside a `TrackPrivate`.

#### `createVideoTrack(AbstractVideoCaptureController videoCaptureController)`
Generic video track factory method used by both camera and screen capture flows.
1. Initializes the `VideoCapturer` on `videoCaptureController`.
2. Creates a dedicated `SurfaceTextureHelper` thread (`"CaptureThread"`) using root EGL context.
3. Attaches `TrackCapturerEventsEmitter` to forward capture events to React Native.
4. Creates native `VideoSource` and `VideoTrack` objects.
5. Registers resources in `tracks` map and starts video capture.

#### `createScreenTrack()`
* Calculates display bounds using `DisplayUtils.getDisplayMetrics()`.
* Instantiates `ScreenCaptureController` with physical width, height, permission intent data, and `resolutionScale`.
* Delegates native track construction to `createVideoTrack()`.

#### `createScreenStream()`
* Invoked after `MediaProjectionService` successfully launches.
* Builds the screen `VideoTrack` and packages it into a `MediaStream`.
* Resolves `displayMediaPromise` with `streamId` and track info objects.

#### `createStream(MediaStreamTrack[] tracks, BiConsumer<String, ArrayList<WritableMap>> successCallback)`
* Creates a native WebRTC `MediaStream` with a generated UUID.
* Adds valid `AudioTrack` and `VideoTrack` instances to the native stream.
* Populates track metadata maps (including `settings` retrieved from the video capture controller or defaults for audio).
* Stores the stream in `webRTCModule.localStreams`.
* Invokes `successCallback` with the `streamId` and track info array.

#### `initializeConstraints(ReadableMap constraints)`
Extracts Android-specific constraints from incoming options:
* `createConfigForDefaultDisplay` (`boolean`): Only parsed on API level >= 34.
* `resolutionScale` (`float`): Clamped between `0.0f` and `1.0f`.

#### `getCameraEnumerator()`
Lazy-initializes `cameraEnumerator`:
* Uses `Camera2Enumerator` if supported by device/context.
* Uses `Camera1Enumerator(false)` as fallback.

#### `checkMandatoryConstraints(MediaConstraints peerConstraints)`
Iterates through `peerConstraints.mandatory` and removes entries where `getValue()` is `null` to prevent native `PeerConnectionFactory` crash.

---

## Inner Classes & Interfaces

### 1. `TrackPrivate` (Static Private Class)
Encapsulates native WebRTC media components associated with a local track.

#### Members
* `mediaSource` (`MediaSource`): The WebRTC source (`AudioSource` or `VideoSource`).
* `track` (`MediaStreamTrack`): The native WebRTC track (`AudioTrack` or `VideoTrack`).
* `videoCaptureController` (`AbstractVideoCaptureController`): Controller managing camera or screen capture execution.
* `surfaceTextureHelper` (`SurfaceTextureHelper`): Helper thread for video frame rendering and texture management.
* `disposed` (`boolean`): Tracks disposal status.

#### `dispose()` Method Execution Order
1. Stops and disposes `videoCaptureController`.
2. Stops listening on `surfaceTextureHelper` and disposes it.
3. Disposes `mediaSource`.
4. Disposes `track`.

---

### 2. `BiConsumer<T, U>` (Interface)
Functional interface defining `void accept(T t, U u)` used internally for asynchronous completion callbacks requiring two arguments (e.g., `streamId` and `tracksInfo`).

---

## Screen Capture Execution Flow (`getDisplayMedia`)

```
JS Layer: getDisplayMedia(constraints)
       │
       ▼
GetUserMediaImpl.getDisplayMedia()
       │
       ├─► Parses options (initializeConstraints)
       └─► Launches system permission prompt (startActivityForResult)
               │
               ▼
ActivityEventListener.onActivityResult()
       │
       ├─► Checks RESULT_OK (rejects NotAllowedError on failure)
       └─► Launches MediaProjectionService (10s timeout)
               │
               ▼
GetUserMediaImpl.createScreenStream()
       │
       ├─► createScreenTrack() -> ScreenCaptureController -> createVideoTrack()
       ├─► createStream() -> Creates native MediaStream
       └─► Resolves React Native displayMediaPromise
```