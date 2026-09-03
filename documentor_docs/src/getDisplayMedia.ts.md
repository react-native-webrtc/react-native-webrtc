# Technical Documentation: `src/getDisplayMedia.ts`

## Overview

The `src/getDisplayMedia.ts` module provides a function that invokes native WebRTC functionality via React Native's `NativeModules` to capture display media (e.g., screen recording/sharing). It wraps the native promise response into a JavaScript `MediaStream` instance or rejects with a `MediaStreamError`.

---

## Dependencies

* **`react-native`**: Imports `NativeModules` to access the native bridge method `WebRTCModule`.
* **`./MediaStream`**: Imports the `MediaStream` class used to construct the returned stream instance.
* **`./MediaStreamError`**: Imports the `MediaStreamError` class used to wrap error objects returned by native code.

---

## Type Definitions

### `Constraints`

An interface representing configuration options passed to the native module when requesting display media.

```typescript
export interface Constraints {
    android?: {
        createConfigForDefaultDisplay?: boolean;
        resolutionScale?: number;
    }
}
```

#### Fields

* **`android`** *(optional)*: An object containing Android-specific configuration options.
  * **`createConfigForDefaultDisplay`** *(optional, `boolean`)*: Flag to configure settings for the default display on Android.
  * **`resolutionScale`** *(optional, `number`)*: A numeric scaling factor applied to the screen resolution.

---

## Exported Functions

### `default function getDisplayMedia(constraints: Constraints = {}): Promise<MediaStream>`

Requests display media from the native platform using the provided constraints.

#### Parameters

* **`constraints`** (`Constraints`, optional): Configuration options for the display media capture. Defaults to an empty object `{}`.

#### Returns

* **`Promise<MediaStream>`**:
  * **Resolves**: A `MediaStream` object populated with the stream details returned by the native layer.
  * **Rejects**: A `MediaStreamError` containing the native error payload.

---

## Internal Logic Breakdown

1. **Native Module Extraction**:
   Extracts `WebRTCModule` from `NativeModules`:
   ```typescript
   const { WebRTCModule } = NativeModules;
   ```

2. **Native Invocation**:
   Executes `WebRTCModule.getDisplayMedia(constraints)`, which returns a Promise.

3. **Success Handling**:
   When the native module successfully creates the display media stream, it returns an object containing `streamId` and `track`.

   The function constructs an `info` object structured as follows:
   ```typescript
   const info = {
       streamId: streamId,
       streamReactTag: streamId,
       tracks: [ track ]
   };
   ```
   It then constructs a new `MediaStream` instance using `info` and resolves the outer Promise with this stream.

4. **Error Handling**:
   If `WebRTCModule.getDisplayMedia(constraints)` fails or rejects, the error handler catches the native error, wraps it in `new MediaStreamError(error)`, and rejects the outer Promise with it.