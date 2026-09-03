# Documentation: `src/RTCRtpTransceiver.ts`

## Overview

The `RTCRtpTransceiver` class represents an RTP transceiver, which pairs an `RTCRtpSender` and an `RTCRtpReceiver` associated with a specific peer connection (`_peerConnectionId`). It provides access to the media direction, current negotiation state, associated media ID (MID), sender, receiver, and methods to update settings like direction and preferred codecs.

This class communicates directly with the underlying React Native bridge via `NativeModules.WebRTCModule`.

---

## Dependencies

- **React Native**: Imports `NativeModules` to access native WebRTC functions (`WebRTCModule`).
- **Internal Modules**:
  - `RTCRtpCodecCapability`: Type definition for codec capabilities.
  - `RTCRtpReceiver`: Represents the receiver object attached to this transceiver.
  - `RTCRtpSender`: Represents the sender object attached to this transceiver.

---

## Class Properties

### Internal Properties

| Property | Type | Description |
| :--- | :--- | :--- |
| `_peerConnectionId` | `number` | The unique identifier for the associated native PeerConnection instance. |
| `_sender` | `RTCRtpSender` | The `RTCRtpSender` instance associated with this transceiver. |
| `_receiver` | `RTCRtpReceiver` | The `RTCRtpReceiver` instance associated with this transceiver. |
| `_mid` | `string \| null` | The media ID assigned during SDP negotiation. Defaults to `null`. |
| `_direction` | `string` | The preferred media direction requested by the transceiver. |
| `_currentDirection` | `string` | The currently active media direction negotiated by the peer connection. |
| `_stopped` | `boolean` | Indicates whether the transceiver has been permanently stopped. |

---

## Constructor

```typescript
constructor(args: {
    peerConnectionId: number,
    isStopped: boolean,
    direction: string,
    currentDirection: string,
    mid?: string,
    sender: RTCRtpSender,
    receiver: RTCRtpReceiver,
})
```

### Constructor Parameters

The constructor accepts a single configuration object containing:

- `peerConnectionId`: (`number`) ID of the associated peer connection.
- `isStopped`: (`boolean`) Initial stopped status of the transceiver.
- `direction`: (`string`) Initial media direction (`'sendrecv'`, `'sendonly'`, `'recvonly'`, or `'inactive'`).
- `currentDirection`: (`string`) Initial negotiated media direction.
- `mid`: (`string`, optional) Initial media stream ID. Defaults to `null` if not provided.
- `sender`: (`RTCRtpSender`) The sender instance.
- `receiver`: (`RTCRtpReceiver`) The receiver instance.

---

## Getters and Setters

### Read-Only Getters

#### `mid`
```typescript
get mid(): string | null
```
Returns the media ID (`_mid`) assigned to this transceiver, or `null` if unassigned.

#### `stopped`
```typescript
get stopped(): boolean
```
Returns `true` if the transceiver is stopped; otherwise, `false`.

#### `currentDirection`
```typescript
get currentDirection(): string
```
Returns the negotiated direction (`_currentDirection`) currently in effect.

#### `sender`
```typescript
get sender(): RTCRtpSender
```
Returns the associated `RTCRtpSender` instance.

#### `receiver`
```typescript
get receiver(): RTCRtpReceiver
```
Returns the associated `RTCRtpReceiver` instance.

---

### Read-Write Property Accessors

#### `direction`

```typescript
get direction(): string
set direction(val: string)
```

- **Getter**: Returns the current preferred direction string (`_direction`).
- **Setter**: Updates the preferred direction for the transceiver.

##### Allowed Setter Values
- `'sendonly'`
- `'recvonly'`
- `'sendrecv'`
- `'inactive'`

##### Setter Error Handling & Validation
1. **Invalid Value**: Throws a `TypeError('Invalid direction provided')` if `val` is not one of the allowed strings.
2. **Stopped Transceiver**: Throws an `Error('Transceiver Stopped')` if `_stopped` is `true`.
3. **No-Op**: If `val` is identical to `this._direction`, the setter returns early without taking action.
4. **Optimistic Native Sync**:
   - The instance optimistically sets `this._direction = val`.
   - Calls `WebRTCModule.transceiverSetDirection(this._peerConnectionId, this.sender.id, val)`.
   - If the native promise rejects, it rolls back `this._direction` to the previous direction value (`oldDirection`).

---

## Public Methods

### `stop()`

```typescript
stop(): void
```

Stops the transceiver.
- **Behavior**:
  1. Checks if the transceiver is already stopped (`_stopped === true`). If so, returns immediately.
  2. Calls `WebRTCModule.transceiverStop(this._peerConnectionId, this.sender.id)`.
  3. Upon successful resolution of the native call, invokes internal `_setStopped()`.

---

### `setCodecPreferences()`

```typescript
setCodecPreferences(codecs: RTCRtpCodecCapability[]): void
```

Configures preferred codecs for media transmission on this transceiver.
- **Parameters**: `codecs` - Array of `RTCRtpCodecCapability` objects specifying codec preferences.
- **Behavior**: Calls `WebRTCModule.transceiverSetCodecPreferences(this._peerConnectionId, this.sender.id, codecs)`.

---

## Internal Methods

### `_setStopped()`

```typescript
_setStopped(): void
```

Helper method executed when the transceiver is stopped. It updates local properties:
- Sets `_stopped` to `true`.
- Sets `_direction` to `'stopped'`.
- Sets `_currentDirection` to `'stopped'`.
- Resets `_mid` to `null`.

---

## Native Module Calls (`WebRTCModule`)

This class interacts with `NativeModules.WebRTCModule` using the following signatures:

1. `transceiverSetDirection(peerConnectionId: number, senderId: string, direction: string): Promise<void>`
2. `transceiverStop(peerConnectionId: number, senderId: string): Promise<void>`
3. `transceiverSetCodecPreferences(peerConnectionId: number, senderId: string, codecs: RTCRtpCodecCapability[]): void`