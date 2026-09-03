# Module Documentation: `src/RTCDataChannelEvent.ts`

## Overview

The `src/RTCDataChannelEvent.ts` file provides a custom event implementation for WebRTC data channels. It exports the generic `RTCDataChannelEvent` class, which extends the base `Event` class from `event-target-shim`. This event is used to notify listeners about events or state changes associated with an `RTCDataChannel` instance.

---

## Dependencies

* **`RTCDataChannel`** (Type import from `./RTCDataChannel`): Represents the WebRTC data channel instance associated with the event.
* **`Event`** (Imported from `./vendor/event-target-shim`): The base event class providing event implementation functionality.

---

## Types & Interfaces

### `DATA_CHANNEL_EVENTS`

A union type defining all valid event type strings supported by `RTCDataChannelEvent`:

```typescript
type DATA_CHANNEL_EVENTS = 
  | 'open'
  | 'message'
  | 'bufferedamountlow'
  | 'closing'
  | 'close'
  | 'error'
  | 'datachannel';
```

### `IRTCDataChannelEventInitDict`

An interface that extends `Event.EventInit` to define the initialization dictionary required when instantiating an `RTCDataChannelEvent`.

```typescript
interface IRTCDataChannelEventInitDict extends Event.EventInit {
    channel: RTCDataChannel;
}
```

#### Properties

| Property | Type | Description |
| :--- | :--- | :--- |
| `channel` | `RTCDataChannel` | The `RTCDataChannel` instance associated with this event. |
| *(inherited)* | `Event.EventInit` | standard event initialization properties (e.g., `bubbles`, `cancelable`). |

---

## Class: `RTCDataChannelEvent<TEventType>`

A generic class representing an event dispatched for an `RTCDataChannel`.

### Generics

* **`TEventType`**: Constrained to `DATA_CHANNEL_EVENTS`. Represents the specific event type string being instantiated.

### Inheritance

* Extends `Event<TEventType>`

### Properties

| Property | Type | Description |
| :--- | :--- | :--- |
| `channel` | `RTCDataChannel` | The WebRTC data channel associated with the event instance. |

---

### Constructor

```typescript
constructor(type: TEventType, eventInitDict: IRTCDataChannelEventInitDict)
```

#### Parameters

1. **`type`** (`TEventType`): The type of event being fired (must be one of the values in `DATA_CHANNEL_EVENTS`).
2. **`eventInitDict`** (`IRTCDataChannelEventInitDict`): Configuration object containing event initialization options and the mandatory `channel` property.

#### Implementation Details

1. Calls the parent `Event` constructor via `super(type, eventInitDict)`.
2. Assigns the `channel` property from `eventInitDict.channel` to `this.channel`.

---

## Usage Summary

The `RTCDataChannelEvent` class is instantiated by passing a valid data channel event type string and an initialization object containing the `RTCDataChannel` instance:

```typescript
import RTCDataChannelEvent from './RTCDataChannelEvent';

const event = new RTCDataChannelEvent('open', {
    channel: dataChannelInstance
});

// Access the channel property from the event instance
console.log(event.channel);
```