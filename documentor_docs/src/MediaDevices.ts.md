# Technical Documentation: `src/MediaDevices.ts`

## Overview

The `src/MediaDevices.ts` file provides a W3C-compatible implementation of the `MediaDevices` interface for React Native. It extends an event target implementation (`EventTarget`) to manage device events and provides methods for listing available media devices (`enumerateDevices`), capturing screen/display media (`getDisplayMedia`), and capturing audio/video media (`getUserMedia`).

The module exports a **singleton instance** of the `MediaDevices` class as its default export.

---

## Dependencies & Imports

* **`NativeModules`** (from `'react-native'`): Used to access `WebRTCModule`, the native layer bridging JavaScript with native WebRTC functionality.
* **`getDisplayMedia`, `DisplayMediaConstraints`** (from `'./getDisplayMedia'`): Internal implementation and type definition for screen capture.
* **`getUserMedia`, `UserMediaConstraints`** (from `'./getUserMedia'`): Internal implementation and type definition for standard user media capture.
* **`Event`, `EventTarget`, `getEventAttributeValue`, `setEventAttributeValue`** (from `'./vendor/event-target-shim'`): Event handling utility classes and functions providing browser-like event target capabilities.

---

## Type Definitions

### `MediaDevicesEventMap`

```typescript
type MediaDevicesEventMap = {
    devicechange: Event<'devicechange'>
}
```

Defines the mapping of supported event types for the `MediaDevices` event target. Currently maps the `'devicechange'` event to an `Event<'devicechange'>` object.

---

## Class: `MediaDevices`

`MediaDevices` extends `EventTarget<MediaDevicesEventMap>`, allowing event listeners to be attached/detached for media device events.

### Properties

#### `ondevicechange`
* **Type**: Event listener function or `null`/`undefined`.
* **Description**: Getter and setter property for managing the `'devicechange'` event handler using `getEventAttributeValue` and `setEventAttributeValue`.

---

### Methods

#### `enumerateDevices()`
* **Signature**: `enumerateDevices(): Promise<any>`
* **Description**: Requests a list of available media input and output devices.
* **Implementation Details**: Wraps the asynchronous call to `WebRTCModule.enumerateDevices(resolve)` inside a native JavaScript `Promise`.

#### `getDisplayMedia(constraints)`
* **Signature**: `getDisplayMedia(constraints: DisplayMediaConstraints): Promise<any>`
* **Parameters**: 
  * `constraints` (`DisplayMediaConstraints`): Configuration options for screen capture.
* **Description**: Initiates screen or display capture adhering to the W3C "Screen Capture" specification.
* **Implementation Details**: Delegates execution directly to the imported `getDisplayMedia` function.

#### `getUserMedia(constraints)`
* **Signature**: `getUserMedia(constraints: UserMediaConstraints): Promise<any>`
* **Parameters**: 
  * `constraints` (`UserMediaConstraints`): Configuration options for camera and microphone media capture.
* **Description**: Requests access to local media streams (e.g., camera and microphone) adhering to the W3C "Media Capture and Streams" specification.
* **Implementation Details**: Delegates execution directly to the imported `getUserMedia` function.

---

## Export

```typescript
export default new MediaDevices();
```

The file exports a single, instantiated object of the `MediaDevices` class. This mimics standard browser behavior where `navigator.mediaDevices` is a pre-instantiated singleton object.