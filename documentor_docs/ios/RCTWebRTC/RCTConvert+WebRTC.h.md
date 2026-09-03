# Technical Documentation: `ios/RCTWebRTC/RCTConvert+WebRTC.h`

## Overview

The `ios/RCTWebRTC/RCTConvert+WebRTC.h` header file defines a category extension on React Native's `RCTConvert` class (`RCTConvert (WebRTC)`). 

Its primary purpose is to declare custom conversion helper methods that parse standard React Native JavaScript/JSON objects (`id json`) into native WebRTC Objective-C data structures provided by the native `WebRTC` framework.

---

## Header Imports

The header includes dependencies from both the React Native framework and the native WebRTC framework:

* **React Native:**
  * `<React/RCTConvert.h>`: The core React Native utility class used to convert bridge JSON arguments into native Cocoa types.
* **WebRTC Framework:**
  * `<WebRTC/RTCConfiguration.h>`: Represents peer connection configuration parameters.
  * `<WebRTC/RTCDataChannelConfiguration.h>`: Represents configuration options for RTC Data Channels.
  * `<WebRTC/RTCIceCandidate.h>`: Represents an Interactive Connectivity Establishment (ICE) candidate.
  * `<WebRTC/RTCIceServer.h>`: Represents an ICE server (STUN/TURN server details).
  * `<WebRTC/RTCSessionDescription.h>`: Represents a Session Description Protocol (SDP) offer or answer.

---

## Category Interface Declaration

```objc
@interface RCTConvert (WebRTC)
```

The interface extends `RCTConvert` to support native WebRTC types.

### Declared Methods

Each class method accepts an unparsed JSON/JavaScript object (`id json`) passed over the React Native bridge and returns an instance of a corresponding native WebRTC class:

#### 1. `+ (RTCIceCandidate *)RTCIceCandidate:(id)json;`
* **Purpose:** Declares a converter to transform a JSON object into an `RTCIceCandidate` object.
* **Input:** `id json` (JSON representation of an ICE candidate).
* **Return Type:** `RTCIceCandidate *`

#### 2. `+ (RTCSessionDescription *)RTCSessionDescription:(id)json;`
* **Purpose:** Declares a converter to transform a JSON object into an `RTCSessionDescription` object (SDP offer/answer).
* **Input:** `id json` (JSON representation of a session description).
* **Return Type:** `RTCSessionDescription *`

#### 3. `+ (RTCIceServer *)RTCIceServer:(id)json;`
* **Purpose:** Declares a converter to transform a JSON object into an `RTCIceServer` object.
* **Input:** `id json` (JSON representation of an ICE server configuration).
* **Return Type:** `RTCIceServer *`

#### 4. `+ (RTCDataChannelConfiguration *)RTCDataChannelConfiguration:(id)json;`
* **Purpose:** Declares a converter to transform a JSON object into an `RTCDataChannelConfiguration` object.
* **Input:** `id json` (JSON representation of data channel settings).
* **Return Type:** `RTCDataChannelConfiguration *`

#### 5. `+ (RTCConfiguration *)RTCConfiguration:(id)json;`
* **Purpose:** Declares a converter to transform a JSON object into an `RTCConfiguration` object.
* **Input:** `id json` (JSON representation of a peer connection configuration).
* **Return Type:** `RTCConfiguration *`

---

## How It Works

1. **Bridge Integration:** React Native uses `RCTConvert` categories to automatically or explicitly map arguments in native module methods defined with `RCT_EXPORT_METHOD`.
2. **Type Safety:** By declaring these conversion selectors on `RCTConvert`, other native Objective-C/Swift classes within the module can invoke `[RCTConvert RTCIceCandidate:json]` (and similar methods) to safely construct native WebRTC objects from raw data passed across the JavaScript-to-Native bridge.