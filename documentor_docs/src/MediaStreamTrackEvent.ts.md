# Technical Documentation: `src/MediaStreamTrackEvent.ts`

## Overview

The `src/MediaStreamTrackEvent.ts` file defines the `MediaStreamTrackEvent` class and its supporting types. This class represents events dispatched when a `MediaStreamTrack` is added to or removed from a media stream (`addtrack` or `removetrack`). 

It extends a shimmed base `Event` class from `./vendor/event-target-shim` to provide typed event handling for media stream track state changes.

---

## Dependencies & Imports

- **`MediaStreamTrack`** (`import type MediaStreamTrack from './MediaStreamTrack'`): Imported as a type-only reference representing the media track associated with the event.
- **`Event`** (`import { Event } from './vendor/event-target-shim'`): Imported as the base class that `MediaStreamTrackEvent` extends.

---

## Type Definitions & Interfaces

### 1. `MEDIA_STREAM_EVENTS`

```typescript
type MEDIA_STREAM_EVENTS = 'addtrack' | 'removetrack';
```

A string literal union type that defines the valid event types supported by this event class:
* `'addtrack'`: Indicates a track was added.
* `'removetrack'`: Indicates a track was removed.

---

### 2. `IMediaStreamTrackEventInitDict`

```typescript
interface IMediaStreamTrackEventInitDict extends Event.EventInit {
  track: MediaStreamTrack;
}
```

An interface defining the initialization dictionary required when instantiating a `MediaStreamTrackEvent`.

* **Extends**: `Event.EventInit` (inherited configuration properties for the base `Event`).
* **Properties**:
  * `track` (`MediaStreamTrack`): The `MediaStreamTrack` instance associated with the event.

---

## Class Documentation

### `MediaStreamTrackEvent<TEventType extends MEDIA_STREAM_EVENTS>`

```typescript
export default class MediaStreamTrackEvent<TEventType extends MEDIA_STREAM_EVENTS> extends Event<TEventType>
```

The primary class exported by this module. It is a generic class constrained by `TEventType`, which must be a valid `MEDIA_STREAM_EVENTS` string literal (`'addtrack'` or `'removetrack'`).

#### Properties

| Property | Type | Description |
| :--- | :--- | :--- |
| `track` | `MediaStreamTrack` | The media stream track object associated with the event instance. |

---

#### Constructor

```typescript
constructor(type: TEventType, eventInitDict: IMediaStreamTrackEventInitDict)
```

Constructs a new `MediaStreamTrackEvent` instance.

* **Parameters**:
  * `type` (`TEventType`): The type of the event (`'addtrack'` or `'removetrack'`).
  * `eventInitDict` (`IMediaStreamTrackEventInitDict`): An object containing initialization properties, including the required `track` property.
* **Behavior**:
  1. Calls `super(type, eventInitDict)` to initialize the base `Event` instance.
  2. Assigns `eventInitDict.track` to the instance property `this.track`.