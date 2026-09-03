# Documentation: `src/RTCRtpReceiver.ts`

## Overview

The `RTCRtpReceiver` class manages the reception of media tracks over an RTP connection within the React Native WebRTC library. It wraps native module interactions to inspect receiver capabilities, fetch statistical data, retrieve active receive parameters, and access the associated `MediaStreamTrack`.

---

## Dependencies & Imports

* **`NativeModules`** (from `'react-native'`): Used to access `WebRTCModule` for native bridge calls.
* **`MediaStreamTrack`** (from `'./MediaStreamTrack'`): Represents the audio or video track associated with this receiver.
* **`RTCRtpCapabilities`** (from `'./RTCRtpCapabilities'`): Type/class representing media capabilities for audio or video.
* **`RTCRtpParametersInit`** (from `'./RTCRtpParameters'`): Type definition for initializing RTP parameters.
* **`RTCRtpReceiveParameters`** (from `'./RTCRtpReceiveParameters'`): Class that holds receiver-specific RTP parameters.

---

## Class Member Variables

| Variable | Type | Description |
| :--- | :--- | :--- |
| `_id` | `string` | The unique identifier for the RTP receiver. |
| `_peerConnectionId` | `number` | The identifier of the associated native `RTCPeerConnection`. |
| `_track` | `MediaStreamTrack \| null` | The `MediaStreamTrack` object associated with this receiver. Defaults to `null`. |
| `_rtpParameters` | `RTCRtpReceiveParameters` | The active parameters configured for receiving media. |

---

## Constructor

```typescript
constructor(info: {
    peerConnectionId: number,
    id: string,
    track?: MediaStreamTrack,
    rtpParameters: RTCRtpParametersInit
})
```

### Parameters
The constructor takes a single configuration object `info` with the following properties:

* **`peerConnectionId`** (`number`): The ID of the native peer connection owning this receiver.
* **`id`** (`string`): The unique string identifier for this receiver.
* **`track`** (`MediaStreamTrack`, optional): The underlying media stream track associated with this receiver instance.
* **`rtpParameters`** (`RTCRtpParametersInit`): Initial parameter configuration used to construct an instance of `RTCRtpReceiveParameters`.

### Behavior
1. Assigns `_id` and `_peerConnectionId` from `info`.
2. Instantiates `_rtpParameters` via `new RTCRtpReceiveParameters(info.rtpParameters)`.
3. Sets `_track` if `info.track` is provided.

---

## Static Methods

### `getCapabilities(kind)`

```typescript
static getCapabilities(kind: 'audio' | 'video'): RTCRtpCapabilities
```

Queries the native module (`WebRTCModule`) for the media capabilities supported by the underlying system for a given media kind.

* **Parameters:**
  * `kind` (`'audio' | 'video'`): The type of media track.
* **Returns:** `RTCRtpCapabilities` — Object containing supported codecs and header extensions returned by `WebRTCModule.receiverGetCapabilities(kind)`.

---

## Instance Methods

### `getStats()`

```typescript
getStats(): Promise<Map<any, any>>
```

Retrieves performance and diagnostic statistics for this specific receiver instance from native code.

* **Returns:** `Promise<Map<string, any>>` — A promise resolving to a JavaScript `Map` of report items parsed from JSON data.
* **Internal Behavior & Performance Optimization:**
  * Invokes `WebRTCModule.receiverGetStats(this._peerConnectionId, this._id)`.
  * The native layer (Android/iOS) serializes the native Map of StatsReports into a single JSON string before sending it across the React Native bridge.
  * The returned Promise parses this JSON string (`JSON.parse(data)`) and constructs a JavaScript `Map` instance. Passing a single string across the RN bridge avoids performance bottlenecks and UI congestion.

---

### `getParameters()`

```typescript
getParameters(): RTCRtpReceiveParameters
```

* **Returns:** `RTCRtpReceiveParameters` — The active RTP parameters (`_rtpParameters`) for this receiver.

---

## Getters

### `id`

```typescript
get id(): string
```

* **Returns:** `string` — The unique ID (`_id`) assigned to this receiver.

---

### `track`

```typescript
get track(): MediaStreamTrack | null
```

* **Returns:** `MediaStreamTrack | null` — The `MediaStreamTrack` associated with this receiver, or `null` if none was set.