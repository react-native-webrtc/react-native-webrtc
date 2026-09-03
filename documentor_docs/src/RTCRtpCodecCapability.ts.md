# Documentation: `src/RTCRtpCodecCapability.ts`

## Overview

The `RTCRtpCodecCapability` class is a light, immutable data wrapper designed to encapsulate information about a media codec capability—specifically its MIME type string.

---

## Class Definition

```typescript
export default class RTCRtpCodecCapability
```

### Purpose

The primary purpose of this class is to hold a read-only `mimeType` property. Upon instantiation, the object freezes itself to prevent any further modifications, ensuring data immutability.

---

## Internal Properties

### `_mimeType`
* **Type:** `string`
* **Visibility:** Internal (instance property)
* **Description:** Stores the underlying MIME type string assigned during class initialization.

---

## Constructor

```typescript
constructor(init: { mimeType: string })
```

### Parameters
* `init`: An object containing configuration properties.
  * `init.mimeType` (`string`): The MIME type string to be assigned to the instance.

### Behavior
1. Assigns `init.mimeType` to the private property `this._mimeType`.
2. Calls `Object.freeze(this)` on the instance. This prevents adding, removing, or modifying properties on the created object.

---

## Getters

### `mimeType`

```typescript
get mimeType(): string
```

* **Returns:** `string` — The MIME type associated with this codec capability object (reads `this._mimeType`).

---

## How It Works

1. **Instantiation**: You instantiate the class by passing an object with a `mimeType` string property to the constructor:
   ```typescript
   const codecCapability = new RTCRtpCodecCapability({ mimeType: 'audio/opus' });
   ```

2. **Accessing Properties**: You can read the MIME type via the `mimeType` getter:
   ```typescript
   console.log(codecCapability.mimeType); // Output: "audio/opus"
   ```

3. **Immutability**: Because `Object.freeze(this)` is called inside the constructor, attempting to alter existing properties or add new ones to an instance will fail (or throw a `TypeError` in strict mode):
   ```typescript
   // Fails due to Object.freeze
   codecCapability.mimeType = 'video/VP8'; 
   ```