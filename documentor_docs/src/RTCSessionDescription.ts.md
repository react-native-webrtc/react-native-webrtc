# Technical Documentation: `src/RTCSessionDescription.ts`

## Overview

The `src/RTCSessionDescription.ts` file defines an interface (`RTCSessionDescriptionInit`) and a default class (`RTCSessionDescription`) that encapsulate the configuration and state of a WebRTC session description. 

It provides a structured way to store, access, and serialize session description data consisting of an SDP (Session Description Protocol) string and a SDP type string.

---

## Key Components

### 1. `RTCSessionDescriptionInit` Interface

An interface that defines the structure for initializing or serializing an `RTCSessionDescription` object.

#### Properties

| Property | Type | Description |
| :--- | :--- | :--- |
| `sdp` | `string` | The Session Description Protocol (SDP) text string. |
| `type` | `string \| null` | The type of the session description (e.g., `'offer'`, `'answer'`, etc.) or `null`. |

---

### 2. `RTCSessionDescription` Class

The default exported class representing a session description instance.

#### Internal Properties

* `_sdp: string`: Internal backing store for the SDP string.
* `_type: string | null`: Internal backing store for the session description type.

#### Constructor

```typescript
constructor(info: RTCSessionDescriptionInit = { type: null, sdp: '' })
```

* **Parameters**:
  * `info` (optional): An object conforming to `RTCSessionDescriptionInit`. Defaults to `{ type: null, sdp: '' }`.
* **Behavior**: Assigns `info.sdp` to `_sdp` and `info.type` to `_type`.

#### Getters

* **`get sdp(): string`**
  * Returns the internal `_sdp` string value.
* **`get type(): string | null`**
  * Returns the internal `_type` string value or `null`.

#### Methods

* **`toJSON(): RTCSessionDescriptionInit`**
  * Serializes the instance into a plain JavaScript object matching the `RTCSessionDescriptionInit` interface.
  * **Returns**: An object containing `{ sdp: this._sdp, type: this._type }`.

---

## How It Works

1. **Instantiation**: You create a new instance of `RTCSessionDescription` by passing an optional initialization object. If no argument is provided, it defaults to an empty SDP string (`''`) and `null` for the type.
2. **Accessing Properties**: Read-only access to the internal values `_sdp` and `_type` is provided via the `sdp` and `type` getter properties.
3. **Serialization**: Calling `toJSON()` extracts the underlying `sdp` and `type` properties into a standard object representation conforming to `RTCSessionDescriptionInit`.

---

## Code Example

```typescript
import RTCSessionDescription from './src/RTCSessionDescription';

// Instantiation with default values
const defaultDesc = new RTCSessionDescription();
console.log(defaultDesc.sdp);  // Output: ''
console.log(defaultDesc.type); // Output: null

// Instantiation with parameters
const offerDesc = new RTCSessionDescription({
    type: 'offer',
    sdp: 'v=0\r\no=- 42 2 IN IP4 127.0.0.1...'
});

console.log(offerDesc.sdp);  // Output: 'v=0\r\no=- 42 2 IN IP4 127.0.0.1...'
console.log(offerDesc.type); // Output: 'offer'

// Serializing to JSON object
const jsonOutput = offerDesc.toJSON();
// jsonOutput is { sdp: 'v=0\r\no=- 42 2 IN IP4 127.0.0.1...', type: 'offer' }
```