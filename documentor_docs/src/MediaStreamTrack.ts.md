# MediaStreamTrack.ts Documentation Guide

The `MediaStreamTrack.ts` module provides a TypeScript implementation of the WebRTC standard `MediaStreamTrack` interface for React Native. It manages individual audio or video tracks, handles property updates (such as enabling/disabling audio/video or applying constraints), and communicates with native platform modules (`WebRTCModule`) via a native bridge.

---

## Type Definitions

### `MediaStreamTrackState`
Represents the current lifecycle state of a track.
* **Type**: `'live' | 'ended'`

### `MediaStreamTrackInfo`
An object structure required to initialize a `MediaStreamTrack` instance.
* `id` (`string`): Unique identifier for the track.
* `kind` (`string`): The media track type (e.g., `'audio'` or `'video'`).
* `remote` (`boolean`): Indicates if the track is coming from a remote peer (`true`) or generated locally (`false`).
* `constraints` (`object`): Initial track constraints.
* `enabled` (`boolean`): Initial enabled state of the track.
* `settings` (`object`): Initial track settings.
* `peerConnectionId` (`number`): The ID of the associated PeerConnection.
* `readyState` (`MediaStreamTrackState`): Initial track state (`'live'` or `'ended'`).

### `MediaTrackSettings`
Represents settings associated with a media track.
* `width` (`number`, optional): Frame width in pixels.
* `height` (`number`, optional): Frame height in pixels.
* `frameRate` (`number`, optional): Frame rate in frames per second.
* `facingMode` (`string`, optional): Camera direction (e.g., `'user'`, `'environment'`).
* `deviceId` (`string`, optional): Identifier for the source device.
* `groupId` (`string`, optional): Group identifier for the source device.

### `MediaStreamTrackEventMap`
Defines events supported by `MediaStreamTrack`:
* `ended`: Dispatched when the track ends.
* `mute`: Dispatched when a remote track is muted.
* `unmute`: Dispatched when a remote track is unmuted.

---

## Class: `MediaStreamTrack`

Extends `EventTarget<MediaStreamTrackEventMap>` to provide event handling functionality (`addEventListener`, `removeEventListener`, `dispatchEvent`).

### Public Properties

| Property | Type | Access | Description |
| :--- | :--- | :--- | :--- |
| `id` | `string` | Readonly | Unique track identifier. |
| `kind` | `string` | Readonly | Kind of track (e.g., `'audio'` or `'video'`). |
| `label` | `string` | Readonly | Description label of the track (defaults to empty string `''`). |
| `remote` | `boolean` | Readonly | Whether the track is remote (`true`) or local (`false`). |
| `enabled` | `boolean` | Read/Write | Controls whether the track outputs media. Setting this communicates the state to native code unless the track is in `'ended'` state. |
| `muted` | `boolean` | Readonly | Indicates whether the track is muted. |
| `readyState` | `string` | Readonly | Returns the track state (`'live'` or `'ended'`). |

---

### Event Handler Attributes

Dynamic getters and setters are implemented using `getEventAttributeValue` and `setEventAttributeValue`:

* **`onended`**: Callback for the `ended` event.
* **`onmute`**: Callback for the `mute` event.
* **`onunmute`**: Callback for the `unmute` event.

---

### Constructor

```typescript
constructor(info: MediaStreamTrackInfo)
```

1. Calls `super()` to initialize event target functionality.
2. Initializes internal state variables (`_constraints`, `_enabled`, `_settings`, `_muted`, `_peerConnectionId`, `_readyState`).
3. Sets public readonly fields (`id`, `kind`, `remote`).
4. If the track is **local** (`!this.remote`), registers event listeners via `_registerEvents()`.

---

### Standard WebRTC Methods

#### `stop(): void`
Stops the track by setting `enabled = false` and updating `_readyState` to `'ended'`.

#### `applyConstraints(constraints?: MediaTrackConstraints): Promise<void>`
Applies video constraints to the track.
* **Requirements**: Only implemented for `video` tracks (throws an error if `kind !== 'video'`).
* **Behavior**:
  * Preserves existing `facingMode` if not explicitly specified in the incoming constraints.
  * Normalizes video constraints using `normalizeConstraints`.
  * Calls `WebRTCModule.mediaStreamTrackApplyConstraints`.
  * Updates `_settings` with the native response and updates `_constraints`.

#### `getConstraints(): MediaTrackConstraints`
Returns a deep clone of the currently applied constraints (`_constraints`).

#### `getSettings(): MediaTrackSettings`
Returns a deep clone of the current settings (`_settings`).

#### `clone(): never`
* **Status**: Not implemented. Throws `Error('Not implemented.')`.

#### `getCapabilities(): never`
* **Status**: Not implemented. Throws `Error('Not implemented.')`.

---

### Custom & Internal Methods

#### `_switchCamera(): void`
* **Deprecated**: Replaced by `applyConstraints`.
* **Requirements**: Only for local video tracks (`!this.remote` and `this.kind === 'video'`).
* **Behavior**: Toggles `facingMode` between `'user'` and `'environment'` and calls `applyConstraints()`.

#### `_setVideoEffects(names: string[]): void`
* **Requirements**: Only for local video tracks (`!this.remote` and `this.kind === 'video'`).
* **Behavior**: Invokes `WebRTCModule.mediaStreamTrackSetVideoEffects(this.id, names)`.

#### `_setVideoEffect(name: string): void`
Convenience wrapper that calls `_setVideoEffects([name])`.

#### `_setMutedInternal(muted: boolean): void`
* **Requirements**: Only for remote tracks (`this.remote === true`).
* **Behavior**: Updates internal `_muted` state and dispatches either a `mute` or `unmute` `Event`.

#### `_setVolume(volume: number): void`
* **Requirements**: Only for audio tracks (`this.kind === 'audio'`).
* **Parameters**: `volume` — Gain value expected in range `0-10`.
* **Behavior**: Invokes `WebRTCModule.mediaStreamTrackSetVolume` with the peer connection ID (or `-1` if local) and track ID.

#### `_registerEvents(): void`
Registers a native event listener (`mediaStreamTrackEnded`). When triggered for this track's ID (and if not already ended), updates `_readyState` to `'ended'` and dispatches an `'ended'` event.

#### `release(): void`
For local tracks (`!this.remote`), removes event listeners registered with the track and calls `WebRTCModule.mediaStreamTrackRelease(this.id)`.

---

## Native Module Calls (`WebRTCModule`)

This class interacts directly with `NativeModules.WebRTCModule` through the following native methods:

| Native Method | Invoking Function | Description |
| :--- | :--- | :--- |
| `mediaStreamTrackSetEnabled` | `enabled` setter | Updates enabled state on native side. |
| `mediaStreamTrackApplyConstraints` | `applyConstraints()` | Applies constraints natively and returns updated settings. |
| `mediaStreamTrackSetVideoEffects` | `_setVideoEffects()` | Sets array of native video effects. |
| `mediaStreamTrackSetVolume` | `_setVolume()` | Sets native audio track volume. |
| `mediaStreamTrackRelease` | `release()` | Releases local native track resources. |