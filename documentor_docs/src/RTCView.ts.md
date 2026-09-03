# Technical Documentation: `src/RTCView.ts`

## Overview

The `src/RTCView.ts` module binds a native WebRTC video rendering view component (`RTCVideoView`) to React Native using `requireNativeComponent`. It provides TypeScript interfaces (`RTCVideoViewProps` and `RTCIOSPIPOptions`) to ensure type safety and document the available properties for rendering video streams, controlling layout fitting, setting z-index order, enabling iOS Picture-in-Picture (PiP), and handling video dimension change callbacks.

---

## Component Export

### Default Export

```typescript
export default requireNativeComponent<RTCVideoViewProps>('RTCVideoView');
```

Registers and exports the native view component named `RTCVideoView` from the underlying platform code (iOS/Android), typed with `RTCVideoViewProps`.

---

## Type Definitions & Interfaces

### 1. `RTCVideoViewProps`

Extends `ViewProps` from `react-native`. Describes all configurable properties supported by the `RTCVideoView` component.

| Property | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `mirror` | `boolean` | `undefined` | Indicates whether the video specified by `streamURL` should be mirrored horizontally during rendering (commonly used for front/user-facing cameras). |
| `objectFit` | `'contain' \| 'cover'` | `'cover'` | Resembles the CSS `object-fit` property. Controls how the video content scales within the view bounds. |
| `streamURL` | `string` | `undefined` | The URL or identifier of the WebRTC media stream to be rendered by the view. |
| `zOrder` | `number` | `undefined` | Specifies the relative z-index / rendering stack order of the view among overlapping `RTCView` instances. |
| `iosPIP` | `RTCIOSPIPOptions` | `undefined` | Configuration options for iOS Picture-in-Picture (PiP). Disables PiP if not supplied. |
| `onDimensionsChange` | `(event: { nativeEvent: { width: number; height: number } }) => void` | `undefined` | Callback function triggered when the resolution or dimensions of the video stream change. |

#### Event Payload for `onDimensionsChange`

When `onDimensionsChange` is called, it provides an event object with the following structure:

```typescript
{
  nativeEvent: {
    width: number;  // The updated width of the video frame
    height: number; // The updated height of the video frame
  }
}
```

---

### 2. `RTCIOSPIPOptions`

Configuration object for iOS Picture-in-Picture (PiP) functionality passed via the `iosPIP` prop on `RTCVideoViewProps`.

> **Note on iOS PiP Requirements (from code comments):**
> - iOS only.
> - Requires iOS 15.0 or above.
> - Requires the PiP background mode capability in the app.
> - Generally recommended for remote video tracks, as local camera capture may pause while the app is in the background.

| Property | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `enabled` | `boolean` | `true` | Controls whether PiP mode can be launched from this view. |
| `preferredSize` | `{ width: number; height: number }` | `undefined` | Sets the target/preferred dimensions for the PiP window. |
| `startAutomatically` | `boolean` | `true` | Indicates whether PiP starts automatically when content is embedded inline and the app enters the background. Maps to `AVPictureInPictureController.canStartPictureInPictureAutomaticallyFromInline`. |
| `stopAutomatically` | `boolean` | `true` | Indicates whether PiP should automatically close when the application returns to the foreground. |

---

## Detailed Property Explanations

### `zOrder` Behavior

The `zOrder` prop acts as a hint to the native rendering layer regarding view stacking order:

- **Platform-Specific Implementation**: Native video renderers (e.g., OpenGL/SurfaceView/TextureView context) manage layering differently depending on the operating system.
- **Android Layering Context**: Android typically provides limited stacking layers for hardware-accelerated video views (e.g., default layer below the window, media overlay layer, and a layer above the window).
- **Recommended Usage**: Keep the number of `zOrder` values minimal (typically `0` for full-screen remote videos in the background and `1` for small local picture-in-picture video overlays).