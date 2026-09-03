# Technical Documentation: `ios/RCTWebRTC/WebRTCModule.h`

## Overview

The `WebRTCModule.h` header file defines the interface for `WebRTCModule`, an Objective-C class that acts as a bridge between React Native (JavaScript) and the native WebRTC framework on iOS. 

By subclassing `RCTEventEmitter` and conforming to the `RCTBridgeModule` protocol, `WebRTCModule` provides the header declarations for managing WebRTC object lifecycles (such as peer connections, streams, and tracks) and emitting WebRTC-related events to JavaScript.

---

## Imported Frameworks and Headers

- **`<AVFoundation/AVFoundation.h>`**: Provides media capture and playback functionality on iOS.
- **`<Foundation/Foundation.h>`**: Core Objective-C data structures and foundational logic.
- **`<React/RCTBridgeModule.h>`**: React Native protocol for creating native bridge modules.
- **`<React/RCTConvert.h>`**: React Native utilities for converting JS types to native Objective-C types.
- **`<React/RCTEventEmitter.h>`**: Base class enabling native modules to send asynchronous events to JavaScript.
- **`<WebRTC/WebRTC.h>`**: Core Google WebRTC library header for iOS.

---

## Event Constants

The file defines several static string constants used to identify events emitted across the React Native bridge. These event names notify the JavaScript layer about state changes, network updates, data channel events, and media track updates.

### Peer Connection Events
| Constant Identifier | Event String Value | Description |
| :--- | :--- | :--- |
| `kEventPeerConnectionSignalingStateChanged` | `"peerConnectionSignalingStateChanged"` | Emitted when a peer connection's signaling state changes. |
| `kEventPeerConnectionStateChanged` | `"peerConnectionStateChanged"` | Emitted when a peer connection's overall connection state changes. |
| `kEventPeerConnectionOnRenegotiationNeeded` | `"peerConnectionOnRenegotiationNeeded"` | Emitted when session renegotiation is required. |
| `kEventPeerConnectionIceConnectionChanged` | `"peerConnectionIceConnectionChanged"` | Emitted when the ICE connection state changes. |
| `kEventPeerConnectionIceGatheringChanged` | `"peerConnectionIceGatheringChanged"` | Emitted when the ICE gathering state changes. |
| `kEventPeerConnectionGotICECandidate` | `"peerConnectionGotICECandidate"` | Emitted when a new ICE candidate is generated. |
| `kEventPeerConnectionDidOpenDataChannel` | `"peerConnectionDidOpenDataChannel"` | Emitted when a remote peer opens an RTCDataChannel. |
| `kEventPeerConnectionOnRemoveTrack` | `"peerConnectionOnRemoveTrack"` | Emitted when a track is removed from a peer connection. |
| `kEventPeerConnectionOnTrack` | `"peerConnectionOnTrack"` | Emitted when a new track is added/received on a peer connection. |

### Data Channel Events
| Constant Identifier | Event String Value | Description |
| :--- | :--- | :--- |
| `kEventDataChannelDidChangeBufferedAmount` | `"dataChannelDidChangeBufferedAmount"` | Emitted when the buffered amount of data in an RTCDataChannel changes. |
| `kEventDataChannelStateChanged` | `"dataChannelStateChanged"` | Emitted when the state of an RTCDataChannel changes (e.g., open, closed). |
| `kEventDataChannelReceiveMessage` | `"dataChannelReceiveMessage"` | Emitted when a message is received over an RTCDataChannel. |

### Media Stream Track Events
| Constant Identifier | Event String Value | Description |
| :--- | :--- | :--- |
| `kEventMediaStreamTrackMuteChanged` | `"mediaStreamTrackMuteChanged"` | Emitted when a media stream track's mute status changes. |
| `kEventMediaStreamTrackEnded` | `"mediaStreamTrackEnded"` | Emitted when a media stream track ends. |

---

## Class Interface: `WebRTCModule`

```objc
@interface WebRTCModule : RCTEventEmitter<RCTBridgeModule>
```

`WebRTCModule` inherits from `RCTEventEmitter` to send events to JavaScript and implements the `<RCTBridgeModule>` protocol to expose methods to React Native.

### Properties

- **`dispatch_queue_t workerQueue`**
  - A GCD dispatch queue used for executing WebRTC operations in the background.

- **`RTCPeerConnectionFactory *peerConnectionFactory`**
  - The central factory object used to create `RTCPeerConnection`, `RTCMediaStream`, and `RTCMediaStreamTrack` instances.

- **`id<RTCVideoDecoderFactory> decoderFactory`**
  - Hardware/software video decoder factory for handling incoming video streams.

- **`id<RTCVideoEncoderFactory> encoderFactory`**
  - Hardware/software video encoder factory for processing outgoing video streams.

- **`NSMutableDictionary<NSNumber *, RTCPeerConnection *> *peerConnections`**
  - Dictionary mapping numeric peer connection IDs (`NSNumber *`) to active `RTCPeerConnection` instances.

- **`NSMutableDictionary<NSString *, RTCMediaStream *> *localStreams`**
  - Dictionary mapping string IDs (`NSString *`) to active local `RTCMediaStream` instances.

- **`NSMutableDictionary<NSString *, RTCMediaStreamTrack *> *localTracks`**
  - Dictionary mapping string IDs (`NSString *`) to active local `RTCMediaStreamTrack` instances.

---

## Instance Methods

### `streamForReactTag:`

```objc
- (RTCMediaStream *)streamForReactTag:(NSString *)reactTag;
```

#### Purpose
Retrieves an `RTCMediaStream` instance associated with a specific React tag string identifier.

#### Parameters
- **`reactTag`**: An `NSString *` representing the unique key or tag associated with a media stream.

#### Returns
- **`RTCMediaStream *`**: The corresponding media stream instance, or `nil` if no stream exists for the provided tag.