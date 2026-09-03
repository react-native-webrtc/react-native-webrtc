# Technical Documentation: `src/EventEmitter.ts`

## Overview

The `src/EventEmitter.ts` module bridges native WebRTC events from React Native's `NativeModules.WebRTCModule` to a JavaScript-level event emitter (`EventEmitter`). 

It listens to native events emitted across the React Native bridge, re-emits them on a pure JavaScript `EventEmitter`, and provides lifecycle management for event listeners using a subscription mapping mechanism (`_subscriptions`).

---

## Key Components

### 1. Emitters & Module Reference

* **`WebRTCModule`**: Extracted from React Native's `NativeModules`. It serves as the native bridge target for event listening.
* **`nativeEmitter`**: An instance of `NativeEventEmitter(WebRTCModule)`. It directly interfaces with native platform events.
* **`eventEmitter`**: An instance of `EventEmitter` (imported from `react-native/Libraries/vendor/emitter/EventEmitter`). It handles JS-side event broadcasting to registered handlers.

### 2. State & Storage

* **`_subscriptions`**: A private `Map` defined as `Map<Listener, EmitterSubscription[]>`. It tracks all active `EmitterSubscription` objects associated with a specific `Listener` key. This allows all subscriptions registered under a given listener identifier to be removed at once.

### 3. Type Definitions

```typescript
type EventHandler = (event: unknown) => void;
type Listener = unknown;
```

* **`EventHandler`**: Callback function accepting a single parameter `event` of type `unknown` and returning `void`.
* **`Listener`**: Any value or object reference used as a unique key in the `_subscriptions` map.

---

## Constants

### `NATIVE_EVENTS`

An array of strings representing the supported native event names:

* `peerConnectionSignalingStateChanged`
* `peerConnectionStateChanged`
* `peerConnectionOnRenegotiationNeeded`
* `peerConnectionIceConnectionChanged`
* `peerConnectionIceGatheringChanged`
* `peerConnectionGotICECandidate`
* `peerConnectionDidOpenDataChannel`
* `peerConnectionOnRemoveTrack`
* `peerConnectionOnTrack`
* `dataChannelStateChanged`
* `dataChannelReceiveMessage`
* `dataChannelDidChangeBufferedAmount`
* `mediaStreamTrackMuteChanged`
* `mediaStreamTrackEnded`

---

## Exported Functions

### `setupNativeEvents()`

Attaches a listener on `nativeEmitter` for every event name listed in `NATIVE_EVENTS`. When `nativeEmitter` fires any of these native events, the callback forwards the event and all received arguments (`...args`) directly to `eventEmitter`.

```typescript
export function setupNativeEvents(): void
```

* **Parameters**: None.
* **Returns**: `void`.
* **Behavior**: Iterates through `NATIVE_EVENTS` and maps each event to `eventEmitter.emit(eventName, ...args)`.

---

### `addListener()`

Registers an event handler for a specific native event and associates the subscription with a given `listener` object/key.

```typescript
export function addListener(
    listener: Listener,
    eventName: string,
    eventHandler: EventHandler
): void
```

#### Parameters

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `listener` | `Listener` (`unknown`) | Key/identifier used to track and group the created subscription in `_subscriptions`. |
| `eventName` | `string` | The name of the event to listen for. Must exist in `NATIVE_EVENTS`. |
| `eventHandler` | `EventHandler` | Callback function to invoke when the event is emitted. |

#### Behavior & Validation
1. Validates `eventName`. If `eventName` is not in `NATIVE_EVENTS`, throws an `Error` with the message: `Invalid event: <eventName>`.
2. Checks if `_subscriptions` contains an entry for `listener`. If not, initializes a new empty array for that key.
3. Attaches `eventHandler` to `eventEmitter` for `eventName`.
4. Pushes the returned `EmitterSubscription` object into `_subscriptions` under the corresponding `listener` key.

---

### `removeListener()`

Removes all event subscriptions associated with a specific `listener` identifier and cleans up the tracking map.

```typescript
export function removeListener(listener: Listener): void
```

#### Parameters

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `listener` | `Listener` (`unknown`) | The key/identifier whose subscriptions should be removed. |

#### Behavior
1. Retrieves the array of `EmitterSubscription` objects associated with `listener` from `_subscriptions`.
2. Iterates over the array and calls `.remove()` on each subscription object.
3. Deletes the `listener` entry from `_subscriptions`.

---

## How It Works

1. **Initialization**:
   Call `setupNativeEvents()` once during initialization. This establishes permanent listeners on `nativeEmitter` for all predefined `NATIVE_EVENTS`. Any incoming native bridge event is automatically re-emitted on `eventEmitter`.

2. **Registering Listeners**:
   Call `addListener(listenerObj, eventName, handler)`.
   * The module validates that `eventName` exists within `NATIVE_EVENTS`.
   * It subscribes `handler` to `eventEmitter`.
   * It stores the subscription in the `_subscriptions` map tied to `listenerObj`.

3. **Event Relay**:
   When native code triggers an event (e.g., `peerConnectionStateChanged`), `nativeEmitter` catches it and calls `eventEmitter.emit('peerConnectionStateChanged', ...args)`. The JS-level `eventEmitter` then invokes all registered `EventHandler` callbacks for that event.

4. **Cleaning Up**:
   Call `removeListener(listenerObj)`. The module fetches all subscriptions linked to `listenerObj`, invokes `.remove()` on each `EmitterSubscription`, and removes `listenerObj` from `_subscriptions`.