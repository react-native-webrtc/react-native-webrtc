# Technical Documentation: `src/RTCRtpEncodingParameters.ts`

## Overview

The `src/RTCRtpEncodingParameters.ts` module provides a TypeScript interface (`RTCRtpEncodingParametersInit`) and class (`RTCRtpEncodingParameters`) for managing WebRTC RTP stream encoding parameters. It handles object initialization, parameter validation via property getters/setters, and serialization back to a plain JavaScript object via `toJSON()`.

---

## Interfaces

### `RTCRtpEncodingParametersInit`

An interface defining the plain object structure used to initialize an instance of `RTCRtpEncodingParameters` or represented during JSON serialization.

#### Properties

| Property | Type | Optional | Description |
| :--- | :--- | :--- | :--- |
| `active` | `boolean` | **No** | Indicates whether the encoding layer is active. |
| `rid` | `string` | Yes | The Restriction Identifier (RID) associated with the encoding stream. |
| `maxFramerate` | `number` | Yes | The maximum frame rate (in frames per second) for the encoding. |
| `maxBitrate` | `number` | Yes | The maximum bitrate (in bits per second) for the encoding. |
| `minBitrate` | `number` | Yes | The minimum bitrate (in bits per second) for the encoding. |
| `scaleResolutionDownBy` | `number` | Yes | The factor by which to scale down the video resolution. |

---

## Classes

### `RTCRtpEncodingParameters` (Default Export)

The primary class representing the RTP encoding configuration, featuring input validation setters and serialization support.

#### Internal State Properties

* `active: boolean` – Directly accessible boolean property indicating active status.
* `_rid: string | null` – Stores the stream identifier (read-only after construction).
* `_maxFramerate: number | null` – Backing field for `maxFramerate`.
* `_maxBitrate: number | null` – Backing field for `maxBitrate`.
* `_minBitrate: number | null` – Backing field for `minBitrate`.
* `_scaleResolutionDownBy: number | null` – Backing field for `scaleResolutionDownBy`.

---

### Constructor

```typescript
constructor(init: RTCRtpEncodingParametersInit)
```

Constructs a new `RTCRtpEncodingParameters` instance using an `RTCRtpEncodingParametersInit` object. 

* `active` is assigned directly from `init.active`.
* Internal fields (`_rid`, `_maxBitrate`, `_minBitrate`, `_maxFramerate`, `_scaleResolutionDownBy`) are assigned from the corresponding `init` properties if present. If an optional property is omitted or `undefined`, the internal property defaults to `null`.

---

### Getters and Setters

The class uses getters and setters to encapsulate and validate mutations to its internal properties.

#### `rid`
* **Getter**: Returns `_rid` (`string | null`).
* **Setter**: None provided (read-only after construction).

#### `maxFramerate`
* **Getter**: Returns `_maxFramerate` (`number | null`).
* **Setter**: Sets `_maxFramerate`.
  * **Validation Rule**: If `framerate` is not `null`/`undefined` AND `framerate > 0`, `_maxFramerate` is updated to `framerate`.
  * **Fallback**: Otherwise, `_maxFramerate` is set to `null`.

#### `maxBitrate`
* **Getter**: Returns `_maxBitrate` (`number | null`).
* **Setter**: Sets `_maxBitrate`.
  * **Validation Rule**: If `bitrate` is not `null`/`undefined` AND `bitrate >= 0`, `_maxBitrate` is updated to `bitrate`.
  * **Fallback**: Otherwise, `_maxBitrate` is set to `null`.

#### `minBitrate`
* **Getter**: Returns `_minBitrate` (`number | null`).
* **Setter**: Sets `_minBitrate`.
  * **Validation Rule**: If `bitrate` is not `null`/`undefined` AND `bitrate >= 0`, `_minBitrate` is updated to `bitrate`.
  * **Fallback**: Otherwise, `_minBitrate` is set to `null`.

#### `scaleResolutionDownBy`
* **Getter**: Returns `_scaleResolutionDownBy` (`number | null`).
* **Setter**: Sets `_scaleResolutionDownBy`.
  * **Validation Rule**: If `resolutionScale` is not `null`/`undefined` AND `resolutionScale >= 1`, `_scaleResolutionDownBy` is updated to `resolutionScale`.
  * **Fallback**: Otherwise, `_scaleResolutionDownBy` is set to `null`.

---

### Methods

#### `toJSON()`

```typescript
toJSON(): RTCRtpEncodingParametersInit
```

Serializes the instance into a plain `RTCRtpEncodingParametersInit` object.

* **Behavior**:
  1. Instantiates a return object containing `active` explicitly converted to a boolean (`Boolean(this.active)`).
  2. Conditionally attaches optional properties (`rid`, `maxBitrate`, `minBitrate`, `maxFramerate`, `scaleResolutionDownBy`) **only if** their underlying internal property values are not strictly equal to `null`.

---

## Setter Validation Constraints Summary

| Property | Valid Condition | Invalid Result |
| :--- | :--- | :--- |
| `maxFramerate` | Value is not null/undefined AND `> 0` | Set to `null` |
| `maxBitrate` | Value is not null/undefined AND `>= 0` | Set to `null` |
| `minBitrate` | Value is not null/undefined AND `>= 0` | Set to `null` |
| `scaleResolutionDownBy` | Value is not null/undefined AND `>= 1` | Set to `null` |