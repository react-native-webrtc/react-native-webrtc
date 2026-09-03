# Technical Documentation: `src/RTCUtil.ts`

## Overview

The `src/RTCUtil.ts` module provides utility functions, constants, and type definitions for handling WebRTC-related operations. Its primary responsibilities include:

- Normalizing media track constraints (audio and video).
- Normalizing RTC offer options into WebRTC internal key-value formats.
- Validating SDP (Session Description Protocol) message types.
- Generating UUID v4-formatted unique identifiers.
- Performing deep clones of JavaScript objects.

---

## Types

### `RTCOfferOptions`

Defines the structure for options passed when creating an RTC offer.

```typescript
export type RTCOfferOptions = {
    iceRestart?: boolean;
    offerToReceiveAudio?: boolean;
    offerToReceiveVideo?: boolean;
    voiceActivityDetection?: boolean;
};
```

#### Fields

| Property | Type | Description |
| :--- | :--- | :--- |
| `iceRestart` | `boolean` (optional) | Indicates whether an ICE restart should be triggered. |
| `offerToReceiveAudio` | `boolean` (optional) | Specifies if the endpoint offers to receive audio tracks. |
| `offerToReceiveVideo` | `boolean` (optional) | Specifies if the endpoint offers to receive video tracks. |
| `voiceActivityDetection` | `boolean` (optional) | Enables or disables voice activity detection. |

---

## Exported Functions

### `uniqueID()`

Generates a pseudo-UUID v4 string in the standard format `{xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}`.

```typescript
export function uniqueID(): string
```

* **Returns**: `string` — A 36-character hyphenated pseudo-UUID string composed of hexadecimal characters generated using `Math.random()`.

---

### `deepClone<T>(obj: T): T`

Creates a deep copy of a provided object using JSON serialization (`JSON.parse(JSON.stringify(obj))`).

```typescript
export function deepClone<T>(obj: T): T
```

* **Parameters**:
  * `obj` (`T`): The object to clone.
* **Returns**: `T` — A new deep-cloned instance of the input object.

---

### `isSdpTypeValid(type: string): boolean`

Validates whether a given string is a recognized SDP type.

```typescript
export function isSdpTypeValid(type: string): boolean
```

* **Parameters**:
  * `type` (`string`): The SDP type string to check.
* **Returns**: `boolean` — `true` if `type` matches one of `'offer'`, `'pranswer'`, `'answer'`, or `'rollback'`; otherwise `false`.

---

### `normalizeOfferOptions(options?: RTCOfferOptions)`

Normalizes user-supplied `RTCOfferOptions` into internal WebRTC string key-value mappings.

```typescript
export function normalizeOfferOptions(options?: RTCOfferOptions): Record<string, string>
```

* **Parameters**:
  * `options` (`RTCOfferOptions`, optional): User-defined offer options.
* **Returns**: `Record<string, string>` — A dictionary mapping capitalized WebRTC constraint keys (e.g., `'IceRestart'`, `'OfferToReceiveAudio'`) to stringified boolean values (`"true"` or `"false"`). Returns an empty object if `options` is missing or not an object.

#### Mappings

| Input Property (Case-insensitive) | Target Internal Key |
| :--- | :--- |
| `iceRestart` | `IceRestart` |
| `offerToReceiveAudio` | `OfferToReceiveAudio` |
| `offerToReceiveVideo` | `OfferToReceiveVideo` |
| `voiceActivityDetection` | `VoiceActivityDetection` |

---

### `normalizeConstraints(constraints)`

Normalizes an audio/video constraints object. It deep-clones the input object and formats both `audio` and `video` settings.

```typescript
export function normalizeConstraints(constraints: any): any
```

* **Parameters**:
  * `constraints`: The raw media constraints object containing `audio` and/or `video` keys.
* **Returns**: A normalized clone of the constraints object.
* **Exceptions**: Throws a `TypeError` if `audio` or `video` properties are neither booleans nor objects (when defined).

---

## Internal Helper Functions & Constants

The file contains internal constants and helper functions used during constraint resolution and ID generation.

### Constants

* **`DEFAULT_AUDIO_CONSTRAINTS`**: `{}`
* **`DEFAULT_VIDEO_CONSTRAINTS`**:
  ```javascript
  {
      facingMode: 'user',
      frameRate: 30,
      height: 720,
      width: 1280
  }
  ```
* **`FACING_MODES`**: `['user', 'environment']`
* **`ASPECT_RATIO`**: `16 / 9` (~1.777)
* **`SDP_TYPES`**: `['offer', 'pranswer', 'answer', 'rollback']`

---

### Internal Functions

#### `getDefaultMediaConstraints(mediaType)`
Returns the default constraints corresponding to `mediaType` (`'audio'` or `'video'`). Throws `TypeError` for unsupported media types.

#### `extractString(constraints, prop)`
Extracts string property values from constraints. If the property value is an object, it inspects `exact` and `ideal` keys in order. If it is a string, it returns the string directly.

#### `extractNumber(constraints, prop)`
Extracts and parses integer values from constraints. Handles direct numbers or constraint objects with `exact`, `ideal`, `max`, or `min` keys.

#### `normalizeMediaConstraints(constraints, mediaType)`
Processes individual media type constraints:
* **Audio**: Returns the `constraints` object as-is.
* **Video**:
  1. Extracts `deviceId`, `facingMode`, `frameRate`, `height`, and `width`.
  2. Removes `deviceId` if empty.
  3. Validates `facingMode` against `FACING_MODES` (`'user'`, `'environment'`), falling back to `'user'` if invalid.
  4. Defaults `frameRate` to `30` if missing.
  5. Calculates or assigns dimensions:
     * If both `height` and `width` are missing: defaults to `720` height and `1280` width.
     * If `width` is given but `height` is missing: calculates `height = Math.round(width / (16/9))`.
     * If `height` is given but `width` is missing: calculates `width = Math.round(height * (16/9))`.

#### `chr4()`
Helper utility that generates a 4-character random hexadecimal string slice from `Math.random().toString(16).slice(-4)`.