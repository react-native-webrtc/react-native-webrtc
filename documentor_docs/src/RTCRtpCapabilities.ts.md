# Technical Documentation: `src/RTCRtpCapabilities.ts`

## File Overview

The `src/RTCRtpCapabilities.ts` file defines and exports the `RTCRtpCapabilities` class. The primary purpose of this class is to represent the codec capabilities for WebRTC senders and receivers.

## Dependencies

- **`RTCRtpCodecCapability`**: Imported from `./RTCRtpCodecCapability`. This type/class represents the individual codec capability objects stored within an `RTCRtpCapabilities` instance.

---

## Class Definition: `RTCRtpCapabilities`

`RTCRtpCapabilities` is exported as the default export of the module.

### Properties

| Property Name | Type | Initial Value | Description |
| :--- | :--- | :--- | :--- |
| `_codecs` | `RTCRtpCodecCapability[]` | `[]` | Internal array storing the list of codec capabilities. |

---

### Constructor

```typescript
constructor(codecs: RTCRtpCodecCapability[])
```

#### Parameters
- **`codecs`**: `RTCRtpCodecCapability[]`  
  An array of `RTCRtpCodecCapability` objects representing supported codecs.

#### Behavior
1. Assigns the provided `codecs` array parameter to the instance property `_codecs`.
2. Calls `Object.freeze(this)` to freeze the newly created instance, making it immutable. Once constructed, properties on the instance cannot be added, deleted, or modified.

---

### Getters

#### `codecs`

```typescript
get codecs(): RTCRtpCodecCapability[]
```

- **Returns**: `RTCRtpCodecCapability[]`
- **Description**: Read-only accessor property that returns the internal list of `RTCRtpCodecCapability` objects stored in `_codecs`.

---

## Immutability & Design Details

- **Immutability**: By executing `Object.freeze(this)` inside the constructor, instances of `RTCRtpCapabilities` are shallowly frozen upon instantiation.
- **Read-Only Access**: The underlying `_codecs` property is exposed publicly via the read-only getter `codecs`. No setter is provided.