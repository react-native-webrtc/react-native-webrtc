# Technical Documentation: `src/RTCPIPView.tsx`

## Overview

The `src/RTCPIPView.tsx` module provides a React wrapper component (`RTCPIPView`) around the underlying `RTCView` component. Its primary purpose is to simplify handling an iOS Picture-in-Picture (PiP) fallback view by accepting it as part of the `iosPIP` prop object and rendering it as a child of `RTCView`.

Additionally, the file exports two utility functions (`startIOSPIP` and `stopIOSPIP`) to dispatch native imperative commands to the `RTCVideoView` view manager via React Native's `UIManager`.

---

## Type Definitions

### `RTCPIPViewProps`

Extends `RTCVideoViewProps` (imported from `./RTCView`).

```typescript
export interface RTCPIPViewProps extends RTCVideoViewProps {
  iosPIP?: RTCIOSPIPOptions & {
    fallbackView?: Component;
  };
}
```

* **`iosPIP`** *(optional)*: An object combining `RTCIOSPIPOptions` with an optional `fallbackView` property.
  * **`fallbackView`** *(optional)*: A React `Component` rendered inside the `RTCView` component when passed.

---

### `RTCViewInstance`

```typescript
type RTCViewInstance = InstanceType<typeof RTCView>;
```

A type alias representing an instance of the `RTCView` component, used to type the forwarded ref.

---

## Components

### `RTCPIPView`

A React component created using `forwardRef`. It wraps `RTCView` and sanitizes props before passing them down.

#### Props and Ref
* **`props`**: `RTCPIPViewProps`
* **`ref`**: Forwarded React reference pointing to an `RTCViewInstance`.

#### Execution Flow & Logic
1. Creates a shallow copy of the incoming `props` (`rtcViewProps`).
2. Extracts the `fallbackView` from `rtcViewProps.iosPIP?.fallbackView`.
3. Deletes `fallbackView` from the `rtcViewProps.iosPIP` object to avoid passing an unexpected property down to the underlying native/RTCView props.
4. Renders and returns an `RTCView` element:
   * Passes the forwarded `ref`.
   * Spreads the remaining `rtcViewProps`.
   * Passes `fallbackView` as child content to `RTCView`.

---

## Utility Functions

### `startIOSPIP(ref)`

Triggers the native command to initiate iOS Picture-in-Picture mode for the given view node.

#### Parameters
* **`ref`**: A React reference object pointing to the target component instance.

#### Implementation Details
Calls `UIManager.dispatchViewManagerCommand` using:
1. `ReactNative.findNodeHandle(ref.current)` to obtain the native node handle.
2. `UIManager.getViewManagerConfig('RTCVideoView').Commands.startIOSPIP` as the command identifier.
3. An empty array `[]` for command arguments.

---

### `stopIOSPIP(ref)`

Triggers the native command to stop iOS Picture-in-Picture mode for the given view node.

#### Parameters
* **`ref`**: A React reference object pointing to the target component instance.

#### Implementation Details
Calls `UIManager.dispatchViewManagerCommand` using:
1. `ReactNative.findNodeHandle(ref.current)` to obtain the native node handle.
2. `UIManager.getViewManagerConfig('RTCVideoView').Commands.stopIOSPIP` as the command identifier.
3. An empty array `[]` for command arguments.

---

## Summary of Exports

| Export Name | Type | Description |
| :--- | :--- | :--- |
| `default` (`RTCPIPView`) | React Component (`forwardRef`) | Wrapper around `RTCView` handling `fallbackView` extraction. |
| `startIOSPIP` | Function | Dispatches the native `startIOSPIP` command to `RTCVideoView`. |
| `stopIOSPIP` | Function | Dispatches the native `stopIOSPIP` command to `RTCVideoView`. |
| `RTCPIPViewProps` | TypeScript Interface | Props interface for `RTCPIPView`. |