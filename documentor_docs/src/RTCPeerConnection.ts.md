# Technical Documentation: `src/RTCPeerConnection.ts`

## Overview

The `src/RTCPeerConnection.ts` file defines the JavaScript/TypeScript wrapper class `RTCPeerConnection`, which implements the WebRTC `RTCPeerConnection` interface for React Native. It extends an `EventTarget` shim to handle WebRTC events and interfaces directly with the native layer via React Native's `NativeModules.WebRTCModule`.

This module manages the lifecycle of peer connections, including SDP offer/answer negotiation, ICE candidate processing, media stream track routing via RTP senders, receivers, and transceivers, as well as RTC data channel creation.

---

## Type Definitions

The module exports/defines several TypeScript types used to configure and monitor `RTCPeerConnection`:

- **`RTCSignalingState`**: `'stable' | 'have-local-offer' | 'have-remote-offer' | 'have-local-pranswer' | 'have-remote-pranswer' | 'closed'`
- **`RTCIceGatheringState`**: `'new' | 'gathering' | 'complete'`
- **`RTCPeerConnectionState`**: `'new' | 'connecting' | 'connected' | 'disconnected' | 'failed' | 'closed'`
- **`RTCIceConnectionState`**: `'new' | 'checking' | 'connected' | 'completed' | 'failed' | 'disconnected' | 'closed'`
- **`RTCDataChannelInit`**: Configuration options for a data channel (`ordered`, `maxPacketLifeTime`, `maxRetransmits`, `protocol`, `negotiated`, `id`).
- **`RTCIceServer`**: Defines ICE server settings (`credential`, `url` [deprecated], `urls`, `username`).
- **`RTCConfiguration`**: Peer connection configuration dictionary (`bundlePolicy`, `certificates`, `iceCandidatePoolSize`, `iceServers`, `iceTransportPolicy`, `rtcpMuxPolicy`).
- **`RTCPeerConnectionEventMap`**: Map of supported event names to their respective `Event` types.

---

## Class Architecture: `RTCPeerConnection`

`RTCPeerConnection` inherits from `EventTarget<RTCPeerConnectionEventMap>`.

### Public Instance Properties

| Property | Type | Initial Value | Description |
| :--- | :--- | :--- | :--- |
| `localDescription` | `RTCSessionDescription \| null` | `null` | The local session description set by `setLocalDescription`. |
| `remoteDescription` | `RTCSessionDescription \| null` | `null` | The remote session description set by `setRemoteDescription`. |
| `signalingState` | `RTCSignalingState` | `'stable'` | Current signaling state of the connection. |
| `iceGatheringState` | `RTCIceGatheringState` | `'new'` | Current state of ICE candidate gathering. |
| `connectionState` | `RTCPeerConnectionState` | `'new'` | Overall connection state. |
| `iceConnectionState` | `RTCIceConnectionState` | `'new'` | State of the ICE transport. |

### Internal Instance Properties

- **`_pcId`**: `number` — Unique identifier assigned to this connection instance from an auto-incrementing module-level counter (`nextPeerConnectionId`).
- **`_transceivers`**: `{ order: number, transceiver: RTCRtpTransceiver }[]` — Sorted list of transceivers associated with this connection.
- **`_remoteStreams`**: `Map<string, MediaStream>` — Cache of remote media streams keyed by stream ID.
- **`_pendingTrackEvents`**: `any[]` — Queue holding track events from native execution until `setRemoteDescription` resolves.

---

## Static Methods

### `generateCertificate(keygenAlgorithm)`
Generates an RSA or ECDSA `RTCCertificate`.
- **Arguments**: `keygenAlgorithm` (string or object describing algorithm name/parameters).
- **Behavior**:
  - Maps `RSASSA-PKCS1-v1_5` to key type `'RSA'`.
  - Maps `ECDSA` (or non-RSA default) to key type `'ECDSA'`.
  - Calls native method `WebRTCModule.generateCertificate(options)`.
- **Returns**: `Promise<RTCCertificate>`

---

## Constructor & Initialization

### `constructor(configuration?: RTCConfiguration)`

1. **ID Allocation**: Assigns `_pcId` using `nextPeerConnectionId++`.
2. **Configuration Sanitization**:
   - Sanitizes `iceServers`: converts deprecated `url` to `urls`, ensures `urls` is an array of lowercase strings, filters out invalid server entries.
   - Sanitizes `certificates`: maps certificate objects to extract internal `_id` values as `certificateId`.
3. **Native Initialization**: Calls `WebRTCModule.peerConnectionInit(configuration, this._pcId)`. Throws an error if native initialization returns `false`.
4. **State Setup**: Initializes `_transceivers`, `_remoteStreams`, and `_pendingTrackEvents`.
5. **Event Registration**: Calls `_registerEvents()` to bind native event listeners.

---

## Event Target Attributes (Getters / Setters)

The class provides event attribute getters and setters for W3C compatibility using `getEventAttributeValue` and `setEventAttributeValue`:

- `onconnectionstatechange`
- `onicecandidate`
- `onicecandidateerror`
- `oniceconnectionstatechange`
- `onicegatheringstatechange`
- `onnegotiationneeded`
- `onsignalingstatechange`
- `ondatachannel`
- `ontrack`
- `onerror`

---

## Public Methods

### SDP Offer / Answer Lifecycle

#### `createOffer(options?: RTCOfferOptions): Promise<RTCSessionDescriptionInit>`
Generates an SDP offer.
- Normalizes offer options via `RTCUtil.normalizeOfferOptions(options)`.
- Calls native `WebRTCModule.peerConnectionCreateOffer`.
- Instantiates any new transceivers (`newTransceivers`) returned from native, wrapping sender, receiver, and tracks.
- Updates existing transceivers using `_updateTransceivers(transceiversInfo)`.
- Returns the SDP info object.

#### `createAnswer(): Promise<RTCSessionDescriptionInit>`
Generates an SDP answer.
- Calls native `WebRTCModule.peerConnectionCreateAnswer`.
- Updates existing transceivers using `_updateTransceivers(transceiversInfo)`.
- Returns the SDP info object.

#### `setLocalDescription(sessionDescription?: RTCSessionDescription | RTCSessionDescriptionInit): Promise<void>`
Sets the local SDP description.
- Validates SDP type using `RTCUtil.isSdpTypeValid`.
- Calls native `WebRTCModule.peerConnectionSetLocalDescription`.
- Updates `this.localDescription` with a new `RTCSessionDescription` (or `null`).
- Updates transceivers, removing stopped ones if the SDP description type was `'answer'`.

#### `setRemoteDescription(sessionDescription: RTCSessionDescription | RTCSessionDescriptionInit): Promise<void>`
Sets the remote SDP description and processes queued track events.
- Rejects if `sessionDescription` is missing or has an invalid type.
- Calls native `WebRTCModule.peerConnectionSetRemoteDescription`.
- Updates `this.remoteDescription`.
- Processes newly returned transceivers (`newTransceivers`).
- Updates existing transceivers.
- **Track Event Processing**: Iterates through queued `_pendingTrackEvents` gathered during native track events:
  - Updates transceiver properties (`_mid`, `_currentDirection`, `_direction`).
  - Retrieves or constructs corresponding remote `MediaStream` objects in `_remoteStreams`.
  - Dispatches `RTCTrackEvent` (`'track'`) on the `RTCPeerConnection`.
  - Dispatches `MediaStreamTrackEvent` (`'addtrack'`) on the affected streams.
  - Calls `track._setMutedInternal(false)` to unmute the track.

---

### Configuration & ICE Handling

#### `setConfiguration(configuration: RTCConfiguration): void`
Passes new configuration options to `WebRTCModule.peerConnectionSetConfiguration`.

#### `addIceCandidate(candidate: any): Promise<void>`
Adds a remote ICE candidate.
- Throws an error if `connectionState` is `'closed'`.
- Validates that either `sdpMLineIndex` or `sdpMid` is present.
- Deeply clones candidate object and calls native `WebRTCModule.peerConnectionAddICECandidate`.
- Updates `this.remoteDescription` with returned SDP state.

#### `restartIce(): void`
Triggers an ICE restart via native `WebRTCModule.peerConnectionRestartIce`.

---

### Track & Transceiver Management

#### `addTrack(track: MediaStreamTrack, ...streams: MediaStream[]): RTCRtpSender`
Adds a `MediaStreamTrack` to the connection.
- Throws if connection is closed or if track already exists in a sender (`_trackExists`).
- Calls native `WebRTCModule.peerConnectionAddTrack`.
- If an existing sender is returned, updates its track reference and transceiver direction.
- If a new transceiver is created, constructs sender, receiver, and transceiver objects, adding them to sorted `_transceivers`.
- Returns the `RTCRtpSender`.

#### `addTransceiver(source: 'audio' | 'video' | MediaStreamTrack, init?: any): RTCRtpTransceiver`
Adds a transceiver for a media type or track.
- Formats source into `type` or `trackId`.
- Extracts stream IDs from `init.streams`.
- Calls native `WebRTCModule.peerConnectionAddTransceiver`.
- Constructs `RTCRtpSender`, `RTCRtpReceiver`, and `RTCRtpTransceiver`.
- Inserts transceiver into `_transceivers` sorted by order.
- Returns the created `RTCRtpTransceiver`.

#### `removeTrack(sender: RTCRtpSender): void`
Removes a track from an existing sender.
- Validates sender connection ownership and existence.
- Calls synchronous native `WebRTCModule.peerConnectionRemoveTrack`.
- Sets sender track reference to `null`.
- Updates transceiver direction (`sendrecv` -> `recvonly`, or `inactive`).

#### `getTransceivers(): RTCRtpTransceiver[]`
Returns array of all transceivers managed by this connection.

#### `getSenders(): RTCRtpSender[]`
Returns array of active (unstopped) senders.

#### `getReceivers(): RTCRtpReceiver[]`
Returns array of active (unstopped) receivers.

---

### Data Channels

#### `createDataChannel(label: string, dataChannelDict?: RTCDataChannelInit): RTCDataChannel`
Creates a data channel with the given label and options.
- Throws `TypeError` if label argument is omitted or `dataChannelDict.id` is not a number.
- Calls native `WebRTCModule.createDataChannel`.
- Returns a new `RTCDataChannel` instance wrapper.

---

### Statistics & Teardown

#### `getStats(selector?: MediaStreamTrack): Promise<Map<string, any>>`
Retrieves WebRTC stats.
- **Without selector**: Invokes native `WebRTCModule.peerConnectionGetStats`, parses returned JSON string into a JavaScript `Map`.
- **With selector (`MediaStreamTrack`)**: Filters senders and receivers matching selector. If exactly one match is found, invokes `getStats()` on that matching sender or receiver. Throws error if 0 or >1 matches are found.

#### `close(): void`
Closes the peer connection.
- No-op if connection is already `'closed'`.
- Calls native `WebRTCModule.peerConnectionClose`.
- Marks all transceivers as stopped via `_setStopped()`.

---

## Native Event Handling (`_registerEvents`)

The connection registers event listeners via `addListener` linked to native events targeting `this._pcId`:

1. **`peerConnectionOnRenegotiationNeeded`**:
   - Dispatches `Event('negotiationneeded')`.

2. **`peerConnectionIceConnectionChanged`**:
   - Updates `this.iceConnectionState`.
   - Dispatches `Event('iceconnectionstatechange')`.

3. **`peerConnectionStateChanged`**:
   - Updates `this.connectionState`.
   - Dispatches `Event('connectionstatechange')`.

4. **`peerConnectionSignalingStateChanged`**:
   - Updates `this.signalingState`.
   - Dispatches `Event('signalingstatechange')`.
   - If state is `'closed'`, unbinds native listeners via `removeListener(this)` and disposes native resources via `WebRTCModule.peerConnectionDispose`.

5. **`peerConnectionOnTrack`**:
   - Pushes event payload to `this._pendingTrackEvents` to be processed inside `setRemoteDescription`.

6. **`peerConnectionOnRemoveTrack`**:
   - Removes track from streams inside `_remoteStreams`.
   - Dispatches `MediaStreamTrackEvent('removetrack')` on affected streams.
   - Sets track muted state to `true` via `track._setMutedInternal(true)`.

7. **`peerConnectionGotICECandidate`**:
   - Updates `this.localDescription`.
   - Dispatches `RTCIceCandidateEvent('icecandidate')` containing the candidate.

8. **`peerConnectionIceGatheringChanged`**:
   - Updates `this.iceGatheringState`.
   - If state is `'complete'`, updates `this.localDescription` and dispatches `RTCIceCandidateEvent('icecandidate')` with `null` candidate.
   - Dispatches `Event('icegatheringstatechange')`.

9. **`peerConnectionDidOpenDataChannel`**:
   - Creates `RTCDataChannel`.
   - Dispatches `RTCDataChannelEvent('datachannel')` on `RTCPeerConnection`.
   - Dispatches `RTCDataChannelEvent('open')` directly on the channel.

10. **`mediaStreamTrackMuteChanged`**:
    - Finds receiver track by ID and updates its internal mute state via `track._setMutedInternal(ev.muted)`.

---

## Private Helper Methods

### `_trackExists(track: MediaStreamTrack): boolean`
Checks whether a `MediaStreamTrack` is already associated with an existing active sender in `_transceivers`.

### `_updateTransceivers(transceiverUpdates: any[], removeStopped = false): void`
Updates existing `RTCRtpTransceiver` instances matching transceiver IDs with updated native states (`currentDirection`, `mid`, `stopped`, sender/receiver RTP parameters). Optional `removeStopped` flag filters out stopped transceivers from `_transceivers`.

### `_insertTransceiverSorted(order: number, transceiver: RTCRtpTransceiver): void`
Appends a transceiver and its relative creation order index to `_transceivers`, sorting the array ascending by `order`.