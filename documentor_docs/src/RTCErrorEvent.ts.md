# Technical Documentation: `src/RTCErrorEvent.ts`

## Overview

The `src/RTCErrorEvent.ts` module defines a custom event class, `RTCErrorEvent`, which extends the base `Event` class from `./vendor/event-target-shim`. Its primary purpose is to represent internal native-side errors that occur during asynchronous invocations related to synchronous WebRTC API operations.

---

## Type Definitions

### `RTCPeerConnectionErrorFunc`

A string union type defining the specific set of `RTCPeerConnection` function names that can be associated with an `RTCErrorEvent`.

```typescript
type RTCPeerConnectionErrorFunc =
    | 'addTransceiver'
    | 'getTransceivers'
    | 'addTrack'
    | 'removeTrack';
```

#### Allowed Values
* `'addTransceiver'`
* `'getTransceivers'`
* `'addTrack'`
* `'removeTrack'`

---

## Class: `RTCErrorEvent<TEventType>`

`RTCErrorEvent` is a generic class that extends `Event<TEventType>`. The generic type parameter `TEventType` is constrained to types extending `RTCPeerConnectionErrorFunc`.

### Generics
* **`TEventType`**: Constrained by `RTCPeerConnectionErrorFunc`. Represents the event type name associated with the event instance.

### Properties

| Property | Type | Modifiers | Description |
| :--- | :--- | :--- | :--- |
| `func` | `RTCPeerConnectionErrorFunc` | `readonly` | Identifies the `RTCPeerConnection` function name where the native error originated. |
| `message` | `string` | `readonly` | A human-readable description of the error message. |

---

### Constructor

```typescript
constructor(type: TEventType, func: RTCPeerConnectionErrorFunc, message: string)
```

#### Parameters

1. **`type`** (`TEventType`): The name/identifier of the event type. Passed directly to the superclass (`Event`) constructor.
2. **`func`** (`RTCPeerConnectionErrorFunc`): The specific peer connection function name associated with the error (e.g., `'addTrack'`).
3. **`message`** (`string`): The error detail message describing what went wrong.

#### Behavior

1. Calls `super(type)` to initialize the underlying event object from `event-target-shim`.
2. Assigns the `func` argument to `this.func`.
3. Assigns the `message` argument to `this.message`.

---

## Code Example / Usage Pattern

Based strictly on the code structure, an instance of `RTCErrorEvent` is constructed as follows:

```typescript
import RTCErrorEvent from './RTCErrorEvent';

// Constructing an error event for a failed 'addTrack' invocation
const errorEvent = new RTCErrorEvent(
    'addTrack', 
    'addTrack', 
    'Failed to add track due to an internal native error.'
);

console.log(errorEvent.type);    // "addTrack"
console.log(errorEvent.func);    // "addTrack"
console.log(errorEvent.message); // "Failed to add track due to an internal native error."
```