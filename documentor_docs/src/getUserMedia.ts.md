# Technical Documentation: `src/getUserMedia.ts`

## Overview

The `src/getUserMedia.ts` module provides an asynchronous function, `getUserMedia`, which prompts the user for permission to use media inputs (audio and/or video) and requests a `MediaStream` from the native platform (`WebRTCModule`). It validates input constraints, requests necessary device permissions (microphone/camera), and normalizes constraints before delegating stream creation to the native layer.

---

## Imported Dependencies

| Import | Source | Description |
| :--- | :--- | :--- |
| `NativeModules` | `react-native` | Used to access the native bridge module `WebRTCModule`. |
| `MediaTrackConstraints` | `./Constraints` | Type definition for individual media track constraints. |
| `MediaStream` | `./MediaStream` | Constructor used to instantiate a `MediaStream` object upon success. |
| `MediaStreamError` | `./MediaStreamError` | Wrapper class for media stream related errors. |
| `permissions` | `./Permissions` | Utility used to request native device permissions (`microphone` and `camera`). |
| `RTCUtil` | `./RTCUtil` | Utility module providing `normalizeConstraints` and `deepClone`. |

---

## Types & Interfaces

### `Constraints`

Defines the structure for media stream constraints passed into `getUserMedia`.

```typescript
export interface Constraints {
    audio?: boolean | MediaTrackConstraints;
    video?: boolean | MediaTrackConstraints;
}
```

* **`audio`** *(optional)*: Accepts a `boolean` value or a `MediaTrackConstraints` object specifying audio requirements.
* **`video`** *(optional)*: Accepts a `boolean` value or a `MediaTrackConstraints` object specifying video requirements.

---

## Default Export Function: `getUserMedia`

### Signature

```typescript
export default function getUserMedia(constraints: Constraints = {}): Promise<MediaStream>
```

### Parameters

* `constraints` (`Constraints`, default: `{}`): An object specifying what types of media streams are requested (`audio`, `video`, or both).

### Returns

* `Promise<MediaStream>`: A Promise that resolves to a `MediaStream` instance if access is granted, or rejects with an error (`TypeError` or `MediaStreamError`).

---

## Detailed Execution Flow

When `getUserMedia(constraints)` is invoked, it follows these step-by-step processes:

```
[ Call getUserMedia(constraints) ]
               │
               ▼
   [ Validate Constraints ] ───( Invalid )───> [ Reject with TypeError ]
               │
           ( Valid )
               ▼
   [ Normalize Constraints ]
               │
               ▼
  [ Request Permissions Parallelly ]
  (Microphone / Camera as required)
               │
               ▼
    [ Check Permission Results ]
               │
     ┌─────────┴─────────┐
(Both Denied)        (At least one granted)
     │                   │
     ▼                   ▼
[ Reject with     [ Clean Ungranted Constraints ]
 SecurityError ]         │
                         ▼
             [ Call WebRTCModule.getUserMedia ]
                         │
              ┌──────────┴──────────┐
          ( Success )           ( Failure )
              │                     │
              ▼                     ▼
     [ Attach track constraints ]  [ Reject Promise with ]
     [ Resolve MediaStream ]       [ TypeError / MediaStreamError ]
```

### 1. Constraint Validation

Before processing, the function executes two validation checks:

1. **Dictionary Check**: Validates that `constraints` is an object.
   * If `typeof constraints !== 'object'`, the promise rejects with `TypeError('constraints is not a dictionary')`.
2. **Media Requirement Check**: Validates that at least one media type (`audio` or `video`) is requested.
   * If both `constraints.audio` and `constraints.video` are undefined or falsy, the promise rejects with `TypeError('audio and/or video is required')`.

### 2. Constraint Normalization

If validation passes, the module normalizes the constraints by calling:
```typescript
constraints = RTCUtil.normalizeConstraints(constraints);
```

### 3. Permission Requests

The module creates an array of promises (`reqPermissions`) to request device permissions using `permissions.request()`:

* **Microphone**: If `constraints.audio` is truthy, it pushes `permissions.request({ name: 'microphone' })`. Otherwise, it pushes `Promise.resolve(false)`.
* **Camera**: If `constraints.video` is truthy, it pushes `permissions.request({ name: 'camera' })`. Otherwise, it pushes `Promise.resolve(false)`.

### 4. Permission Resolution & Constraint Cleanup

Using `Promise.all(reqPermissions)`, the permissions resolution is handled:

1. **All Denied**: If neither `audioPerm` nor `videoPerm` are granted, the promise rejects with a `MediaStreamError` containing:
   * `name`: `'SecurityError'`
   * `message`: `'Permission denied.'`
2. **Partial Grant / Cleanup**: If at least one permission is granted, any media type whose permission was denied is removed from the `constraints` object (`delete constraints.audio` or `delete constraints.video`).

### 5. Native Module Execution

The module calls the native method:
```typescript
WebRTCModule.getUserMedia(constraints, success, failure);
```

#### Success Callback (`success(id, tracks)`)
1. Iterates over the returned `tracks`.
2. Clones original constraint objects onto each track via `RTCUtil.deepClone(c)` if `constraints[trackInfo.kind]` is an object.
3. Constructs an info object:
   ```typescript
   {
       streamId: id,
       streamReactTag: id,
       tracks
   }
   ```
4. Resolves the main promise with `new MediaStream(info)`.

#### Failure Callback (`failure(type, message)`)
1. Checks the error `type`.
2. If `type === 'TypeError'`, constructs a standard `TypeError(message)`.
3. Otherwise, constructs a `MediaStreamError({ message, name: type })`.
4. Rejects the main promise with the created error.

---

## Error Handling Summary

| Condition | Error Class | Error `name` / Message |
| :--- | :--- | :--- |
| `constraints` parameter is not an object | `TypeError` | `'constraints is not a dictionary'` |
| Neither `audio` nor `video` is truthy | `TypeError` | `'audio and/or video is required'` |
| Both requested permissions denied | `MediaStreamError` | `name: 'SecurityError'`, `message: 'Permission denied.'` |
| Native layer failure (`type === 'TypeError'`) | `TypeError` | Custom native error message |
| Native layer generic failure | `MediaStreamError` | `name: <type>`, `message: <message>` |