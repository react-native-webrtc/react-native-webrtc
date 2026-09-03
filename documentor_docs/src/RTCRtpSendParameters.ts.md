# Documentation: `src/RTCRtpSendParameters.ts`

## Overview

The `src/RTCRtpSendParameters.ts` module defines types and classes used to configure and manage WebRTC RTP sender parameters. It extends the base `RTCRtpParameters` functionality by adding sender-specific configuration options, specifically handling `transactionId`, stream encoding parameters (`encodings`), and video quality degradation preferences (`degradationPreference`).

It also handles format translation between native string representations (e.g., `MAINTAIN_FRAMERATE`) and standard Web API string formats (e.g., `maintain-framerate`).

---

## Types and Interfaces

### `DegradationPreferenceType`
A union type defining the valid web-standard strings for video degradation strategy under low bandwidth or resource constraints:
```typescript
type DegradationPreferenceType = 
    | 'maintain-framerate'
    | 'maintain-resolution'
    | 'balanced'
    | 'disabled';
```

---

### `RTCRtpSendParametersInit`
Extends `RTCRtpParametersInit` to define the initialization object required when constructing an instance of `RTCRtpSendParameters`.

| Property | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `transactionId` | `string` | Yes | Unique identifier for the sender parameters transaction. |
| `encodings` | `RTCRtpEncodingParametersInit[]` | Yes | An array of encoding parameter initializers. |
| `degradationPreference` | `string` | No | Degradation strategy string (typically in native format, e.g., `"MAINTAIN_FRAMERATE"`). |

---

## Helper Class

### `DegradationPreference` (Internal)
A static utility class that converts degradation preference string formats between native formats and standard Web API formats.

#### Static Methods

* **`fromNative(nativeFormat: string): DegradationPreferenceType`**
  * **Purpose**: Converts a native string format (e.g., `MAINTAIN_FRAMERATE`) to web-compliant format (`maintain-framerate`).
  * **Transformation Logic**: Lowercases the input string and replaces underscores (`_`) with hyphens (`-`).
  
* **`toNative(format: DegradationPreferenceType): string`**
  * **Purpose**: Converts a web-compliant format (e.g., `maintain-framerate`) back to the native string format (`MAINTAIN_FRAMERATE`).
  * **Transformation Logic**: Uppercases the input string and replaces hyphens (`-`) with underscores (`_`).

---

## Class: `RTCRtpSendParameters`

Extends `RTCRtpParameters` to represent the active RTP send parameters.

### Properties

| Property | Type | Modifiers | Description |
| :--- | :--- | :--- | :--- |
| `transactionId` | `string` | `readonly` | Unique identifier associated with this set of parameters. |
| `encodings` | `(RTCRtpEncodingParameters \| RTCRtpEncodingParametersInit)[]` | Public | Array of RTP encoding parameter instances. |
| `degradationPreference` | `DegradationPreferenceType \| null` | Public | The current degradation strategy in web format, or `null` if unspecified. |

---

### Methods

#### `constructor(init: RTCRtpSendParametersInit)`
Constructs a new `RTCRtpSendParameters` instance.

1. Calls `super(init)` to initialize base `RTCRtpParameters` properties.
2. Assigns `this.transactionId` from `init.transactionId`.
3. Sets `this.degradationPreference`:
   * If `init.degradationPreference` is provided, converts it via `DegradationPreference.fromNative(init.degradationPreference)`.
   * Otherwise, sets it to `null`.
4. Initializes `this.encodings` as an array where each item in `init.encodings` is instantiated as a new `RTCRtpEncodingParameters` object.

#### `toJSON(): Record<string, any>`
Serializes the instance into a plain JavaScript object suitable for JSON serialization or native bridge passing.

1. Obtains the base JSON object by calling `super.toJSON()`.
2. Appends `transactionId` to the object.
3. Maps `this.encodings` through `deepClone` to populate the `encodings` array with deep copies.
4. If `this.degradationPreference` is not `null`, converts it back to the native format via `DegradationPreference.toNative()` and assigns it to the `degradationPreference` field.
5. Returns the resulting object.

---

## Implementation Example / Data Flow

```
Input (Native Init):
{
  transactionId: "tx-123",
  encodings: [{ active: true }],
  degradationPreference: "MAINTAIN_FRAMERATE"
}
        │
        ▼  Constructor Execution
RTCRtpSendParameters Instance:
  - transactionId: "tx-123"
  - encodings: [ RTCRtpEncodingParameters instance ]
  - degradationPreference: "maintain-framerate"
        │
        ▼  toJSON() Call
Serialized Output (Native Format):
{
  ...superProperties,
  transactionId: "tx-123",
  encodings: [{ active: true }],
  degradationPreference: "MAINTAIN_FRAMERATE"
}
```