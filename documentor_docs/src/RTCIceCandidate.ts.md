# Documentation: `src/RTCIceCandidate.ts`

## Overview

The `src/RTCIceCandidate.ts` file provides a TypeScript implementation of an ICE (Interactive Connectivity Establishment) candidate representation used in WebRTC peer-to-peer connections. It defines the structure and behavior of candidate data, including properties for the SDP media description and line index, parameter validation, and serialization functionality.

---

## Type Definitions

### `RTCIceCandidateInfo`

An interface defining the options object passed to the `RTCIceCandidate` constructor.

```typescript
interface RTCIceCandidateInfo {
    candidate?: string;
    sdpMLineIndex?: number | null;
    sdpMid?: string | null;
}
```

#### Properties

| Property | Type | Description |
| :--- | :--- | :--- |
| `candidate` | `string` (optional) | The transport address string representing the ICE candidate. |
| `sdpMLineIndex` | `number \| null` (optional) | The zero-based index of the m-line in the SDP to which the candidate belongs. |
| `sdpMid` | `string \| null` (optional) | The media stream identification tag (`sdpMid`) associated with the candidate. |

---

## Class: `RTCIceCandidate`

The default export of the module representing an ICE candidate instance.

### Class Properties

* **`candidate`** (`string`): The ICE candidate string description.
* **`sdpMLineIndex`** (`number | null | undefined`): The m-line index within the SDP media description.
* **`sdpMid`** (`string | null | undefined`): The media stream ID tag.

---

### Constructor

```typescript
constructor({ candidate = '', sdpMLineIndex = null, sdpMid = null }: RTCIceCandidateInfo)
```

Constructs a new instance of `RTCIceCandidate`.

#### Parameters

Accepts a single configuration object of type [`RTCIceCandidateInfo`](#rtcicecandidateinfo) with the following destructured defaults:

* `candidate` (default: `''`): Candidate string.
* `sdpMLineIndex` (default: `null`): SDP m-line index.
* `sdpMid` (default: `null`): SDP media stream identification tag.

#### Validation & Errors

The constructor performs a mandatory check on `sdpMLineIndex` and `sdpMid`:

* **`TypeError`**: Thrown if **both** `sdpMLineIndex` and `sdpMid` are `null`. At least one of these two identifiers must be non-null.

---

### Methods

#### `toJSON()`

Serializes the `RTCIceCandidate` instance into a plain JavaScript object suitable for JSON conversion.

* **Returns**: `Object`
  * `candidate`: `string`
  * `sdpMLineIndex`: `number | null | undefined`
  * `sdpMid`: `string | null | undefined`

```typescript
toJSON(): {
    candidate: string;
    sdpMLineIndex: number | null | undefined;
    sdpMid: string | null | undefined;
}
```

---

## Usage Examples

### Instantiation with `sdpMid`

```typescript
import RTCIceCandidate from './src/RTCIceCandidate';

const iceCandidate = new RTCIceCandidate({
    candidate: 'candidate:842163049 1 udp 1686052863 192.168.1.2 53713 typ host generation 0 ufrag /32p network-id 1',
    sdpMid: '0'
});

console.log(iceCandidate.toJSON());
// Output:
// {
//   candidate: "candidate:842163049 1 udp 1686052863 192.168.1.2 53713 typ host generation 0 ufrag /32p network-id 1",
//   sdpMLineIndex: null,
//   sdpMid: "0"
// }
```

### Instantiation with `sdpMLineIndex`

```typescript
import RTCIceCandidate from './src/RTCIceCandidate';

const iceCandidate = new RTCIceCandidate({
    candidate: 'candidate:842163049 1 udp 1686052863 192.168.1.2 53713 typ host generation 0 ufrag /32p network-id 1',
    sdpMLineIndex: 0
});
```

### Invalid Instantiation (Throws `TypeError`)

Passing an empty object or omitting both `sdpMLineIndex` and `sdpMid` triggers an error:

```typescript
import RTCIceCandidate from './src/RTCIceCandidate';

// Throws TypeError: `sdpMLineIndex` and `sdpMid` must not be both null
const invalidCandidate = new RTCIceCandidate({});
```