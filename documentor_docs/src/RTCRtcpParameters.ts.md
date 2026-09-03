# Technical Documentation: `src/RTCRtcpParameters.ts`

## Overview

The `src/RTCRtcpParameters.ts` module defines the data structure and class for managing RTCP (Real-time Transport Control Protocol) parameters. It exports an initialization interface (`RTCRtcpParametersInit`) and an immutable class (`RTCRtcpParameters`) that encapsulates RTCP configuration properties and supports JSON serialization.

---

## Exports Summary

| Name | Type | Description |
| :--- | :--- | :--- |
| `RTCRtcpParametersInit` | Interface | Defines the plain object shape required to initialize an `RTCRtcpParameters` instance. |
| `RTCRtcpParameters` | Default Export Class | Represents immutable RTCP parameters with properties for `cname` and `reducedSize`. |

---

## Interfaces

### `RTCRtcpParametersInit`

An interface used as the initialization object for creating instances of `RTCRtcpParameters`.

#### Properties

* **`cname`** (`string`)
  * The Canonical Name (CNAME) used in RTCP to identify the media source.
* **`reducedSize`** (`boolean`)
  * Indicates whether reduced-size RTCP packets are configured/enabled.

```typescript
export interface RTCRtcpParametersInit {
    cname: string;
    reducedSize: boolean;
}
```

---

## Classes

### `RTCRtcpParameters` (Default Export)

Class representing RTCP parameters. Once instantiated, instances are frozen to guarantee immutability.

#### Readonly Properties

* **`cname`** (`string`): The canonical name associated with the RTCP parameters.
* **`reducedSize`** (`boolean`): Indicates if reduced-size RTCP is enabled.

#### Constructor

```typescript
constructor(init: RTCRtcpParametersInit)
```

* **Parameters:**
  * `init` (`RTCRtcpParametersInit`): An object containing initialization values for `cname` and `reducedSize`.
* **Behavior:**
  * Assigns `this.cname` from `init.cname`.
  * Assigns `this.reducedSize` from `init.reducedSize`.
  * Executes `Object.freeze(this)` on the instance to prevent any subsequent modification, addition, or deletion of properties.

#### Methods

##### `toJSON()`

```typescript
toJSON(): RTCRtcpParametersInit
```

* **Description:** Serializes the `RTCRtcpParameters` instance into a plain JavaScript object matching the `RTCRtcpParametersInit` interface structure.
* **Returns:** `RTCRtcpParametersInit`
  * An object containing:
    * `cname`: The current instance's `cname` property.
    * `reducedSize`: The current instance's `reducedSize` property.

---

## How It Works

1. **Instantiation:**
   To create an `RTCRtcpParameters` instance, pass an object conforming to `RTCRtcpParametersInit` to the constructor:
   ```typescript
   import RTCRtcpParameters from './RTCRtcpParameters';

   const rtcpParams = new RTCRtcpParameters({
       cname: "user@example.com",
       reducedSize: true
   });
   ```

2. **Immutability:**
   Because `Object.freeze(this)` is invoked inside the constructor, attempting to reassign or modify properties on the instantiated object will fail (and throw a `TypeError` in strict mode):
   ```typescript
   // Attempting to modify properties will be prevented by Object.freeze
   rtcpParams.cname = "new-cname"; // Fails / Errors in strict mode
   ```

3. **Serialization:**
   Calling `toJSON()` retrieves a plain object representation of the instance, which is useful when passing parameters across process boundaries or serializing to JSON:
   ```typescript
   const jsonOutput = rtcpParams.toJSON();
   // Output: { cname: "user@example.com", reducedSize: true }
   ```