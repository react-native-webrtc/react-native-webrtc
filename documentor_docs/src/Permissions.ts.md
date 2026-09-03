# Technical Documentation: `src/Permissions.ts`

## Overview

The `src/Permissions.ts` module provides a cross-platform permissions interface for React Native applications targeting **Android**, **iOS**, and **macOS**. It implements a subset of the W3C Permissions API standard specifically tailored for camera and microphone access.

The module exports a singleton instance of the `Permissions` class as its default export.

---

## Type Definitions

### `PermissionDescriptor`

Defines the structure for specifying a target permission.

```typescript
type PermissionDescriptor = {
    name: string;
};
```

* **`name`**: The string identifier of the requested permission (must be either `'camera'` or `'microphone'`).

---

## Class Properties

The `Permissions` class defines the following properties:

| Property | Type | Description |
| :--- | :--- | :--- |
| `RESULT` | `object` | An object map representing standard permission result status strings (`DENIED: 'denied'`, `GRANTED: 'granted'`, `PROMPT: 'prompt'`). |
| `VALID_PERMISSIONS` | `string[]` | Array listing the supported permission names: `['camera', 'microphone']`. |
| `_lastReq` | `Promise<unknown>` | Internal promise chain reference used to sequence permission requests sequentially on Android. Initialized to `Promise.resolve()`. |

---

## Public Methods

### `query(permissionDesc: PermissionDescriptor): Promise<string>`

Queries the current status of a specified permission.

#### Parameters
* **`permissionDesc`**: A `PermissionDescriptor` object containing the `name` of the permission to query.

#### Return Value
Returns a `Promise` that resolves to the permission status string or rejects with a `TypeError`.

#### Platform Behavior
* **Android**:
  * Maps `'camera'` to `PermissionsAndroid.PERMISSIONS.CAMERA` and `'microphone'` to `PermissionsAndroid.PERMISSIONS.RECORD_AUDIO`.
  * Calls `PermissionsAndroid.check(perm)`.
  * Resolves to `this.RESULT.GRANTED` (`'granted'`) if the check succeeds and returns `true`.
  * Resolves to `this.RESULT.PROMPT` (`'prompt'`) if the check returns `false` or if an error occurs.
* **iOS / macOS**:
  * Calls native method `WebRTCModule.checkPermission(permissionDesc.name)` and returns its promise.
* **Other Platforms**:
  * Rejects with `TypeError('Unsupported platform.')`.

---

### `request(permissionDesc: PermissionDescriptor): Promise<any>`

Requests access for a specified permission. This is a custom method not present in the W3C Permissions API standard.

#### Parameters
* **`permissionDesc`**: A `PermissionDescriptor` object containing the `name` of the permission to request.

#### Return Value
Returns a `Promise` that resolves to the result of the permission request or rejects with a `TypeError`.

#### Platform Behavior
* **Android**:
  * Maps permission names to Android native constants (`CAMERA` or `RECORD_AUDIO`).
  * Enqueues the request sequentially onto `this._lastReq` using promise chaining (`this._lastReq.then(requestPermission, requestPermission)`) to prevent concurrent permission prompts.
  * Resolves to a boolean (`true` if granted, `false` otherwise).
* **iOS / macOS**:
  * Calls native method `WebRTCModule.requestPermission(permissionDesc.name)` and returns its promise.
* **Other Platforms**:
  * Rejects with `TypeError('Unsupported platform.')`.

---

## Internal Helper Methods

### `_validatePermissionDescriptor(permissionDesc: any): void`

Validates the input object provided to `query` or `request`.

#### Throws:
* **`TypeError`**: `"Argument 1 of Permissions.query is not an object."` — If `permissionDesc` is not of type `'object'`.
* **`TypeError`**: `"Missing required 'name' member of PermissionDescriptor."` — If `permissionDesc.name` is `undefined`.
* **`TypeError`**: `"'name' member of PermissionDescriptor is not a valid value for enumeration PermissionName."` — If `permissionDesc.name` is not included in `this.VALID_PERMISSIONS`.

---

### `_requestPermissionAndroid(perm: Permission): Promise<boolean>`

Internal helper method for handling individual Android system permission prompts.

#### Execution Details:
* Calls `PermissionsAndroid.request(perm)`.
* Resolves to `true` if the returned result equals `PermissionsAndroid.RESULTS.GRANTED`.
* Resolves to `false` if the permission is rejected or an error occurs.

---

## Module Export

The module executes a default export of a instantiated `Permissions` object:

```typescript
export default new Permissions();
```