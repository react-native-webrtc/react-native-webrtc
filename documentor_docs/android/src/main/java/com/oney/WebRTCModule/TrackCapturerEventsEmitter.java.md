# Technical Documentation: `TrackCapturerEventsEmitter.java`

**File Location:** `android/src/main/java/com/oney/WebRTCModule/TrackCapturerEventsEmitter.java`  
**Package:** `com.oney.WebRTCModule`

---

## Overview

`TrackCapturerEventsEmitter` is an Android Java class responsible for listening to video capturer lifecycle events and emitting corresponding track-ending events to the React Native JavaScript side via `WebRTCModule`. 

It implements the `AbstractVideoCaptureController.CapturerEventsListener` interface, acting as a bridge between native Android video capture termination logic and React Native event dispatching.

---

## Class Architecture & Definition

```java
public class TrackCapturerEventsEmitter implements AbstractVideoCaptureController.CapturerEventsListener
```

### Implemented Interfaces
* **`AbstractVideoCaptureController.CapturerEventsListener`**: Provides callback hooks for video capture lifecycle events.

---

## Class Fields

| Field | Type | Access | Description |
| :--- | :--- | :--- | :--- |
| `TAG` | `String` | `private static final` | Stores the canonical name of the class (`TrackCapturerEventsEmitter.class.getCanonicalName()`), used for Android logging. |
| `webRTCModule` | `WebRTCModule` | `private final` | Reference to the `WebRTCModule` instance used to dispatch events to JavaScript. |
| `trackId` | `String` | `private final` | The unique identifier of the media stream track associated with this event emitter. |

---

## Constructor

```java
public TrackCapturerEventsEmitter(WebRTCModule webRTCModule, String trackId)
```

### Parameters
* **`webRTCModule`** (`WebRTCModule`): The module instance responsible for sending events to the React Native bridge.
* **`trackId`** (`String`): The ID of the media track being observed.

### Behavior
Initializes the private `webRTCModule` and `trackId` fields with the provided arguments.

---

## Methods

### `onCapturerEnded()`

```java
@Override
public void onCapturerEnded()
```

#### Description
Callback method triggered when the video capturer stops or finishes its operation. It constructs an event payload and triggers an event dispatch through `WebRTCModule`.

#### Implementation Details
1. **Creates Event Payload:** Initializes a React Native `WritableMap` using `Arguments.createMap()`.
2. **Populates Payload:** Inserts the `trackId` string property into the map with key `"trackId"`.
3. **Logs Debug Message:** Emits a debug log message (`Log.d`) using `TAG` with the message `"ended event trackId: <trackId>"`.
4. **Emits Event:** Calls `webRTCModule.sendEvent("mediaStreamTrackEnded", params)`, sending the event name `"mediaStreamTrackEnded"` along with the payload map to the React Native side.

---

## Workflow Summary

```
[ Video Capturer Ends ]
          │
          ▼
`onCapturerEnded()` called
          │
          ├─► Construct `WritableMap` { "trackId": trackId }
          ├─► Log debug details via Android `Log.d`
          │
          ▼
Calls `webRTCModule.sendEvent("mediaStreamTrackEnded", params)`
          │
          ▼
[ Sent to React Native Layer ]
```

---

## Dependencies & Imports

* **Android Logging:** `android.util.Log`
* **React Native Bridge Utilities:**
  * `com.facebook.react.bridge.Arguments`
  * `com.facebook.react.bridge.WritableMap`
* **WebRTC Imports:**
  * `org.webrtc.CapturerObserver`
  * `org.webrtc.VideoFrame`