# Technical Documentation: `src/RTCAudioSession.ts`

## Overview

The `RTCAudioSession` class provides static utility methods for notifying the underlying native WebRTC module (`WebRTCModule`) about iOS CallKit audio session lifecycle events. It acts as a bridge between React Native audio management (such as CallKit events) and the native iOS WebRTC layer.

---

## Dependencies

*   **`react-native`**:
    *   `NativeModules`: Used to access native bridges, specifically `WebRTCModule`.
    *   `Platform`: Used to check the operating system of the target device (`Platform.OS`).

---

## Class Definition

### `RTCAudioSession`

A default exported class containing static methods to manage audio session activation state.

---

## Static Methods

### `audioSessionDidActivate()`

Notifies the native `WebRTCModule` that the CallKit audio session has activated.

*   **Syntax:**
    ```typescript
    RTCAudioSession.audioSessionDidActivate(): void
    ```
*   **Parameters:** None.
*   **Return Value:** `void`
*   **Behavior:**
    1. Checks if the runtime platform is iOS (`Platform.OS === 'ios'`).
    2. If true, invokes `WebRTCModule.audioSessionDidActivate()`.
    3. If false (e.g., Android), takes no action.

---

### `audioSessionDidDeactivate()`

Notifies the native `WebRTCModule` that the CallKit audio session has deactivated.

*   **Syntax:**
    ```typescript
    RTCAudioSession.audioSessionDidDeactivate(): void
    ```
*   **Parameters:** None.
*   **Return Value:** `void`
*   **Behavior:**
    1. Checks if the runtime platform is iOS (`Platform.OS === 'ios'`).
    2. If true, invokes `WebRTCModule.audioSessionDidDeactivate()`.
    3. If false (e.g., Android), takes no action.

---

## Platform Specificity

The logic within `RTCAudioSession` is **strictly restricted to iOS**. Both static methods contain explicit checks for `Platform.OS === 'ios'`. Calling these methods on non-iOS platforms will result in a no-op (no operations performed).