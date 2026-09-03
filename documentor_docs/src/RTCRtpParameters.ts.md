# Technical Documentation: `src/RTCRtpParameters.ts`

## Overview

The `src/RTCRtpParameters.ts` module defines the `RTCRtpParameters` class and its corresponding initialization interface, `RTCRtpParametersInit`. It serves as a data container for RTP (Real-time Transport Protocol) parameters, encapsulating audio/video codecs, header extensions, and RTCP (RTP Control Protocol) configuration settings.

---

## Module Dependencies

- **`./RTCRtcpParameters`**: Imports `RTCRtcpParameters` class and `RTCRtcpParametersInit` interface to handle RTCP settings.
- **`./RTCRtpCodecParameters`**: Imports `RTCRtpCodecParameters` class and `RTCRtpCodecParametersInit` interface to handle codec configurations.
- **`./RTCRtpHeaderExtension`**: Imports `RTCRtpHeaderExtension` class and `RTCRtpHeaderExtensionInit` interface to handle RTP header extensions.
- **`./RTCUtil`**: Imports the `deepClone` utility function to perform deep copying during object serialization.

---

## Interfaces

### `RTCRtpParametersInit`

An interface that defines the structure of the initialization object required to construct an `RTCRtpParameters` instance.

```typescript
export interface RTCRtpParametersInit {
    codecs: RTCRtpCodecParametersInit[],
    headerExtensions: RTCRtpHeaderExtensionInit[],
    rtcp: RTCRtcpParametersInit
}
```

#### Properties

| Property | Type | Description |
| :--- | :--- | :--- |
| `codecs` | `RTCRtpCodecParametersInit[]` | An array of initialization objects for RTP codecs. |
| `headerExtensions` | `RTCRtpHeaderExtensionInit[]` | An array of initialization objects for RTP header extensions. |
| `rtcp` | `RTCRtcpParametersInit` | An initialization object for RTCP parameters. |

---

## Class: `RTCRtpParameters` (Default Export)

The `RTCRtpParameters` class structures and initializes RTP parameters from an `RTCRtpParametersInit` object and provides a serialization method.

### Class Properties

| Property | Type | Modifiers | Initial Value | Description |
| :--- | :--- | :--- | :--- | :--- |
| `codecs` | `(RTCRtpCodecParameters \| RTCRtpCodecParametersInit)[]` | Public | `[]` | Array storing codec objects associated with the RTP transmission. |
| `headerExtensions` | `RTCRtpHeaderExtension[]` | `readonly` | `[]` | Readonly array storing RTP header extension instances. |
| `rtcp` | `RTCRtcpParameters` | `readonly` | Uninitialized | Readonly property holding the `RTCRtcpParameters` instance. |

---

### Constructor

```typescript
constructor(init: RTCRtpParametersInit)
```

#### Behavior
When instantiated with an `init` configuration object:
1. Iterates over each item in `init.codecs`, creates a new `RTCRtpCodecParameters` instance for each, and appends it to `this.codecs`.
2. Iterates over each item in `init.headerExtensions`, creates a new `RTCRtpHeaderExtension` instance for each, and appends it to `this.headerExtensions`.
3. Instantiates `this.rtcp` by passing `init.rtcp` into the `RTCRtcpParameters` constructor.

---

### Methods

#### `toJSON()`

Serializes the `RTCRtpParameters` instance into a plain JavaScript object containing deep clones of its properties.

```typescript
toJSON(): {
    codecs: any[];
    headerExtensions: any[];
    rtcp: any;
}
```

##### Return Value
Returns an object structured as follows:
- `codecs`: An array produced by calling `deepClone(c)` on each element in `this.codecs`.
- `headerExtensions`: An array produced by calling `deepClone(he)` on each element in `this.headerExtensions`.
- `rtcp`: The result of calling `deepClone(this.rtcp)`.

---

## How It Works

1. **Instantiation**: A caller passes an object conforming to `RTCRtpParametersInit` to `new RTCRtpParameters(init)`.
2. **Object Hydration**: The constructor transforms plain initialization data objects into instances of their respective classes (`RTCRtpCodecParameters`, `RTCRtpHeaderExtension`, and `RTCRtcpParameters`).
3. **Serialization**: Calling `.toJSON()` produces a plain JavaScript object copy, leveraging `deepClone` to ensure that references to nested objects (`codecs`, `headerExtensions`, and `rtcp`) are detached from the internal instance state.