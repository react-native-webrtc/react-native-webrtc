# Technical Documentation: `src/RTCTrackEvent.ts`

## Overview

The `RTCTrackEvent.ts` module exports the `RTCTrackEvent` class, which represents the WebRTC `track` event. This event is dispatched when a new incoming media track is added or modified within an `RTCPeerConnection`. 

The class extends the custom `Event` implementation from `./vendor/event-target-shim` and encapsulates event data, including the associated `RTCRtpTransceiver`, `RTCRtpReceiver`, `MediaStreamTrack`, and array of `MediaStream` objects.

---

## Type Definitions & Interfaces

### `TRACK_EVENTS`
```typescript
type TRACK_EVENTS = 'track'
```
A type alias that restricts the allowed event type string strictly to `'track'`.

### `IRTCTrackEventInitDict`
```typescript
interface IRTCTrackEventInitDict extends Event.EventInit {
    streams: MediaStream[]
    transceiver: RTCRtpTransceiver
}
```
An interface extending `Event.EventInit` used to pass configuration options to the `RTCTrackEvent` constructor.

* **`streams`**: `MediaStream[]` — An array of `MediaStream` instances associated with the track.
* **`transceiver`**: `RTCRtpTransceiver` — The `RTCRtpTransceiver` associated with the track event.

---

## Class: `RTCTrackEvent<TEventType extends TRACK_EVENTS>`

Extends `Event<TEventType>`.

### Readonly Properties

| Property | Type | Description |
| :--- | :--- | :--- |
| `streams` | `MediaStream[]` | An array of `MediaStream` objects associated with the track event. Default is an empty array `[]`. |
| `transceiver` | `RTCRtpTransceiver` | The `RTCRtpTransceiver` associated with the event. |
| `receiver` | `RTCRtpReceiver \| null` | The `RTCRtpReceiver` extracted from the `transceiver.receiver` property. |
| `track` | `MediaStreamTrack \| null` | The `MediaStreamTrack` extracted from the receiver (`transceiver.receiver.track`) if a receiver exists; otherwise `null`. |

---

## Constructor

```typescript
constructor(type: TEventType, eventInitDict: IRTCTrackEventInitDict)
```

### Parameters
1. **`type`**: `TEventType` — The event type (must match `'track'`).
2. **`eventInitDict`**: `IRTCTrackEventInitDict` — An object containing initialization properties for the event.

### Initialization Logic
When an instance of `RTCTrackEvent` is constructed, the following sequence occurs:

1. **Base Event Initialization**: Calls `super(type, eventInitDict)` to initialize base event properties (such as event type and standard event options).
2. **Streams Assignment**: Sets `this.streams` to `eventInitDict.streams`.
3. **Transceiver Assignment**: Sets `this.transceiver` to `eventInitDict.transceiver`.
4. **Receiver Resolution**: Sets `this.receiver` to `eventInitDict.transceiver.receiver`.
5. **Track Resolution**: Evaluates whether `eventInitDict.transceiver.receiver` is present:
   * If `eventInitDict.transceiver.receiver` exists, `this.track` is assigned `eventInitDict.transceiver.receiver.track`.
   * If `eventInitDict.transceiver.receiver` is null/undefined, `this.track` is assigned `null`.

---

## Dependencies

* **`MediaStream`**: Imported from `./MediaStream`. Represents a stream of media content.
* **`MediaStreamTrack`**: Type-only import from `./MediaStreamTrack`. Represents a individual media track.
* **`RTCRtpReceiver`**: Imported from `./RTCRtpReceiver`. Handles the reception of media for an `RTCPeerConnection`.
* **`RTCRtpTransceiver`**: Imported from `./RTCRtpTransceiver`. Constructs the transceiver pairing a sender and receiver.
* **`Event`**: Imported from `./vendor/event-target-shim`. Provides the base `Event` implementation.