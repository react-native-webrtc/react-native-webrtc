# Technical Documentation: `RTCRtpSender.ts`

## Overview

The `RTCRtpSender` class controls the transmission of media data (audio or video) for a specific track sent via a WebRTC peer connection in React Native. It interfaces directly with native code via React Native's `NativeModules.WebRTCModule` to inspect capabilities, manage RTP parameters, replace media tracks dynamically, and retrieve real-time statistics.

---

## File Dependencies

* **React Native**: Imports `NativeModules` to interact with native platform implementations (`WebRTCModule`).
* **MediaStreamTrack**: Represents the media source (audio or video track) assigned to the sender.
* **RTCRtpCapabilities**: Type/Class representing supported native codecs and header extensions.
* **RTCRtpSendParameters**: Type/Class defining RTP configuration settings for sending media.

---

## Class Definition: `RTCRtpSender`

### Class Properties

| Property | Type | Access | Description |
| :--- | :--- | :--- | :--- |
| `_id` | `string` | Internal | Unique identifier for the RTP sender instance. |
| `_track` | `MediaStreamTrack \| null` | Internal | The `MediaStreamTrack` currently associated with this sender (defaults to `null`). |
| `_peerConnectionId` | `number` | Internal | Identifier for the native peer connection instance that owns this sender. |
| `_rtpParameters` | `RTCRtpSendParameters` | Internal | The current RTP parameters configured for this sender. |

---

## Constructor

```typescript
constructor(info: {
    peerConnectionId: number,
    id: string,
    track?: MediaStreamTrack,
    rtpParameters: RTCRtpSendParametersInit
})
```

### Parameters (`info` object)
* **`peerConnectionId`** (`number`): The native peer connection ID.
* **`id`** (`string`): The unique sender string identifier.
* **`track`** (`MediaStreamTrack`, optional): The initial track to attach to the sender.
* **`rtpParameters`** (`RTCRtpSendParametersInit`): Initial RTP send parameter configuration object used to instantiate an `RTCRtpSendParameters` instance.

### Behavior
1. Stores `peerConnectionId` and `id`.
2. Initializes `_rtpParameters` by passing `info.rtpParameters` to `new RTCRtpSendParameters()`.
3. If `info.track` is provided, assigns it to `_track`.

---

## Read-Only Properties (Getters)

### `track`
* **Type**: `MediaStreamTrack | null`
* **Description**: Returns the media stream track currently associated with this sender instance.

### `id`
* **Type**: `string`
* **Description**: Returns the unique identifier of the sender instance.

---

## Static Methods

### `getCapabilities(kind: 'audio' | 'video'): RTCRtpCapabilities`

Queries the native platform for supported media encoding capabilities for the specified media type.

* **Parameters**:
  * `kind` (`'audio' | 'video'`): The media kind to query capabilities for.
* **Returns**: `RTCRtpCapabilities` returned directly from `WebRTCModule.senderGetCapabilities(kind)`.

---

## Instance Methods

### `replaceTrack(track: MediaStreamTrack | null): Promise<void>`

Replaces the track currently being transmitted with a new track without renegotiating the connection. Pass `null` to stop transmitting media.

* **Parameters**:
  * `track` (`MediaStreamTrack | null`): The new track to send, or `null` to detach the current track.
* **Behavior**:
  1. Invokes the native bridge method `WebRTCModule.senderReplaceTrack(this._peerConnectionId, this._id, track ? track.id : null)`.
  2. If the native call throws an error, the exception is caught, execution terminates silently, and `_track` remains unchanged.
  3. If successful, updates the internal `_track` reference to the provided `track`.
* **Returns**: A `Promise<void>` that resolves when the operation completes.

---

### `getParameters(): RTCRtpSendParameters`

* **Returns**: `RTCRtpSendParameters` — The current in-memory RTP send parameter settings associated with this sender.

---

### `setParameters(parameters: RTCRtpSendParameters): Promise<void>`

Updates the sender's RTP configuration (such as encodings or degradation preferences) on the native side.

* **Parameters**:
  * `parameters` (`RTCRtpSendParameters`): The updated send parameters to apply.
* **Behavior**:
  1. Serializes and deserializes `parameters` (`JSON.parse(JSON.stringify(parameters))`) to strip out private/underscore properties prior to passing through the bridge.
  2. Calls native method `WebRTCModule.senderSetParameters(this._peerConnectionId, this._id, _params)`.
  3. Updates internal `_rtpParameters` with a new instance of `RTCRtpSendParameters` constructed from the native response `newParameters`.
* **Returns**: A `Promise<void>` that resolves when parameters are updated.

---

### `getStats(): Promise<Map<string, any>>`

Retrieves sender-specific statistics from the native layer.

* **Behavior**:
  1. Calls `WebRTCModule.senderGetStats(this._peerConnectionId, this._id)`.
  2. Receives a serialized JSON string representing a map of statistics reports.
  3. Parses the string using `JSON.parse(data)` and converts it into a JavaScript `Map`.
* **Implementation Note**: Passing stats across the React Native bridge as a single JSON string is significantly faster on iOS and Android compared to passing structured map objects. This minimizes congestion over the bridge, preserving UI responsiveness.
* **Returns**: A `Promise` resolving to a `Map` of statistics data.

---

## Native Bridge Interoperability

This class relies on `NativeModules.WebRTCModule` for the following native bridge methods:

* `WebRTCModule.senderReplaceTrack(peerConnectionId, senderId, trackId)`
* `WebRTCModule.senderGetCapabilities(kind)`
* `WebRTCModule.senderSetParameters(peerConnectionId, senderId, parameters)`
* `WebRTCModule.senderGetStats(peerConnectionId, senderId)`