# Technical Documentation: `VideoTrackAdapter.java`

**File Location:** `android/src/main/java/com/oney/WebRTCModule/VideoTrackAdapter.java`  
**Package:** `com.oney.WebRTCModule`

---

## 1. Overview

The `VideoTrackAdapter` class is responsible for monitoring remote WebRTC `VideoTrack` instances on Android and detecting whether video frames are actively being rendered or if rendering has stalled/stopped. 

When frames stop arriving for a specified duration, the adapter considers the track **muted**. When frames resume, it considers the track **unmuted**. These state transitions trigger events sent back to the React Native JavaScript layer (`mediaStreamTrackMuteChanged`).

---

## 2. Class Architecture & Key Components

### Class Constants

| Constant | Type | Value | Description |
| :--- | :--- | :--- | :--- |
| `TAG` | `String` | `VideoTrackAdapter.class.getCanonicalName()` | Log tag used for Android logcat debugging. |
| `INITIAL_MUTE_DELAY` | `long` | `3000` | Initial delay in milliseconds before the timer task first runs after starting. |
| `MUTE_DELAY` | `long` | `1500` | Period in milliseconds between subsequent frame arrival checks. |

---

### Class Member Variables

* **`private Map<String, TrackMuteUnmuteImpl> muteImplMap`**: A `HashMap` tracking `TrackMuteUnmuteImpl` instances indexed by their WebRTC `VideoTrack` string ID (`trackId`).
* **`private Timer timer`**: A `java.util.Timer` named `"VideoTrackMutedTimer"` used to schedule periodic frame-rate checks across monitored video tracks.
* **`private final int peerConnectionId`**: Integer ID associated with the parent `PeerConnection`.
* **`private final WebRTCModule webRTCModule`**: Reference to the main `WebRTCModule` instance used to emit event maps back to React Native.

---

## 3. Public Methods

### Constructor
```java
public VideoTrackAdapter(WebRTCModule webRTCModule, int peerConnectionId)
```
Constructs a new `VideoTrackAdapter` bound to a specific `WebRTCModule` instance and a `peerConnectionId`.

---

### `addAdapter`
```java
public void addAdapter(VideoTrack videoTrack)
```
Attaches frame monitoring logic to a provided `VideoTrack`.

* **Behavior**:
  1. Extracts the track ID (`videoTrack.id()`).
  2. Checks if `muteImplMap` already contains an entry for `trackId`. If present, logs a warning and aborts execution to prevent duplicate listeners.
  3. Instantiates a new `TrackMuteUnmuteImpl` object for the given `trackId`.
  4. Registers the new implementation in `muteImplMap`.
  5. Registers `onMuteImpl` as a `VideoSink` on the `videoTrack` using `videoTrack.addSink(onMuteImpl)`.
  6. Calls `onMuteImpl.start()` to begin periodic timer execution.

---

### `removeAdapter`
```java
public void removeAdapter(VideoTrack videoTrack)
```
Detaches frame monitoring logic from a provided `VideoTrack`.

* **Behavior**:
  1. Extracts the track ID (`videoTrack.id()`).
  2. Removes and retrieves the `TrackMuteUnmuteImpl` from `muteImplMap`.
  3. If no adapter exists for `trackId`, logs a warning and aborts execution.
  4. Unbinds the implementation sink from the `VideoTrack` using `videoTrack.removeSink(onMuteImpl)`.
  5. Calls `onMuteImpl.dispose()` to cancel scheduled timer tasks and clean up resources.

---

## 4. Private Inner Class: `TrackMuteUnmuteImpl`

`TrackMuteUnmuteImpl` implements the WebRTC `VideoSink` interface to intercept video frames as they arrive.

### Fields
* **`private TimerTask emitMuteTask`**: The scheduled task that periodically checks frame count changes.
* **`private volatile boolean disposed`**: Indicates whether the implementation instance has been disposed.
* **`private AtomicInteger frameCounter`**: Thread-safe integer counting the total frames received by this sink.
* **`private boolean mutedState`**: Holds the current boolean mute state (`true` if muted, `false` if active).
* **`private final String trackId`**: The ID of the `VideoTrack` being monitored.

---

### Methods

#### `TrackMuteUnmuteImpl(String trackId)`
Constructor that sets `trackId` and initializes `frameCounter` with a new `AtomicInteger`.

#### `onFrame(VideoFrame frame)`
* **Implemented from:** `VideoSink`
* **Behavior:** Increments `frameCounter` by `1` using `frameCounter.addAndGet(1)` every time a new `VideoFrame` is received.

#### `start()`
* **Behavior**:
  1. Returns immediately if `disposed` is `true`.
  2. Synchronizes on `this` instance.
  3. Cancels any pre-existing `emitMuteTask`.
  4. Creates a new `TimerTask`:
     * Initializes `lastFrameNumber` with the current count from `frameCounter.get()`.
     * In `run()`:
       * Checks if `disposed`; exits if `true`.
       * Evaluates `isMuted`: `true` if `lastFrameNumber == frameCounter.get()` (meaning no new frames were received since the last execution).
       * If `isMuted` differs from `mutedState`:
         * Updates `mutedState = isMuted`.
         * Calls `emitMuteEvent(isMuted)`.
       * Updates `lastFrameNumber = frameCounter.get()` for the next tick.
  5. Schedules the task on `timer` with `INITIAL_MUTE_DELAY` (3000ms) delay and `MUTE_DELAY` (1500ms) interval.

#### `emitMuteEvent(boolean muted)`
* **Behavior**:
  1. Constructs a `WritableMap` containing:
     * `"pcId"` (integer): `peerConnectionId`
     * `"trackId"` (string): `trackId`
     * `"muted"` (boolean): `muted` state
  2. Logs the mute/unmute action via `Log.d`.
  3. Calls `webRTCModule.sendEvent("mediaStreamTrackMuteChanged", params)` to inform React Native.

#### `dispose()`
* **Behavior**:
  1. Sets `disposed = true`.
  2. Synchronizes on `this` instance.
  3. Cancels `emitMuteTask` if active and nullifies its reference.

---

## 5. Event Emission Specifications

When track state changes between active and muted, the adapter sends an event to JavaScript via React Native's event bridge.

* **Event Name:** `mediaStreamTrackMuteChanged`
* **Payload Structure:**

```json
{
  "pcId": <Integer>,
  "trackId": "<String>",
  "muted": <Boolean>
}
```

---

## 6. Logic Flow Diagram

```
VideoTrack.addSink(TrackMuteUnmuteImpl)
                │
                ▼
      onFrame(VideoFrame) ───► Increments frameCounter (AtomicInteger)
                │
                ▼
 Timer Task (Every 1500ms after 3000ms initial delay)
                │
                ├─► Compare frameCounter against lastFrameNumber
                │
                ├─► Equal? (No new frames) ──► isMuted = true
                └─► Changed? (Frames arrived) ─► isMuted = false
                │
                ▼
    Has isMuted state changed from mutedState?
                │
               YES
                │
                ▼
   Emit "mediaStreamTrackMuteChanged" via WebRTCModule
```