# Technical Documentation: `SerializeUtils.java`

## Overview

The `SerializeUtils` class in `com.oney.WebRTCModule` is a utility class designed to bridge communication between the native Google WebRTC Java library (`org.webrtc.*`) and the React Native Java bridge (`com.facebook.react.bridge.*`). 

Its primary objective is twofold:
1. **Serialization:** Converting native WebRTC Java objects (such as `MediaStream`, `RtpSender`, `RtpReceiver`, `RtpTransceiver`, `RtpParameters`, and `RtpCapabilities`) into React Native `ReadableMap` and `ReadableArray` objects to send data to JavaScript.
2. **Parsing:** Parsing incoming JS parameters formatted as `ReadableMap` objects into native Java objects or enums (such as `RtpParameters`, `RtpTransceiverInit`, and `MediaStreamTrack.MediaType`) for consumption by the WebRTC native engine.

---

## Class Methods

### 1. Serialization APIs (Native WebRTC → React Native JS)

These methods construct React Native bridge objects (`WritableMap`, `WritableArray`) from native WebRTC structures.

---

#### `serializeVideoCodecInfo(VideoCodecInfo info)`
* **Parameters:** `VideoCodecInfo info`
* **Returns:** `ReadableMap`
* **Description:** Serializes `VideoCodecInfo` into a map containing a single key `mimeType` with the value `"video/" + info.name`.

---

#### `serializeStream(int pcId, String streamReactTag, MediaStream stream)`
* **Parameters:** 
  * `int pcId`: The ID of the associated PeerConnection.
  * `String streamReactTag`: React tag assigned to the stream.
  * `MediaStream stream`: The native WebRTC `MediaStream`.
* **Returns:** `ReadableMap`
* **Description:** Serializes a `MediaStream`. The resulting map contains:
  * `streamId` (`String`): The ID of the `MediaStream`.
  * `streamReactTag` (`String`): The React identifier string.
  * `tracks` (`WritableArray`): Array containing all serialized video and audio tracks belonging to the stream (processed via `serializeTrack`).

---

#### `serializeDirection(RtpTransceiver.RtpTransceiverDirection src)`
* **Parameters:** `RtpTransceiver.RtpTransceiverDirection src`
* **Returns:** `String`
* **Description:** Converts the native `RtpTransceiverDirection` enum into its lower-case string counterpart.
* **Mapping:**
  * `INACTIVE` → `"inactive"`
  * `RECV_ONLY` → `"recvonly"`
  * `SEND_ONLY` → `"sendonly"`
  * `SEND_RECV` → `"sendrecv"`
  * `STOPPED` → `"stopped"`
* **Throws:** `Error` if the direction is invalid.

---

#### `serializeTrack(int pcId, MediaStreamTrack track)`
* **Parameters:** 
  * `int pcId`: The PeerConnection ID.
  * `MediaStreamTrack track`: The media stream track instance.
* **Returns:** `ReadableMap`
* **Description:** Serializes a `MediaStreamTrack` object with the following fields:
  * `id` (`String`): Track ID.
  * `peerConnectionId` (`int`): PeerConnection ID.
  * `kind` (`String`): Track kind (e.g., `"audio"` or `"video"`).
  * `enabled` (`boolean`): Whether the track is enabled.
  * `readyState` (`String`): The track state converted to lower-case.
  * `remote` (`boolean`): Hardcoded to `true`.

---

#### `serializeSender(int id, RtpSender sender)`
* **Parameters:** 
  * `int id`: The PeerConnection ID.
  * `RtpSender sender`: The native sender instance.
* **Returns:** `ReadableMap`
* **Description:** Serializes an `RtpSender` object. Contains:
  * `id` (`String`): Sender ID.
  * `peerConnectionId` (`int`): PeerConnection ID.
  * `track` (`ReadableMap`, optional): Serialized track object if the sender has an attached track.
  * `rtpParameters` (`ReadableMap`): Serialized RTP parameters of the sender.
* **Note:** *Transport* and *DTMF* sender fields are currently omitted from this implementation.

---

#### `serializeReceiver(int id, RtpReceiver receiver)`
* **Parameters:** 
  * `int id`: The PeerConnection ID.
  * `RtpReceiver receiver`: The native receiver instance.
* **Returns:** `ReadableMap`
* **Description:** Serializes an `RtpReceiver` object into a map containing:
  * `id` (`String`): Receiver ID.
  * `peerConnectionId` (`int`): PeerConnection ID.
  * `track` (`ReadableMap`, optional): Serialized track object if present.
  * `rtpParameters` (`ReadableMap`): Serialized RTP parameters.

---

#### `serializeTransceiver(int id, RtpTransceiver transceiver)`
* **Parameters:** 
  * `int id`: The PeerConnection ID.
  * `RtpTransceiver transceiver`: The native transceiver object.
* **Returns:** `ReadableMap`
* **Description:** Constructs a map representation of an `RtpTransceiver`:
  * `id` (`String`): ID from the transceiver's sender.
  * `peerConnectionId` (`int`): PeerConnection ID.
  * `mid` (`String`): Media ID assigned to the transceiver.
  * `direction` (`String`): Serialized current direction.
  * `currentDirection` (`String`, optional): Serialized active direction if non-null.
  * `isStopped` (`boolean`): Indicates whether the transceiver is stopped.
  * `receiver` (`ReadableMap`): Serialized receiver.
  * `sender` (`ReadableMap`): Serialized sender.

---

#### `serializeRtpParameters(RtpParameters params)`
* **Parameters:** `RtpParameters params`
* **Returns:** `ReadableMap`
* **Description:** Serializes complete RTP parameters including codecs, encodings, header extensions, and RTCP settings:
  * `transactionId` (`String`)
  * `rtcp` (`ReadableMap`): Contains `cname` and `reducedSize`.
  * `headerExtensions` (`WritableArray`): Array of maps containing `id`, `uri`, and `encrypted`.
  * `encodings` (`WritableArray`): Array of encodings with properties `active`, optional `rid`, `maxBitrate`, `minBitrate`, `maxFramerate`, and `scaleResolutionDownBy`.
  * `codecs` (`WritableArray`): Array of codec maps with `payloadType`, `mimeType`, `clockRate`, optional `channels`, and optional `sdpFmtpLine`.
  * `degradationPreference` (`String`, optional): Serialized preference if present.

---

#### `serializeRtpCapabilities(RtpCapabilities capabilities)`
* **Parameters:** `RtpCapabilities capabilities`
* **Returns:** `ReadableMap`
* **Description:** Serializes WebRTC capabilities into a map containing a `codecs` array constructed via `serializeRtpCapabilitiesCodec`.

---

#### `serializeRtpCapabilitiesCodec(RtpCapabilities.CodecCapability codec)`
* **Parameters:** `RtpCapabilities.CodecCapability codec`
* **Returns:** `ReadableMap`
* **Description:** Converts a `CodecCapability` object into a map containing:
  * `payloadType` (`int`)
  * `mimeType` (`String`)
  * `clockRate` (`int`)
  * `channels` (`int`, optional)
  * `sdpFmtpLine` (`String`, optional)

---

#### `serializeSdpParameters(Map<String, String> parameters)`
* **Parameters:** `Map<String, String> parameters`
* **Returns:** `String`
* **Description:** Utility method that converts a map of key-value SDP parameters into a formatted SDP FMTP line string delimited by semicolons (e.g., `key1=val1;key2=val2`).

---

### 2. Parsing APIs (React Native JS → Native WebRTC)

These methods parse incoming `ReadableMap` objects sent from JavaScript and map them back to native WebRTC Java types.

---

#### `updateRtpParameters(ReadableMap updateParams, RtpParameters rtpParams)`
* **Parameters:** 
  * `ReadableMap updateParams`: Incoming updates from JavaScript.
  * `RtpParameters rtpParams`: The target native `RtpParameters` object to mutate.
* **Returns:** `RtpParameters` (or `null` if validation fails)
* **Description:** Updates `rtpParams` encodings and degradation preferences based on `updateParams`:
  * Validates that the input encodings array count matches the existing native encodings list size. Returns `null` if sizes differ.
  * Updates `active`, `rid`, `maxBitrateBps`, `minBitrateBps`, `maxFramerate`, and `scaleResolutionDownBy` for each encoding layer.
  * Updates `degradationPreference` if supplied in `updateParams`.

---

#### `parseMediaType(String type)`
* **Parameters:** `String type`
* **Returns:** `MediaStreamTrack.MediaType`
* **Description:** Converts media type string values to native enums.
  * `"audio"` → `MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO`
  * `"video"` → `MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO`
* **Throws:** `Error` if the string is unknown.

---

#### `parseDirection(String src)`
* **Parameters:** `String src`
* **Returns:** `RtpTransceiver.RtpTransceiverDirection`
* **Description:** Maps JavaScript string values to `RtpTransceiverDirection` enums.
  * `"sendrecv"` → `SEND_RECV`
  * `"sendonly"` → `SEND_ONLY`
  * `"recvonly"` → `RECV_ONLY`
  * `"inactive"` → `INACTIVE`
* **Note:** Does not accept `"stopped"` (throws an error if passed).
* **Throws:** `Error` if the direction is invalid or unmapped.

---

#### `parseEncoding(ReadableMap params)` *(Private)*
* **Parameters:** `ReadableMap params`
* **Returns:** `RtpParameters.Encoding`
* **Description:** Helper method that initializes an `RtpParameters.Encoding` object using the `rid` string provided in `params` (default active state: `true`, default scale resolution down by: `1.0`), and populates `active`, `maxBitrateBps`, `minBitrateBps`, `maxFramerate`, and `scaleResolutionDownBy` if present in the input map.

---

#### `parseTransceiverOptions(ReadableMap map)`
* **Parameters:** `ReadableMap map`
* **Returns:** `RtpTransceiver.RtpTransceiverInit`
* **Description:** Parses options for transceiver initialization. Returns `null` if the input map is `null`. Extracts and processes:
  * `direction`: Defaults to `SEND_RECV` if omitted.
  * `streamIds`: Extracted into a `List<String>`.
  * `sendEncodings`: Extracted into a `List<RtpParameters.Encoding>` via `parseEncoding`.

---

## Direction Mapping Reference

| String Value | Native Enum (`RtpTransceiverDirection`) | Serialization Support | Parsing Support |
| :--- | :--- | :---: | :---: |
| `"sendrecv"` | `SEND_RECV` | Yes | Yes |
| `"sendonly"` | `SEND_ONLY` | Yes | Yes |
| `"recvonly"` | `RECV_ONLY` | Yes | Yes |
| `"inactive"` | `INACTIVE` | Yes | Yes |
| `"stopped"` | `STOPPED` | Yes | No (Throws Error) |