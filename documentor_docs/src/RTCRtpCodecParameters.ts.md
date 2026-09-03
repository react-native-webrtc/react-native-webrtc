# Technical Documentation: `src/RTCRtpCodecParameters.ts`

## Overview

The `src/RTCRtpCodecParameters.ts` file provides a representation of WebRTC RTP Codec Parameters. It defines an interface for initialization options (`RTCRtpCodecParametersInit`) and an immutable class (`RTCRtpCodecParameters`) that holds codec configuration data, such as payload type, clock rate, MIME type, channel count, and SDP format parameters (`sdpFmtpLine`).

---

## Key Components

### 1. `RTCRtpCodecParametersInit` (Interface)

An interface defining the plain object structure used to construct an `RTCRtpCodecParameters` instance.

#### Properties

| Property | Type | Optional | Description |
| :--- | :--- | :--- | :--- |
| `payloadType` | `number` | No | The RTP payload type assigned to the codec. |
| `clockRate` | `number` | No | The codec clock rate in Hertz (Hz). |
| `mimeType` | `string` | No | The media MIME type (e.g., `"audio/opus"` or `"video/VP8"`). |
| `channels` | `number` | Yes | The number of audio channels (e.g., `1` for mono, `2` for stereo). |
| `sdpFmtpLine` | `string` | Yes | The format-specific parameters line from the SDP (`a=fmtp:` line). |

---

### 2. `RTCRtpCodecParameters` (Class)

The default export class representing immutable RTP codec parameters.

#### Properties

All properties on an instance of `RTCRtpCodecParameters` are `readonly`:

| Property | Type | Description |
| :--- | :--- | :--- |
| `payloadType` | `number` | The RTP payload type. |
| `clockRate` | `number` | The codec clock rate in Hz. |
| `mimeType` | `string` | The media MIME type string. |
| `channels` | `number \| null` | The number of audio channels, or `null` if not defined/falsy. |
| `sdpFmtpLine` | `string \| null` | The SDP format parameters string, or `null` if not defined/falsy. |

---

#### Constructor

```typescript
constructor(init: RTCRtpCodecParametersInit)
```

##### Behavior:
1. **Property Assignment**: Assigns `payloadType`, `clockRate`, and `mimeType` directly from the `init` argument.
2. **Value Normalization**:
   - `channels`: Evaluates `init.channels`. If truthy, assigns `init.channels`; otherwise, defaults to `null`.
   - `sdpFmtpLine`: Evaluates `init.sdpFmtpLine`. If truthy, assigns `init.sdpFmtpLine`; otherwise, defaults to `null`.
3. **Immutability**: Invokes `Object.freeze(this)` to prevent further modification, addition, or deletion of properties on the instantiated object.

---

#### Methods

##### `toJSON()`

```typescript
toJSON(): RTCRtpCodecParametersInit
```

Serializes the instance back into a plain object adhering to `RTCRtpCodecParametersInit`.

* **Return Value**: An object containing `payloadType`, `clockRate`, and `mimeType`.
* **Conditional Property Inclusion**:
  * `channels` is only added to the output object if `this.channels !== null`.
  * `sdpFmtpLine` is only added to the output object if `this.sdpFmtpLine !== null`.

---

## Detailed Code Execution Flow

### Instance Creation Example

```typescript
import RTCRtpCodecParameters from './src/RTCRtpCodecParameters';

const codec = new RTCRtpCodecParameters({
    payloadType: 111,
    clockRate: 48000,
    mimeType: 'audio/opus',
    channels: 2,
    sdpFmtpLine: 'minptime=10;useinbandfec=1'
});

// Accessing properties
console.log(codec.payloadType); // 111
console.log(codec.channels);    // 2

// Object is frozen
// codec.payloadType = 96; // Throws an error in strict mode
```

### Serialization Example (`toJSON`)

```typescript
// Case 1: All properties present
const fullCodec = new RTCRtpCodecParameters({
    payloadType: 96,
    clockRate: 90000,
    mimeType: 'video/VP8'
});

console.log(fullCodec.toJSON());
// Output:
// {
//   payloadType: 96,
//   clockRate: 90000,
//   mimeType: 'video/VP8'
// }
```