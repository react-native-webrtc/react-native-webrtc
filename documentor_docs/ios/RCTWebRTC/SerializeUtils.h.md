# Technical Documentation: `ios/RCTWebRTC/SerializeUtils.h`

## Overview

The `SerializeUtils.h` header file defines the interface for `SerializeUtils`, an Objective-C utility class within the `react-native-webrtc` iOS module. 

The primary purpose of `SerializeUtils` is to act as a serialization and deserialization bridge layer between native WebRTC objects (such as transceivers, senders, receivers, tracks, streams, capabilities, and parameters) and bridge-compatible data types (`NSDictionary`, `NSString`, `NSMutableArray`). This allows WebRTC object states to be transmitted back and forth between the iOS native code and the JavaScript environment.

---

## Header Imports

The header imports required WebRTC native framework components and local module headers:

* `<WebRTC/RTCMediaStreamTrack.h>`: Native track representations.
* `<WebRTC/RTCPeerConnectionFactory.h>`: WebRTC peer connection factory types.
* `<WebRTC/RTCRtpReceiver.h>`: RTP receiver representations.
* `<WebRTC/RTCRtpTransceiver.h>`: RTP transceiver and sender representations.
* `<WebRTC/RTCVideoCodecInfo.h>`: Video codec capabilities and info.
* `"WebRTCModule+RTCPeerConnection.h"`: Category header extending the WebRTC module with peer connection functionality.

---

## Interface Definition

```objc
@interface SerializeUtils : NSObject
```

`SerializeUtils` inherits directly from `NSObject` and exposes only class (`+`) methods.

---

## Key Components and Methods

### 1. Transceiver Methods

#### `transceiverToJSONWithPeerConnectionId:transceiver:`
Serializes an `RTCRtpTransceiver` object into a JSON-formatted `NSString` associated with a specific peer connection ID.

* **Parameters:**
  * `id` (`nonnull NSNumber *`): The unique identifier of the peer connection.
  * `transceiver` (`RTCRtpTransceiver *_Nonnull`): The native WebRTC transceiver instance.
* **Returns:** `NSString *_Nonnull` – A JSON string representing the transceiver.

---

#### `constructTransceiversInfoArrayWithPeerConnection:`
Constructs an array containing information for all transceivers associated with a given `RTCPeerConnection`.

* **Parameters:**
  * `peerConnection` (`RTCPeerConnection *_Nonnull`): The native peer connection object.
* **Returns:** `NSMutableArray *_Nonnull` – An array containing transceiver info objects.

---

### 2. Sender and Receiver Serialization

#### `senderToJSONWithPeerConnectionId:sender:`
Converts an `RTCRtpSender` instance to a dictionary representation associated with a peer connection ID.

* **Parameters:**
  * `id` (`nonnull NSNumber *`): The peer connection identifier.
  * `sender` (`RTCRtpSender *_Nonnull`): The native RTP sender instance.
* **Returns:** `NSDictionary *_Nonnull` – A dictionary containing serialized sender attributes.

---

#### `receiverToJSONWithPeerConnectionId:receiver:`
Converts an `RTCRtpReceiver` instance to a dictionary representation associated with a peer connection ID.

* **Parameters:**
  * `id` (`nonnull NSNumber *`): The peer connection identifier.
  * `receiver` (`RTCRtpReceiver *_Nonnull`): The native RTP receiver instance.
* **Returns:** `NSDictionary *_Nonnull` – A dictionary containing serialized receiver attributes.

---

### 3. Media Track and Stream Serialization

#### `trackToJSONWithPeerConnectionId:track:`
Serializes an `RTCMediaStreamTrack` instance into a dictionary format attached to a peer connection ID.

* **Parameters:**
  * `id` (`nonnull NSNumber *`): The peer connection identifier.
  * `track` (`RTCMediaStreamTrack *_Nonnull`): The media stream track instance (audio or video).
* **Returns:** `NSDictionary *_Nonnull` – A dictionary containing serialized track details.

---

#### `streamToJSONWithPeerConnectionId:stream:streamReactTag:`
Serializes an `RTCMediaStream` along with its associated React Native tag into a dictionary format.

* **Parameters:**
  * `id` (`NSNumber *_Nonnull`): The peer connection identifier.
  * `stream` (`RTCMediaStream *_Nonnull`): The native media stream instance.
  * `streamReactTag` (`NSString *_Nonnull`): The string identifier used by React Native to reference the stream.
* **Returns:** `NSDictionary *_Nonnull` – A dictionary containing stream details.

---

### 4. Capabilities and Codec Serialization

#### `capabilitiesToJSON:`
Converts an `RTCRtpCapabilities` object into a dictionary format.

* **Parameters:**
  * `capabilities` (`RTCRtpCapabilities *_Nonnull`): The RTP capabilities instance.
* **Returns:** `NSDictionary *_Nonnull` – Dictionary representation of RTP capabilities.

---

#### `codecCapabilityToJSON:`
Converts an individual `RTCRtpCodecCapability` instance into a dictionary format.

* **Parameters:**
  * `codec` (`RTCRtpCodecCapability *_Nonnull`): The RTP codec capability instance.
* **Returns:** `NSDictionary *_Nonnull` – Dictionary representation of the codec capability.

---

### 5. Transceiver Direction Utilities

#### `serializeDirection:`
Converts a native `RTCRtpTransceiverDirection` enum value into its corresponding string representation.

* **Parameters:**
  * `direction` (`RTCRtpTransceiverDirection`): The native direction enum (`sendrecv`, `sendonly`, `recvonly`, or `inactive`).
* **Returns:** `NSString *_Nonnull` – The serialized string representation.

---

#### `parseDirection:`
Parses a direction string representation into an `RTCRtpTransceiverDirection` enum value.

* **Parameters:**
  * `direction` (`NSString *_Nonnull`): The string representation of the direction.
* **Returns:** `RTCRtpTransceiverDirection` – The parsed enum value.

---

### 6. Transceiver Options and Parameters Parsing/Serialization

#### `parseTransceiverOptions:`
Parses a dictionary of parameters (received from JavaScript) into an `RTCRtpTransceiverInit` configuration object.

* **Parameters:**
  * `parameters` (`NSDictionary *_Nonnull`): A dictionary containing transceiver initialization options.
* **Returns:** `RTCRtpTransceiverInit *_Nonnull` – The native transceiver initialization object.

---

#### `parametersToJSON:`
Converts an `RTCRtpParameters` object into a dictionary format.

* **Parameters:**
  * `parameters` (`RTCRtpParameters *_Nonnull`): The native RTP parameters object.
* **Returns:** `NSDictionary *_Nonnull` – A dictionary containing serialized parameters.

---

## How It Works

1. **Native to JS Data Conversion:** When native WebRTC events fire or queries are executed (e.g., retrieving transceivers or track information), methods like `transceiverToJSONWithPeerConnectionId:transceiver:`, `trackToJSONWithPeerConnectionId:track:`, or `parametersToJSON:` convert native WebRTC Objective-C instances into dictionaries or strings that React Native can serialize across the bridge.
2. **JS to Native Option Parsing:** When parameters or configurations are passed from JavaScript (e.g., transceiver direction strings or transceiver init options), `parseDirection:` and `parseTransceiverOptions:` parse string/dictionary data back into WebRTC native types like `RTCRtpTransceiverDirection` or `RTCRtpTransceiverInit`.