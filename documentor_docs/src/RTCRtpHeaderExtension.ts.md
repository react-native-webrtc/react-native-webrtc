# Technical Documentation: `src/RTCRtpHeaderExtension.ts`

## Overview

The `src/RTCRtpHeaderExtension.ts` module provides a data structure and initialization interface for WebRTC RTP header extensions. It exports:
1. `RTCRtpHeaderExtensionInit`: An interface defining the initial configuration parameters for an RTP header extension.
2. `RTCRtpHeaderExtension`: A default exported immutable class representing the RTP header extension instance with serialization support.

---

## Exports Summary

| Export | Type | Description |
| :--- | :--- | :--- |
| `RTCRtpHeaderExtensionInit` | `interface` | Defines the required shape of the initialization object. |
| `RTCRtpHeaderExtension` | `class` (default) | An immutable class storing RTP header extension properties. |

---

## Detailed Component Reference

### 1. `RTCRtpHeaderExtensionInit` (Interface)

An object structure required to instantiate an `RTCRtpHeaderExtension` instance.

#### Properties

* **`id`** (`number`): The local numeric identifier assigned to the RTP header extension.
* **`uri`** (`string`): The Uniform Resource Identifier (URI) specifying the type/specification of the RTP header extension.
* **`encrypted`** (`boolean`): Indicates whether the extension header is encrypted.

---

### 2. `RTCRtpHeaderExtension` (Class)

The primary class that encapsulates RTP header extension data. Instances of this class are frozen upon construction to enforce immutability.

#### Readonly Instance Properties

* **`id`** (`readonly id: number`): The numerical identifier of the header extension.
* **`uri`** (`readonly uri: string`): The URI string identifying the extension format or standard.
* **`encrypted`** (`readonly encrypted: boolean`): A boolean flag reflecting encryption status.

#### Methods

##### `constructor(init: RTCRtpHeaderExtensionInit)`
* **Parameters**: 
  * `init`: An object conforming to `RTCRtpHeaderExtensionInit`.
* **Behavior**:
  1. Assigns `init.id` to `this.id`.
  2. Assigns `init.uri` to `this.uri`.
  3. Assigns `init.encrypted` to `this.encrypted`.
  4. Calls `Object.freeze(this)` to prevent modifications, additions, or deletions of properties on the created instance.

##### `toJSON(): RTCRtpHeaderExtensionInit`
* **Returns**: `RTCRtpHeaderExtensionInit`
* **Behavior**:
  Returns a plain JavaScript object containing the `id`, `uri`, and `encrypted` properties. This facilitates JSON serialization (e.g., via `JSON.stringify()`).

---

## Key Characteristics & Behavior

* **Immutability**: By calling `Object.freeze(this)` inside the constructor, instances of `RTCRtpHeaderExtension` cannot be altered after creation.
* **JSON Serialization**: The implementation of `toJSON()` ensures that when `JSON.stringify()` is invoked on an instance, it safely serializes into a plain dictionary matching `RTCRtpHeaderExtensionInit`.

---

## Code Example

```typescript
import RTCRtpHeaderExtension, { RTCRtpHeaderExtensionInit } from './src/RTCRtpHeaderExtension';

// Configuration object matching RTCRtpHeaderExtensionInit
const initConfig: RTCRtpHeaderExtensionInit = {
    id: 1,
    uri: 'urn:ietf:params:rtp-hdrext:ssrc-audio-level',
    encrypted: false
};

// Create an instance
const headerExtension = new RTCRtpHeaderExtension(initConfig);

// Access properties
console.log(headerExtension.id);        // Output: 1
console.log(headerExtension.uri);       // Output: 'urn:ietf:params:rtp-hdrext:ssrc-audio-level'
console.log(headerExtension.encrypted); // Output: false

// Serialize to plain JSON object
const jsonRepresentation = headerExtension.toJSON();
console.log(jsonRepresentation);
// Output: { id: 1, uri: 'urn:ietf:params:rtp-hdrext:ssrc-audio-level', encrypted: false }
```