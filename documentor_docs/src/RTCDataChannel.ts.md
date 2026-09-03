# Technical Documentation: `RTCDataChannel.ts`

## Overview

The `RTCDataChannel` class provides a JavaScript interface for bidirectional, peer-to-peer data transfer in a React Native environment. It acts as a wrapper around native WebRTC data channels by interfacing with the React Native native module (`WebRTCModule`) and extending `EventTarget` to mimic standard W3C WebRTC `RTCDataChannel` behavior.

---

## Key Dependencies

* **`react-native` (`NativeModules`)**: Used to access `WebRTCModule`, which handles lower-level native data channel operations.
* **`base64-js`**: Handles encoding outbound binary data to Base64 strings and decoding incoming Base64 strings into `ArrayBuffer` objects.
* **`./vendor/event-target-shim`**: Supplies `EventTarget` and attribute helper methods (`getEventAttributeValue`, `setEventAttributeValue`) to manage standard web event listening patterns.
* **`./EventEmitter`**: Facilitates listening to and removing native bridge events (`addListener`, `removeListener`).
* **`./MessageEvent` & `./RTCDataChannelEvent`**: Custom event implementations dispatched when channel state, messages, or buffer levels change.

---

## Type Definitions

### `RTCDataChannelState`
Defines the possible connection states for the data channel:
```typescript
type RTCDataChannelState = 'connecting' | 'open' | 'closing' | 'closed';
```

### `DataChannelEventMap`
Maps standard event names to their corresponding event objects:
* `bufferedamountlow`: `RTCDataChannelEvent<'bufferedamountlow'>`
* `close`: `RTCDataChannelEvent<'close'>`
* `closing`: `RTCDataChannelEvent<'closing'>`
* `error`: `RTCDataChannelEvent<'error'>`
* `message`: `MessageEvent<'message'>`
* `open`: `RTCDataChannelEvent<'open'>`

---

## Class Member Overview

### Internal Properties

| Property | Type | Description |
| :--- | :--- | :--- |
| `_peerConnectionId` | `number` | ID of the parent `RTCPeerConnection` instance on the native side. |
| `_reactTag` | `string` | Unique identifier for this specific data channel bridge instance. |
| `_bufferedAmount` | `number` | Number of bytes currently queued to be sent. |
| `_id` | `number \| null` | Stream identifier for the channel. Set to `null` if initial value is `-1` until negotiated. |
| `_label` | `string` | Name/label describing the data channel. |
| `_maxPacketLifeTime` | `number \| undefined` | Maximum time (in ms) retransmissions may occur. |
| `_maxRetransmits` | `number \| undefined` | Maximum number of retransmission attempts. |
| `_negotiated` | `boolean` | Indicates if the channel was negotiated by the application out-of-band. |
| `_ordered` | `boolean` | Indicates whether message delivery order is guaranteed. |
| `_protocol` | `string` | Subprotocol name used for the channel. |
| `_readyState` | `RTCDataChannelState` | Current state of the data channel connection. |

### Public Instance Properties

* **`binaryType`** (`string`): Set to `'arraybuffer'`. *Note: Only `'arraybuffer'` is supported.*
* **`bufferedAmountLowThreshold`** (`number`): Threshold in bytes. When `_bufferedAmount` drops below this value, a `bufferedamountlow` event is fired. Defaults to `0`.

---

## Constructor

```typescript
constructor(info: any)
```

Constructs an instance of `RTCDataChannel`, assigns internal state properties from the `info` object, and registers native event listeners.

#### Initialization Logic:
1. Calls `super()` to initialize `EventTarget`.
2. Stores `_peerConnectionId` and `_reactTag`.
3. Normalizes `_id`: converts `-1` to `null`.
4. Converts `ordered` and `negotiated` fields to `boolean`.
5. Sets `_protocol` to `info.protocol` or defaults to `''`.
6. Invokes `this._registerEvents()` to subscribe to native bridge events.

---

## Read-Only Getters

Standard W3C standard getters exposing internal property states:

* **`bufferedAmount`**: Returns current queued bytes (`_bufferedAmount`).
* **`label`**: Returns channel label (`_label`).
* **`id`**: Returns channel ID (`_id`).
* **`ordered`**: Returns ordering flag (`_ordered`).
* **`maxPacketLifeTime`**: Returns max packet lifetime setting (`_maxPacketLifeTime`).
* **`maxRetransmits`**: Returns max retransmits setting (`_maxRetransmits`).
* **`protocol`**: Returns subprotocol name (`_protocol`).
* **`negotiated`**: Returns out-of-band negotiation flag (`_negotiated`).
* **`readyState`**: Returns current state string (`_readyState`).

---

## Event Handlers (Getters & Setters)

Standard WebRTC `on*` event property handlers managed via `getEventAttributeValue` and `setEventAttributeValue`:

* `onbufferedamountlow`
* `onclose`
* `onclosing`
* `onerror`
* `onmessage`
* `onopen`

---

## Instance Methods

### `send()`

Sends data across the channel to the remote peer.

```typescript
send(data: string): void;
send(data: ArrayBuffer): void;
send(data: ArrayBufferView): void;
send(data: string | ArrayBuffer | ArrayBufferView): void;
```

#### Behavior:
* **String Data**: Calls `WebRTCModule.dataChannelSend` passing `this._peerConnectionId`, `this._reactTag`, data, and type `'text'`.
* **Binary Data (`ArrayBuffer` / `ArrayBufferView`)**:
  1. Normalizes input into a `Uint8Array`.
  2. Encodes the binary payload into a Base64 string using `base64.fromByteArray()`.
  3. Calls `WebRTCModule.dataChannelSend` passing `this._peerConnectionId`, `this._reactTag`, Base64 string, and type `'binary'`.
* **Invalid Input**: Throws a `TypeError` if data is not a `string`, `ArrayBuffer`, or `ArrayBufferView`.

---

### `close()`

Initiates closure of the data channel.

```typescript
close(): void
```

#### Behavior:
* Checks if `_readyState` is already `'closing'` or `'closed'`. If so, returns immediately without action.
* Otherwise, calls `WebRTCModule.dataChannelClose(this._peerConnectionId, this._reactTag)`.

---

### Internal Event Registration (`_registerEvents()`)

Registers listeners with native event emitters via `addListener`. Every listener validates `ev.reactTag === this._reactTag` before processing.

```typescript
_registerEvents(): void
```

#### Native Bridge Subscriptions:

1. **`dataChannelStateChanged`**
   * Updates `_readyState` with `ev.state`.
   * Updates `_id` if currently `null` and `ev.id !== -1`.
   * **State Dispatches**:
     * `'open'`: Dispatches `RTCDataChannelEvent('open')`.
     * `'closing'`: Dispatches `RTCDataChannelEvent('closing')`.
     * `'closed'`: Dispatches `RTCDataChannelEvent('close')`, removes event listeners on this channel via `removeListener(this)`, and disposes native resources via `WebRTCModule.dataChannelDispose(this._peerConnectionId, this._reactTag)`.

2. **`dataChannelReceiveMessage`**
   * Decodes incoming message payload:
     * If `ev.type === 'binary'`, converts Base64 string back into an `ArrayBuffer` via `base64.toByteArray(ev.data).buffer`.
     * If text, uses raw string (`ev.data`).
   * Dispatches a `MessageEvent('message', { data })`.

3. **`dataChannelDidChangeBufferedAmount`**
   * Updates `_bufferedAmount` to `ev.bufferedAmount`.
   * Checks if `_bufferedAmount < this.bufferedAmountLowThreshold`. If true, dispatches `RTCDataChannelEvent('bufferedamountlow')`.