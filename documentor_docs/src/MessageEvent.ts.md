# Technical Documentation: `src/MessageEvent.ts`

## Overview

The `src/MessageEvent.ts` file defines a custom `MessageEvent` class along with its supporting types and interfaces. This class extends a base `Event` implementation imported from `./vendor/event-target-shim` and is designed to represent events fired when messages are received or encounter errors (typically associated with an `RTCDataChannel`).

---

## Type Definitions

### `MessageEventData`

```typescript
export type MessageEventData = string | ArrayBuffer | Blob;
```

Represents the allowed data types for a message event payload. 
- **Types allowed**: `string`, `ArrayBuffer`, or `Blob`.

---

### `MESSAGE_EVENTS`

```typescript
type MESSAGE_EVENTS = 'message' | 'messageerror';
```

An internal union type restricting valid event type names for `MessageEvent`.
- `'message'`: Fired when a message is successfully received.
- `'messageerror'`: Fired when a message cannot be deserialized or encountered an error.

---

## Interfaces

### `IMessageEventInitDict`

```typescript
interface IMessageEventInitDict extends Event.EventInit {
    data: MessageEventData;
}
```

Defines the initialization dictionary required to construct a `MessageEvent` instance.

* **Extends**: `Event.EventInit` (base event initialization options like `bubbles` or `cancelable`).
* **Properties**:
  * `data` (`MessageEventData`): The payload data associated with the event (`string`, `ArrayBuffer`, or `Blob`).

---

## Class: `MessageEvent<TEventType>`

```typescript
export default class MessageEvent<TEventType extends MESSAGE_EVENTS> extends Event<TEventType>
```

A generic event class for message-related events. 

### Type Parameters

* `TEventType`: Must extend `MESSAGE_EVENTS` (`'message'` or `'messageerror'`).

### Properties

| Property | Type | Description |
| :--- | :--- | :--- |
| `data` | `MessageEventData` | The data payload contained within the event (`string`, `ArrayBuffer`, or `Blob`). |

### Constructor

```typescript
constructor(type: TEventType, eventInitDict: IMessageEventInitDict)
```

Constructs a new `MessageEvent` instance.

* **Parameters**:
  * `type` (`TEventType`): The name of the event (`'message'` or `'messageerror'`).
  * `eventInitDict` (`IMessageEventInitDict`): An object containing initialization properties, including the required `data` property.
* **Execution Logic**:
  1. Calls `super(type, eventInitDict)` to initialize the base `Event` class.
  2. Assigns `eventInitDict.data` to `this.data`.

---

## Summary of Operations

1. **Imports**: Imports `Event` base class from `./vendor/event-target-shim`.
2. **Type Checking**: Restricts the event types strictly to `'message'` or `'messageerror'` through generics and internal type aliases.
3. **Initialization**: Accepts event parameters via `IMessageEventInitDict` and stores the event payload in the public `data` property.