# Documentation: `PeerConnectionObserver.java`

## Overview

The `PeerConnectionObserver` class acts as an event bridge and manager for an individual WebRTC `PeerConnection` instance on Android. It implements WebRTC's native `PeerConnection.Observer` interface, capturing native WebRTC callbacks, mapping native WebRTC data types into React Native bridge formats (`WritableMap`, `WritableArray`), and forwarding events to the React Native JavaScript layer via `WebRTCModule`.

It also provides wrapper methods for creating and managing WebRTC `DataChannel`s, retrieving WebRTC connection statistics, and managing RTP senders and transceivers.

---

## Key Responsibilities

1. **Native WebRTC Event Observation:** Listens for native events (ICE candidates, connection state changes, signaling changes, track additions/removals, remote data channels).
2. **React Native Event Dispatching:** Serializes WebRTC events and forwards them to the JS layer via `webRTCModule.sendEvent(...)`.
3. **Data Channel Lifecycle & I/O:** Handles creation, sending text/binary data, closing, and disposing of data channels.
4. **RTP Transceiver & Sender Lookup:** Provides helpers to retrieve senders/transceivers and add media/track transceivers.
5. **Statistics Aggregation:** Wraps global, receiver-specific, and sender-specific `getStats()` API calls and resolves React Native `Promise` instances with JSON statistics.
6. **Track & Stream Tracking:** Maintains local mappings of remote stream React tags, remote streams, and remote tracks, while managing video adapters using `VideoTrackAdapter`.

---

## Member Variables

| Variable | Type | Description |
| :--- | :--- | :--- |
| `TAG` | `String` | Logging tag inherited from `WebRTCModule.TAG`. |
| `dataChannels` | `Map<String, DataChannelWrapper>` | Maps unique React tag IDs to their corresponding `DataChannelWrapper` instances. |
| `id` | `int` | Unique identifier for this peer connection observer instance. |
| `transceiverNextId` | `int` | Internal counter used to generate sequential ordering indices for transceivers. |
| `peerConnection` | `PeerConnection` | Reference to the native WebRTC `PeerConnection` instance being observed. |
| `remoteStreamIds` | `Map<String, String>` | Maps native WebRTC stream IDs to React Native tag strings. |
| `remoteStreams` | `Map<String, MediaStream>` | Maps React Native stream tags to native `MediaStream` objects. |
| `remoteTracks` | `Map<String, MediaStreamTrack>` | Maps track IDs to native `MediaStreamTrack` instances. |
| `videoTrackAdapters` | `VideoTrackAdapter` | Helper instance managing video adapters for remote video tracks. |
| `webRTCModule` | `WebRTCModule` | Reference to the parent React Native module used to send events back to JS. |

---

## Method Documentation

### Constructor & Lifecycle Methods

#### `PeerConnectionObserver(WebRTCModule webRTCModule, int id)`
Initializes the observer instance, instantiating internal maps for data channels, remote streams, remote tracks, and the `VideoTrackAdapter`.

#### `PeerConnection getPeerConnection()`
Returns the underlying native `PeerConnection` instance.

#### `void setPeerConnection(PeerConnection peerConnection)`
Sets the native `PeerConnection` reference associated with this observer.

#### `void close()`
Calls `close()` on the native `PeerConnection` instance.

#### `void dispose()`
Cleans up resources associated with the observer:
1. Removes video adapters for all registered remote `VideoTrack` instances.
2. Unregisters observers for all active `DataChannel` wrappers.
3. Disposes of the underlying native `PeerConnection`.
4. Clears internal mapping structures (`remoteStreamIds`, `remoteStreams`, `remoteTracks`, `dataChannels`).

---

### Transceiver & Sender Operations

#### `public synchronized int getNextTransceiverId()`
Increments and returns the transceiver counter (`transceiverNextId`) to provide ordering IDs.

#### `RtpTransceiver addTransceiver(MediaStreamTrack.MediaType mediaType, RtpTransceiver.RtpTransceiverInit init)`
Adds a transceiver for a media type (`AUDIO` or `VIDEO`) to the native peer connection. Returns `null` if `peerConnection` is `null`.

#### `RtpTransceiver addTransceiver(MediaStreamTrack track, RtpTransceiver.RtpTransceiverInit init)`
Adds a transceiver for a specific `MediaStreamTrack` to the native peer connection. Returns `null` if `peerConnection` is `null`.

#### `RtpSender getSender(String id)`
Searches the native peer connection's active senders and returns the `RtpSender` matching the provided ID. Returns `null` if not found or if `peerConnection` is `null`.

#### `RtpTransceiver getTransceiver(String id)`
Searches the native peer connection's transceivers and returns the `RtpTransceiver` whose sender ID matches the provided ID. Returns `null` if not found or if `peerConnection` is `null`.

---

### Data Channel Operations

#### `WritableMap createDataChannel(String label, ReadableMap config)`
Creates a native `DataChannel` on the peer connection configured via the JS `ReadableMap`.
* **Config keys evaluated:** `id`, `ordered`, `maxRetransmitTime`, `maxRetransmits`, `protocol`, `negotiated`.
* Assigns a unique UUID string as the `reactTag`.
* Wraps the channel in `DataChannelWrapper` and registers it as an observer.
* Returns a `WritableMap` containing channel metadata (`peerConnectionId`, `reactTag`, `label`, `id`, `ordered`, `maxPacketLifeTime`, `maxRetransmits`, `protocol`, `negotiated`, `readyState`).

#### `void dataChannelClose(String reactTag)`
Retrieves the `DataChannelWrapper` matching the `reactTag` and invokes `.close()` on the native `DataChannel`.

#### `void dataChannelDispose(String reactTag)`
Unregisters the observer from the `DataChannel` associated with `reactTag` and removes it from the `dataChannels` map.

#### `void dataChannelSend(String reactTag, String data, String type)`
Sends data over the specified data channel.
* If `type` is `"text"`, converts the string to UTF-8 bytes.
* If `type` is `"binary"`, decodes the string from Base64 (`Base64.NO_WRAP`).
* Wraps the byte array in a `DataChannel.Buffer` and sends it via the native channel.

---

### Statistics Operations

#### `void getStats(Promise promise)`
Fetches WebRTC stats for the entire connection (`peerConnection.getStats(...)`) and resolves the provided React Native promise with a JSON string representation processed via `StringUtils.statsToJSON`.

#### `public void receiverGetStats(String receiverId, Promise promise)`
Locates the `RtpReceiver` matching `receiverId`. If found, fetches stats specific to that receiver and resolves the promise with JSON stats. If not found, resolves with an empty JSON stats report.

#### `public void senderGetStats(String senderId, Promise promise)`
Locates the `RtpSender` matching `senderId`. If found, fetches stats specific to that sender and resolves the promise with JSON stats. If not found, resolves with an empty JSON stats report.

---

### `PeerConnection.Observer` Event Callbacks

All thread-sensitive callbacks utilize `ThreadUtils.runOnExecutor(...)` to dispatch work off the WebRTC native observer thread onto the module's executor thread.

#### `void onIceCandidate(final IceCandidate candidate)`
Fired when a new native ICE candidate is gathered.
* Constructs `candidate` map (`sdpMLineIndex`, `sdpMid`, `candidate`).
* Appends current `localDescription` SDP if available.
* Emits React Native event: **`peerConnectionGotICECandidate`**.

#### `void onIceCandidatesRemoved(final IceCandidate[] candidates)`
*Empty implementation.*

#### `void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState)`
Fired when the ICE connection state changes.
* Maps state to a string value.
* Emits React Native event: **`peerConnectionIceConnectionChanged`**.

#### `void onConnectionChange(PeerConnection.PeerConnectionState peerConnectionState)`
Fired when the overall PeerConnection state changes.
* Maps state to a string value.
* Emits React Native event: **`peerConnectionStateChanged`**.

#### `void onIceConnectionReceivingChange(boolean receiving)`
*Empty implementation.*

#### `void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState)`
Fired when the ICE gathering state changes.
* Maps state to a string value.
* If state is `COMPLETE`, includes current local SDP description.
* Emits React Native event: **`peerConnectionIceGatheringChanged`**.

#### `void onDataChannel(DataChannel dataChannel)`
Fired when a remote peer opens a data channel.
* Wraps the channel into `DataChannelWrapper` with a new UUID `reactTag`.
* Constructs metadata `info` map.
* Emits React Native event: **`peerConnectionDidOpenDataChannel`**.

#### `void onRenegotiationNeeded()`
Fired when negotiation is needed (e.g., tracks added/removed).
* Emits React Native event: **`peerConnectionOnRenegotiationNeeded`**.

#### `void onSignalingChange(PeerConnection.SignalingState signalingState)`
Fired when the signaling state changes.
* Maps state to a string value.
* Emits React Native event: **`peerConnectionSignalingStateChanged`**.

#### `void onAddTrack(final RtpReceiver receiver, final MediaStream[] mediaStreams)`
Fired when a new track is signaled by the remote peer.
1. Matches the `RtpReceiver` to its corresponding `RtpTransceiver`.
2. Registers new video tracks with `videoTrackAdapters`.
3. Maps native media streams to React tag IDs.
4. Serializes streams, receiver, and transceiver using `SerializeUtils`.
5. Emits React Native event: **`peerConnectionOnTrack`**.

#### `void onTrack(final RtpTransceiver transceiver)`
*Empty implementation* (Unified Plan callback handled via `onAddTrack`).

#### `void onRemoveTrack(RtpReceiver receiver)`
Fired when a remote track is removed.
* Emits React Native event: **`peerConnectionOnRemoveTrack`** containing `receiverId`.

#### `void onAddStream(MediaStream stream)` / `void onRemoveStream(MediaStream stream)`
*Empty implementations* (Maintained only for interface compilation; Plan B is unsupported).

---

## State Mapping Helper Functions

Internal private methods convert native C++/Java WebRTC enum states into standard lowercase strings for JavaScript consumption:

### `peerConnectionStateString(PeerConnectionState state)`
* `NEW` $\rightarrow$ `"new"`
* `CONNECTING` $\rightarrow$ `"connecting"`
* `CONNECTED` $\rightarrow$ `"connected"`
* `DISCONNECTED` $\rightarrow$ `"disconnected"`
* `FAILED` $\rightarrow$ `"failed"`
* `CLOSED` $\rightarrow$ `"closed"`

### `iceConnectionStateString(IceConnectionState state)`
* `NEW` $\rightarrow$ `"new"`
* `CHECKING` $\rightarrow$ `"checking"`
* `CONNECTED` $\rightarrow$ `"connected"`
* `COMPLETED` $\rightarrow$ `"completed"`
* `FAILED` $\rightarrow$ `"failed"`
* `DISCONNECTED` $\rightarrow$ `"disconnected"`
* `CLOSED` $\rightarrow$ `"closed"`

### `iceGatheringStateString(IceGatheringState state)`
* `NEW` $\rightarrow$ `"new"`
* `GATHERING` $\rightarrow$ `"gathering"`
* `COMPLETE` $\rightarrow$ `"complete"`

### `signalingStateString(SignalingState state)`
* `STABLE` $\rightarrow$ `"stable"`
* `HAVE_LOCAL_OFFER` $\rightarrow$ `"have-local-offer"`
* `HAVE_LOCAL_PRANSWER` $\rightarrow$ `"have-local-pranswer"`
* `HAVE_REMOTE_OFFER` $\rightarrow$ `"have-remote-offer"`
* `HAVE_REMOTE_PRANSWER` $\rightarrow$ `"have-remote-pranswer"`
* `CLOSED` $\rightarrow$ `"closed"`

---

## Summary of Events Sent to React Native

| Native Event Name | Trigger Condition | Payload Data |
| :--- | :--- | :--- |
| `peerConnectionGotICECandidate` | Native `onIceCandidate` callback | `pcId`, `candidate` object (`sdpMLineIndex`, `sdpMid`, `candidate`), `sdp` object (`type`, `sdp`). |
| `peerConnectionIceConnectionChanged` | Native `onIceConnectionChange` callback | `pcId`, `iceConnectionState` (string). |
| `peerConnectionStateChanged` | Native `onConnectionChange` callback | `pcId`, `connectionState` (string). |
| `peerConnectionIceGatheringChanged` | Native `onIceGatheringChange` callback | `pcId`, `iceGatheringState` (string), optional `sdp` object if COMPLETE. |
| `peerConnectionDidOpenDataChannel` | Native `onDataChannel` callback | `pcId`, `dataChannel` map (channel parameters and state). |
| `peerConnectionOnRenegotiationNeeded` | Native `onRenegotiationNeeded` callback | `pcId`. |
| `peerConnectionSignalingStateChanged` | Native `onSignalingChange` callback | `pcId`, `signalingState` (string). |
| `peerConnectionOnTrack` | Native `onAddTrack` callback | `pcId`, `streams` array, `receiver` map, `transceiver` map, `transceiverOrder`. |
| `peerConnectionOnRemoveTrack` | Native `onRemoveTrack` callback | `pcId`, `receiverId`. |