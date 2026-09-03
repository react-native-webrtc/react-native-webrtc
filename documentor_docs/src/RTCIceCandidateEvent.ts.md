# Module Documentation: `src/RTCIceCandidateEvent.ts`

## Overview

The `src/RTCIceCandidateEvent.ts` module defines the `RTCIceCandidateEvent` class and its supporting types. This class represents events related to WebRTC ICE (Interactive Connectivity Establishment) candidates (specifically `icecandidate` and `icecandidateerror`). It extends the base `Event` implementation provided by `event-target-shim`.

---

## Type Definitions and Interfaces

### `RTC_ICECANDIDATE_EVENTS`

```typescript
type RTC_ICECANDIDATE_EVENTS = 'icecandidate' | 'icecandidateerror';
```

A union type representing the valid string event names accepted by `RTCIceCandidateEvent`:
* `'icecandidate'`
* `'icecandidateerror'`

---

### `IRTCDataChannelEventInitDict`

```typescript
interface IRTCDataChannelEventInitDict extends Event.EventInit {
    candidate: RTCIceCandidate | null;
}
```

An interface defining the initialization dictionary used when constructing an `RTCIceCandidateEvent`.

* **Extends**: `Event.EventInit` (from `./vendor/event-target-shim`).
* **Properties**:
  * `candidate` (`RTCIceCandidate | null`): The ICE candidate associated with the event, or `null` if no candidate is present (e.g., when ICE gathering completes).

---

## Class: `RTCIceCandidateEvent<TEventType>`

```typescript
export default class RTCIceCandidateEvent<TEventType extends RTC_ICECANDIDATE_EVENTS> extends Event<TEventType>
```

A generic event class that extends `Event<TEventType>` to handle ICE candidate-related events.

### Type Parameters

* **`TEventType`**: Must extend `RTC_ICECANDIDATE_EVENTS` (`'icecandidate'` or `'icecandidateerror'`).

---

### Class Properties

#### `candidate`

```typescript
candidate: RTCIceCandidate | null;
```

* **Type**: `RTCIceCandidate | null`
* **Description**: Holds the `RTCIceCandidate` instance passed via the initialization dictionary, or `null` if none was provided.

---

### Constructor

```typescript
constructor(type: TEventType, eventInitDict: IRTCDataChannelEventInitDict)
```

Constructs a new `RTCIceCandidateEvent` instance.

#### Parameters

1. **`type`** (`TEventType`): The type of event being created (must be `'icecandidate'` or `'icecandidateerror'`).
2. **`eventInitDict`** (`IRTCDataChannelEventInitDict`): An object containing initialization options for the event, including the `candidate` property.

#### Implementation Details

* Calls `super(type, eventInitDict)` to initialize the base `Event`.
* Assigns `this.candidate` using optional chaining and nullish coalescing:
  ```typescript
  this.candidate = eventInitDict?.candidate ?? null;
  ```
  If `eventInitDict` or `eventInitDict.candidate` is `undefined` or `null`, `this.candidate` defaults to `null`.

---

## Dependencies

* **`./RTCIceCandidate`**: Imports the `RTCIceCandidate` type definition.
* **`./vendor/event-target-shim`**: Imports the base `Event` class and `Event.EventInit` interface.