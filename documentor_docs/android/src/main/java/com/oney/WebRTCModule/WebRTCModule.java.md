# Documentation: `WebRTCModule.java`

## Overview

The `WebRTCModule` class serves as the core React Native bridge module for WebRTC capabilities on Android. Annotated with `@ReactModule(name = "WebRTCModule")`, it inherits from `ReactContextBaseJavaModule` and acts as the native entry point through which the JavaScript layer controls WebRTC functionality.

It manages the life cycle of WebRTC peer connections, local and remote media streams, media tracks, transceivers, data channels, and DTLS certificates.

---

## Class Architecture & State Management

### Key Member Fields

*   `PeerConnectionFactory mFactory`: The central WebRTC factory instance used to build peer connections, create local streams, and query RTP capabilities.
*   `VideoEncoderFactory mVideoEncoderFactory`: Factory responsible for hardware/software video encoding.
*   `VideoDecoderFactory mVideoDecoderFactory`: Factory responsible for hardware/software video decoding.
*   `AudioDeviceModule mAudioDeviceModule`: WebRTC native audio device module abstraction.
*   `SparseArray<PeerConnectionObserver> mPeerConnectionObservers`: Sparse array mapping unique integer PeerConnection IDs to their corresponding `PeerConnectionObserver` wrapper objects.
*   `Map<String, MediaStream> localStreams`: Map storing local `MediaStream` instances keyed by stream React tag/ID.
*   `static Map<String, RtcCertificatePem> mCertificates`: Thread-safe static map storing generated DTLS certificates (`RtcCertificatePem`) by unique String IDs to avoid leaking private keys across the React Native bridge.
*   `GetUserMediaImpl getUserMediaImpl`: Helper object handling audio/video stream capture, camera access, and screen capture (`getDisplayMedia`).

---

## Module Initialization

When instantiated by the React Native bridge, the constructor initializes native WebRTC settings:

1.  **Configuration Options**: Reads global module options from `WebRTCModuleOptions.getInstance()`.
2.  **Global WebRTC Initialization**: Invokes `PeerConnectionFactory.initialize()` with custom field trials, native library loader, and injectable loggers/logging severity.
3.  **Codec Factory Setup**: Configures video encoder/decoder factories:
    *   Tries hardware-accelerated factories (`H264AndSoftwareVideoEncoderFactory` and `H264AndSoftwareVideoDecoderFactory`) using `EglUtils.getRootEglBaseContext()`.
    *   Falls back to software codec factories (`SoftwareVideoEncoderFactory` / `SoftwareVideoDecoderFactory`) if EGL context is unavailable.
4.  **Audio Device Setup**: Instantiates `JavaAudioDeviceModule` if no custom `AudioDeviceModule` was provided in options.
5.  **Factory Creation**: Builds the `PeerConnectionFactory` using the configured audio module and video codec factories. Releases native ADM pointer ownership immediately as `PeerConnectionFactory` assumes ownership.
6.  **Capture Helper Setup**: Initializes `GetUserMediaImpl` with the current context and module instance.

---

## Configuration & Helper Methods

### Private Helper Methods

*   `getPeerConnection(int id)`: Resolves a `PeerConnection` instance from `mPeerConnectionObservers` given its numeric ID.
*   `sendEvent(String eventName, @Nullable ReadableMap params)`: Emits asynchronous bridge events to the JS layer via `RCTDeviceEventEmitter`.
*   `createIceServer(...)`: Overloaded helper creating `PeerConnection.IceServer` objects with optional credentials.
*   `createIceServers(ReadableArray iceServersArray)`: Parses a JS array of ICE server configuration objects into a `List<PeerConnection.IceServer>`.
*   `parseRTCConfiguration(ReadableMap map)`: Converts a JS map into WebRTC `PeerConnection.RTCConfiguration`.
    *   Enforces `Unified Plan` SDP semantics (`PeerConnection.SdpSemantics.UNIFIED_PLAN`).
    *   Enforces implicit rollbacks (`enableImplicitRollback = true`).
    *   Configures DTLS crypto options (enables GCM ciphers).
    *   Parses transport options: `iceTransportPolicy`, `bundlePolicy`, `rtcpMuxPolicy`, `certificates`, `iceCandidatePoolSize`.
    *   Parses advanced/private configuration flags: `tcpCandidatePolicy`, `candidateNetworkPolicy`, `keyType`, `continualGatheringPolicy`, `audioJitterBufferMaxPackets`, `iceConnectionReceivingTimeout`, `iceBackupCandidatePairPingInterval`, `audioJitterBufferFastAccelerate`, `pruneTurnPorts`, `presumeWritableWhenFullyRelayed`.
*   `getTransceiversInfo(PeerConnection peerConnection)`: Serializes transceiver states (`currentDirection`, `transceiverId`, `mid`, `isStopped`, RTP parameters) into a `ReadableArray` for JS negotiation/renegotiation handling.
*   `bytesToHex(byte[] bytes)`: Formats byte arrays into colon-delimited hex strings for X.509 certificate SHA-256 fingerprints.

---

## React Native Bridge Methods (`@ReactMethod`)

### 1. Peer Connection Management

*   `peerConnectionInit(ReadableMap configuration, int id)` *(Synchronous)*
    *   **Purpose**: Initializes a new `PeerConnection` with an integer ID and RTC configuration on a background thread.
    *   **Returns**: `boolean` indicating success.
*   `peerConnectionSetConfiguration(ReadableMap configuration, int id)`
    *   **Purpose**: Updates the configuration of an existing `PeerConnection`.
*   `peerConnectionRestartIce(int pcId)`
    *   **Purpose**: Triggers ICE restart on the `PeerConnection`.
*   `peerConnectionGetStats(int peerConnectionId, Promise promise)`
    *   **Purpose**: Resolves the promise with JSON-formatted stats for the entire peer connection.
*   `peerConnectionClose(int id)`
    *   **Purpose**: Closes the peer connection observer.
*   `peerConnectionDispose(int id)`
    *   **Purpose**: Disposes of the peer connection, releases its resources, and removes it from `mPeerConnectionObservers`.

### 2. SDP Negotiation

*   `peerConnectionCreateOffer(int id, ReadableMap options, Promise promise)`
    *   **Purpose**: Creates an SDP offer with specified constraints.
    *   **Resolves**: A map containing `sdpInfo`, `transceiversInfo`, and `newTransceivers`.
*   `peerConnectionCreateAnswer(int id, ReadableMap options, Promise promise)`
    *   **Purpose**: Creates an SDP answer with specified constraints.
    *   **Resolves**: A map containing `sdpInfo` and `transceiversInfo`.
*   `peerConnectionSetLocalDescription(int pcId, ReadableMap desc, Promise promise)`
    *   **Purpose**: Sets the local SDP description (or implicit offer if `desc` is null).
    *   **Resolves**: Updated local `sdpInfo` and `transceiversInfo`.
*   `peerConnectionSetRemoteDescription(int id, ReadableMap desc, Promise promise)`
    *   **Purpose**: Sets the remote SDP description.
    *   **Resolves**: Updated remote `sdpInfo`, `transceiversInfo`, and `newTransceivers`.
*   `peerConnectionAddICECandidate(int pcId, ReadableMap candidateMap, Promise promise)`
    *   **Purpose**: Adds a remote ICE candidate (`sdpMid`, `sdpMLineIndex`, `candidate`).
    *   **Resolves**: Current remote description SDP map.

### 3. Transceivers, Senders, & Receivers

*   `peerConnectionAddTransceiver(int id, ReadableMap options)` *(Synchronous)*
    *   **Purpose**: Adds a transceiver by media type string ("audio"/"video") or track ID.
    *   **Returns**: Serialized transceiver data and transceiver ordering index.
*   `peerConnectionAddTrack(int id, String trackId, ReadableMap options)` *(Synchronous)*
    *   **Purpose**: Adds a local track to a `PeerConnection` with optional stream IDs.
    *   **Returns**: Serialized transceiver order, transceiver info, and sender info.
*   `peerConnectionRemoveTrack(int id, String senderId)` *(Synchronous)*
    *   **Purpose**: Removes a track from the peer connection via its `RtpSender` ID.
*   `senderSetParameters(int id, String senderId, ReadableMap options, Promise promise)`
    *   **Purpose**: Updates RTP parameters on an `RtpSender`.
*   `senderReplaceTrack(int id, String senderId, String trackId, Promise promise)`
    *   **Purpose**: Replaces the track on an existing `RtpSender` without renegotiation.
*   `senderGetCapabilities(String kind)` *(Synchronous)*
    *   **Purpose**: Retrieves sender RTP capabilities for "audio" or "video".
*   `receiverGetCapabilities(String kind)` *(Synchronous)*
    *   **Purpose**: Retrieves receiver RTP capabilities for "audio" or "video".
*   `senderGetStats(int pcId, String senderId, Promise promise)`
    *   **Purpose**: Gets statistics specifically for an `RtpSender`.
*   `receiverGetStats(int pcId, String receiverId, Promise promise)`
    *   **Purpose**: Gets statistics specifically for an `RtpReceiver`.
*   `transceiverStop(int id, String senderId, Promise promise)`
    *   **Purpose**: Stops standard transceiver operation.
*   `transceiverSetDirection(int id, String senderId, String direction, Promise promise)`
    *   **Purpose**: Sets direction (`sendrecv`, `sendonly`, `recvonly`, `inactive`).
*   `transceiverSetCodecPreferences(int id, String senderId, ReadableArray codecPreferences)` *(Synchronous)*
    *   **Purpose**: Configures ordered codec preferences on a transceiver by matching requested codecs against native capabilities.

### 4. Media Streams & Media Tracks

*   `getUserMedia(ReadableMap constraints, Callback successCallback, Callback errorCallback)`
    *   **Purpose**: Requests user media (camera/microphone) streams, delegating to `GetUserMediaImpl`.
*   `getDisplayMedia(ReadableMap constraints, Promise promise)`
    *   **Purpose**: Requests screen capture media streams, delegating to `GetUserMediaImpl`.
*   `enumerateDevices(Callback callback)`
    *   **Purpose**: Enumerates available audio/video input and output devices.
*   `mediaStreamCreate(String id)`
    *   **Purpose**: Creates a local `MediaStream` object and saves it to `localStreams`.
*   `mediaStreamAddTrack(String streamId, int pcId, String trackId)`
    *   **Purpose**: Adds a track (local or remote) to a local stream.
*   `mediaStreamRemoveTrack(String streamId, int pcId, String trackId)`
    *   **Purpose**: Removes a track from a local stream.
*   `mediaStreamRelease(String id)`
    *   **Purpose**: Disposes of a local stream and removes it from `localStreams`.
*   `mediaStreamTrackRelease(String id)`
    *   **Purpose**: Disables and disposes of a local track via `GetUserMediaImpl`.
*   `mediaStreamTrackSetEnabled(int pcId, String id, boolean enabled)`
    *   **Purpose**: Enables or disables an audio or video track.
*   `mediaStreamTrackApplyConstraints(String id, ReadableMap constraints, Promise promise)`
    *   **Purpose**: Dynamically applies track constraints using `GetUserMediaImpl`.
*   `mediaStreamTrackSetVolume(int pcId, String id, double volume)`
    *   **Purpose**: Adjusts the volume multiplier for an `AudioTrack`.
*   `mediaStreamTrackSetVideoEffects(String id, ReadableArray names)`
    *   **Purpose**: Applies named video effects to a track via `GetUserMediaImpl`.

### 5. RTC Data Channels

*   `createDataChannel(int peerConnectionId, String label, ReadableMap config)` *(Synchronous)*
    *   **Purpose**: Creates an `RTCDataChannel` on the specified `PeerConnection`. Delegates management to `PeerConnectionObserver`.
*   `dataChannelSend(int peerConnectionId, String reactTag, String data, String type)`
    *   **Purpose**: Sends text or binary messages through the data channel.
*   `dataChannelClose(int peerConnectionId, String reactTag)`
    *   **Purpose**: Closes the specified data channel.
*   `dataChannelDispose(int peerConnectionId, String reactTag)`
    *   **Purpose**: Releases memory resources associated with the data channel.

### 6. Certificate Generation & Security

*   `generateCertificate(ReadableMap options, Promise promise)`
    *   **Purpose**: Generates an `RtcCertificatePem` DTLS certificate (RSA or ECDSA) with a specified expiration time.
    *   **Security Model**: Private key information remains stored in `mCertificates` on the native side.
    *   **Returns**: An object containing `certificateId`, `expires` timestamp, and SHA-256 certificate `fingerprints`.

### 7. Native Event Listeners

*   `addListener(String eventName)` / `removeListeners(Integer count)`
    *   **Purpose**: Boilerplate required by React Native for event emitters (`RCTDeviceEventEmitter`).

---

## Threading Model

To ensure thread safety and prevent UI thread blocking, execution logic within `WebRTCModule` is delegated to an internal WebRTC executor thread using:
*   `ThreadUtils.runOnExecutor(Runnable runnable)` for asynchronous tasks.
*   `ThreadUtils.submitToExecutor(Callable<T> callable).get()` for synchronous bridge returns (`isBlockingSynchronousMethod = true`).