# Technical Documentation: `src/MediaStream.ts`

## Overview

The `MediaStream.ts` file exports the `MediaStream` class, which serves as a JavaScript wrapper for managing WebRTC media streams in a React Native environment. It extends `EventTarget` (from a local vendor shim) to support event listener management for track events (`addtrack` and `removetrack`). 

The class synchronizes media stream operations (creation, track addition, track removal, and stream release) between JavaScript and the native layer via React Native's `NativeModules.WebRTCModule`.

---

## Dependencies and Imports

*   **`NativeModules` (from `react-native`)**: Used to access `WebRTCModule` for native bridge communications.
*   **`MediaStreamTrack`, `{ MediaStreamTrackInfo }` (from `./MediaStreamTrack`)**: Represents track objects and track metadata types contained within a media stream.
*   **`MediaStreamTrackEvent` (from `./MediaStreamTrackEvent`)**: Defines event objects dispatched for `addtrack` and `removetrack` events.
*   **`uniqueID` (from `./RTCUtil`)**: A utility function used to generate unique identifiers (UUIDs).
*   **`EventTarget`, `getEventAttributeValue`, `setEventAttributeValue` (from `./vendor/event-target-shim`)**: Provides the base class for event target handling and helper functions for event attribute getters/setters.

---

## Class Definition & Type Definitions

### `MediaStreamEventMap`
A type mapping event names to their corresponding event objects:
*   `addtrack`: `MediaStreamTrackEvent<'addtrack'>`
*   `removetrack`: `MediaStreamTrackEvent<'removetrack'>`

### `MediaStream` Class
`MediaStream` extends `EventTarget<MediaStreamEventMap>`.

#### Internal Properties
*   `_tracks: MediaStreamTrack[]`: An internal array holding the `MediaStreamTrack` instances assigned to this stream. Default is `[]`.
*   `_id: string`: The standard identifier for the stream.
*   `_reactTag: string`: The native bridge identifier used to associate the JS `MediaStream` instance with its native counterpart in `WebRTCModule`.

---

## Constructor

```typescript
constructor(arg?:
    MediaStream |
    MediaStreamTrack[] |
    { streamId: string, streamReactTag: string, tracks: MediaStreamTrackInfo[] }
)
```

The constructor behavior depends on the type of `arg` provided:

1.  **Default / `undefined`**:
    *   Generates a local UUID for `_id` and sets `_reactTag` equal to `_id`.
    *   Calls `WebRTCModule.mediaStreamCreate(this.id)`.

2.  **`MediaStream` instance**:
    *   Generates a local UUID for `_id` and sets `_reactTag` equal to `_id`.
    *   Calls `WebRTCModule.mediaStreamCreate(this.id)`.
    *   Iterates over `arg.getTracks()` and adds each track to the new stream using `this.addTrack(track)`.

3.  **`MediaStreamTrack[]` Array**:
    *   Generates a local UUID for `_id` and sets `_reactTag` equal to `_id`.
    *   Calls `WebRTCModule.mediaStreamCreate(this.id)`.
    *   Iterates over the array and adds each track using `this.addTrack(track)`.

4.  **Internal Object Descriptor (`{ streamId, streamReactTag, tracks }`)**:
    *   Used internally when a stream is first created on the native side.
    *   Assigns `this._id = arg.streamId` and `this._reactTag = arg.streamReactTag`.
    *   Instantiates a new `MediaStreamTrack` for each entry in `arg.tracks` and pushes it directly into `this._tracks` (does **not** call `addTrack` or trigger native creation calls).

5.  **Invalid Type**:
    *   Throws a `TypeError` if `arg` does not match any of the above formats.

---

## Getters and Setters

### Event Attributes
*   **`onaddtrack`**: Getter/Setter wrapping `getEventAttributeValue` and `setEventAttributeValue` for the `'addtrack'` event.
*   **`onremovetrack`**: Getter/Setter wrapping `getEventAttributeValue` and `setEventAttributeValue` for the `'removetrack'` event.

### Properties
*   **`id: string`**: Returns `this._id`.
*   **`active: boolean`**: Always returns `true`.

---

## Instance Methods

### `addTrack(track: MediaStreamTrack): void`
Adds a `MediaStreamTrack` to the stream.
*   Checks if the track already exists in `_tracks`. If present, the method exits without action.
*   Pushes `track` into `_tracks`.
*   Invokes `WebRTCModule.mediaStreamAddTrack`:
    *   Parameter 1: `this._reactTag`
    *   Parameter 2: `track.remote ? track._peerConnectionId : -1`
    *   Parameter 3: `track.id`

### `removeTrack(track: MediaStreamTrack): void`
Removes a `MediaStreamTrack` from the stream.
*   Searches for `track` in `_tracks`. If not found, the method exits without action.
*   Removes the track from `_tracks` using `splice`.
*   Invokes `WebRTCModule.mediaStreamRemoveTrack`:
    *   Parameter 1: `this._reactTag`
    *   Parameter 2: `track.remote ? track._peerConnectionId : -1`
    *   Parameter 3: `track.id`

### `getTracks(): MediaStreamTrack[]`
Returns a shallow copy array of all `MediaStreamTrack` instances currently in `_tracks`.

### `getTrackById(trackId: any): MediaStreamTrack | undefined`
Searches `_tracks` and returns the track whose `id` strictly equals `trackId`. Returns `undefined` if no match is found.

### `getAudioTracks(): MediaStreamTrack[]`
Returns a filtered array of tracks from `_tracks` where `track.kind === 'audio'`.

### `getVideoTracks(): MediaStreamTrack[]`
Returns a filtered array of tracks from `_tracks` where `track.kind === 'video'`.

### `clone(): never`
Throws an `Error('Not implemented.')`.

### `toURL(): string`
Returns `this._reactTag`.

### `release(releaseTracks: boolean = true): void`
Releases the stream native resources and cleans up tracks.
1.  Creates a copy of `_tracks`.
2.  Iterates over each track:
    *   Calls `this.removeTrack(track)`.
    *   If `releaseTracks` is `true` (default), calls `track.release()`.
3.  Calls `WebRTCModule.mediaStreamRelease(this._reactTag)`.

---

## Native Bridge (`WebRTCModule`) Interface

This class directly invokes the following native methods on `WebRTCModule`:

| Native Method | Parameters Passed | Triggered By |
| :--- | :--- | :--- |
| `mediaStreamCreate` | `(streamId: string)` | `constructor()` (when creating local/empty/cloned streams) |
| `mediaStreamAddTrack` | `(reactTag: string, peerConnectionId: number, trackId: string)` | `addTrack()` |
| `mediaStreamRemoveTrack` | `(reactTag: string, peerConnectionId: number, trackId: string)` | `removeTrack()` |
| `mediaStreamRelease` | `(reactTag: string)` | `release()` |