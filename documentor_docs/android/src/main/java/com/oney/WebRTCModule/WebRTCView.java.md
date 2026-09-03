# WebRTCView Technical Documentation

## 1. Overview

`WebRTCView` is a custom Android `ViewGroup` implemented within the `com.oney.WebRTCModule` package. It acts as the native rendering component for video streams in a React Native WebRTC environment (typically matching the JS `RTCView` component). 

Its primary responsibility is host and manage an underlying WebRTC `SurfaceViewRenderer` view instance, handle layout calculations (emulating CSS `object-fit`), safely bind/unbind native WebRTC `VideoTrack` instances, manage EGL/OpenGL contexts, and communicate video dimension events back to React Native.

---

## 2. Class Signature and Hierarchy

* **Package:** `com.oney.WebRTCModule`
* **Superclass:** `android.view.ViewGroup`
* **Dependencies:** React Native Bridge (`ReactContext`, `RCTEventEmitter`, `WritableMap`), WebRTC Native SDK (`SurfaceViewRenderer`, `VideoTrack`, `MediaStream`, `ScalingType`, `RendererEvents`, `EglBase`).

---

## 3. Key Components and Fields

### Static Constants & Fields
* **`DEFAULT_SCALING_TYPE`** (`ScalingType`): Defaults to `ScalingType.SCALE_ASPECT_FIT` (aligning with HTML5 `<video>` default behavior).
* **`TAG`** (`String`): Logging tag derived from `WebRTCModule.TAG`.
* **`surfaceViewRendererInstances`** (`int`): Static counter tracking the number of created `SurfaceViewRenderer` instances across the app for debugging EGL context initialization issues.

### Instance Fields
* **`surfaceViewRenderer`** (`SurfaceViewRenderer`): The embedded WebRTC view component that renders native video frames via OpenGL.
* **`videoTrack`** (`VideoTrack`): The native WebRTC video track currently attached to the renderer.
* **`streamURL`** (`String`): Identifier/React tag representing the `MediaStream` being rendered.
* **`scalingType`** (`ScalingType`): Dictates how the video frame is scaled within the view bounds (`SCALE_ASPECT_FIT` or `SCALE_ASPECT_FILL`).
* **`mirror`** (`boolean`): Indicates whether the rendered video should be flipped horizontally.
* **`rendererAttached`** (`boolean`): Flag indicating if `surfaceViewRenderer` is actively initialized and added as a sink to the current `videoTrack`.
* **`onDimensionsChangeEnabled`** (`boolean`): Controls whether frame dimension change events are emitted to React Native.
* **`frameWidth`, `frameHeight`, `frameRotation`** (`int`): Dimensions and rotation degree of the most recently rendered frame.
* **`layoutSyncRoot`** (`Object`): A dedicated synchronization lock object guarding layout-related state (`frameWidth`, `frameHeight`, `frameRotation`, `scalingType`).

---

## 4. Lifecycle & Rendering Flow

### Window Lifecycle
Rendering resources and OpenGL context initialization are tied directly to the view's window attachment state to preserve memory and prevent leaks:

1. **`onAttachedToWindow()`**: Automatically calls `tryAddRendererToVideoTrack()`. Initializes rendering only when the view is attached to an active window.
2. **`onDetachedFromWindow()`**: Automatically calls `removeRendererFromVideoTrack()`. Tears down the renderer and unbinds from the track when the view leaves the window.

### Frame Rendering & Resolution Events
`WebRTCView` implements `RendererEvents` to react to renderer status changes:

1. **`onFirstFrameRendered()`**: Fires when the first video frame is drawn. Sets `surfaceViewRenderer`'s background color to `Color.TRANSPARENT` so the underlying video surface becomes visible.
2. **`onFrameResolutionChanged(int videoWidth, int videoHeight, int rotation)`**:
   * Evaluates if dimensions or rotation have changed.
   * Updates state variables under the `layoutSyncRoot` lock.
   * Triggers a layout update via `requestSurfaceViewRendererLayoutRunnable`.
   * If `onDimensionsChangeEnabled` is `true`, constructs a React Native event containing `width` and `height` and dispatches it via `RCTEventEmitter` with event name `"onDimensionsChange"`.

---

## 5. Layout Logic (`onLayout`)

`WebRTCView` overrides `onLayout` to enforce custom positioning based on aspect ratio and scaling type:

* **`SCALE_ASPECT_FILL` (CSS `object-fit: cover`)**:
  * Expands the `surfaceViewRenderer` child bounds to match the parent `ViewGroup` completely (`0, 0, width, height`), delegating cropping and filling to the native renderer.
* **`SCALE_ASPECT_FIT` (CSS `object-fit: contain`)**:
  * Calculates the effective aspect ratio accounting for frame rotation:
    $$\text{AspectRatio} = \begin{cases} \frac{\text{width}}{\text{height}} & \text{if rotation \% 180 == 0} \\[6pt] \frac{\text{height}}{\text{width}} & \text{otherwise} \end{cases}$$
  * Uses `RendererCommon.getDisplaySize` to calculate adjusted bounds.
  * Centers the calculated bounds within the parent `ViewGroup` (letterboxing or pillarboxing).

---

## 6. Public API Methods

| Method | Parameters | Description |
| :--- | :--- | :--- |
| **`WebRTCView`** | `Context context` | Constructor. Instantiates the view, adds child `surfaceViewRenderer`, and sets default mirroring (`false`) and scaling type (`SCALE_ASPECT_FIT`). |
| **`setMirror`** | `boolean mirror` | Configures horizontal mirroring on `surfaceViewRenderer` and requests layout update. |
| **`setObjectFit`** | `String objectFit` | Maps CSS `object-fit` string (`"cover"` $\rightarrow$ `SCALE_ASPECT_FILL`, default $\rightarrow$ `SCALE_ASPECT_FIT`) and updates scaling type. |
| **`setZOrder`** | `int zOrder` | Configures z-ordering on `surfaceViewRenderer`: <br>• `0`: Standard (`setZOrderMediaOverlay(false)`)<br>• `1`: Media Overlay (`setZOrderMediaOverlay(true)`)<br>• `2`: On Top (`setZOrderOnTop(true)`). |
| **`setOnDimensionsChange`** | `boolean enabled` | Toggles whether frame dimension changes dispatch JS events back to React Native. |

---

## 7. Internal / Package-Private Methods

### `setStreamURL(String streamURL)`
Asynchronously sets or updates the rendered stream:
1. Compares `streamURL` against current `this.streamURL`. Exits early if identical.
2. Invokes `getVideoTrackForStreamURL` on an executor thread.
3. Clears the current `VideoTrack` (via `setVideoTrack(null)`).
4. Stores the new `streamURL` and sets the retrieved new `VideoTrack`.

### `getVideoTrackForStreamURL(String streamURL, Consumer<VideoTrack> callback)`
Queries the `WebRTCModule` native module for a `MediaStream` corresponding to `streamURL`.
* Offloads execution to `ThreadUtils.runOnExecutor` to avoid blocking the UI thread.
* Extracts the first `VideoTrack` from `stream.videoTracks`.
* Posts the result back to the UI thread via `post(...)` to execute the callback.

### `setVideoTrack(VideoTrack videoTrack)`
Manages transitioning between video tracks:
* Removes sink from `oldVideoTrack` and cleans surface renderer if transitioning to a `null` track.
* Updates internal `this.videoTrack` reference.
* Attaches renderer to the new track using `tryAddRendererToVideoTrack()`.
* Clears surface to black (`cleanSurfaceViewRenderer()`) when transitioning from a `null` state.

### `tryAddRendererToVideoTrack()`
Handles OpenGL and sink setup:
* Validates preconditions: `!rendererAttached`, `videoTrack != null`, and `ViewCompat.isAttachedToWindow(this)`.
* Obtains root EGL context via `EglUtils.getRootEglBaseContext()`.
* Initializes `surfaceViewRenderer` with the EGL context and `rendererEvents`.
* Offloads `videoTrack.addSink(surfaceViewRenderer)` execution to the WebRTC executor thread.
* Increments `surfaceViewRendererInstances` static counter and sets `rendererAttached = true`.

### `removeRendererFromVideoTrack()`
Handles cleanup and sink removal:
* Checks if `rendererAttached` is `true`.
* Offloads `videoTrack.removeSink(surfaceViewRenderer)` to the executor thread (handling potential `IllegalStateException` if track was previously released).
* Releases `surfaceViewRenderer` resources and decrements `surfaceViewRendererInstances`.
* Resets cached dimensions (`frameHeight`, `frameWidth`, `frameRotation`) to `0` under lock.
* Marks `rendererAttached = false` and requests layout.

### `cleanSurfaceViewRenderer()`
Sets background color of `surfaceViewRenderer` to `Color.BLACK` and calls `clearImage()`.

### `requestSurfaceViewRendererLayout()`
Requests layout on `surfaceViewRenderer`. If the view is not currently in a layout pass (`!ViewCompat.isInLayout(this)`), it explicitly forces an `onLayout(...)` call.

---

## 8. Threading and Concurrency Model

* **UI Thread:** Manages View hierarchy actions, layout updates, event emission via `RCTEventEmitter`, and state setters.
* **Executor Thread (`ThreadUtils.runOnExecutor`):** Offloads heavy or potentially blocking WebRTC native operations (`getStreamForReactTag`, `addSink`, `removeSink`) away from the UI thread.
* **Layout State Synchronization (`layoutSyncRoot`):** Protects variable reads and writes across rendering/UI threads for `frameHeight`, `frameWidth`, `frameRotation`, and `scalingType`.