# Developer Documentation: `src/MediaStreamErrorEvent.ts`

## Overview

The `MediaStreamErrorEvent` class provides a custom event object structure for representing errors associated with media streams. It stores an event type and allows arbitrary initialization properties (such as an optional `error` payload) to be copied onto the event instance.

---

## Dependencies

* **`MediaStreamError`** (Type import): Imported from `./MediaStreamError`. Used as a type annotation for the optional `error` property.

---

## Class Definition

### `MediaStreamErrorEvent`

```typescript
export default class MediaStreamErrorEvent
```

A default-exported class representing a media stream error event.

---

## Properties

| Property | Type | Optional | Description |
| :--- | :--- | :--- | :--- |
| `type` | `string` | No | A string representing the type or name of the event. |
| `error` | `MediaStreamError` | Yes | An optional error object containing details about the media stream error. |

---

## Constructor

### `constructor(type, eventInitDict)`

Initializes a new instance of the `MediaStreamErrorEvent` class.

#### Parameters

* **`type`**: The event type. The constructor calls `.toString()` on this argument to guarantee the internal `type` property is stored as a string.
* **`eventInitDict`**: An object containing initial property values to assign to the instance.

#### Constructor Logic

1. Converts the `type` parameter to a string by calling `type.toString()` and assigns it to `this.type`.
2. Calls `Object.assign(this, eventInitDict)` to copy all enumerable own properties from `eventInitDict` onto the new `MediaStreamErrorEvent` instance (such as the `error` property if provided in `eventInitDict`).

---

## How It Works

1. **Instantiation**: When `new MediaStreamErrorEvent(type, eventInitDict)` is invoked, it accepts an event identifier (`type`) and an initialization dictionary (`eventInitDict`).
2. **Type Normalization**: The `type` argument is converted to a string using `.toString()`.
3. **Property Copying**: `Object.assign` copies properties from `eventInitDict` directly onto `this`. If `eventInitDict` includes an `error` property matching the `MediaStreamError` structure, it is assigned to the instance.