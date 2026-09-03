# Technical Documentation: `WebRTCModule+VideoTrackAdapter.h`

**File Path:** `ios/RCTWebRTC/WebRTCModule+VideoTrackAdapter.h`  
**Language:** Objective-C Header File

---

## Overview

The `WebRTCModule+VideoTrackAdapter.h` header file defines an Objective-C category named `VideoTrackAdapter` on the `RTCPeerConnection` class. This category extends `RTCPeerConnection` with a dynamic property to store video track adapters and method signatures to manage (add or remove) adapters for specific `RTCVideoTrack` instances.

---

## File Dependencies / Imports

* **`<WebRTC/RTCPeerConnection.h>`**: Imports the standard native WebRTC framework interface for `RTCPeerConnection`.
* **`"WebRTCModule.h"`**: Imports the core React Native WebRTC module header file.

---

## Interface & Category Summary

* **Target Class:** `RTCPeerConnection`
* **Category Name:** `VideoTrackAdapter`

```objc
@interface RTCPeerConnection (VideoTrackAdapter)
```

By extending `RTCPeerConnection` via an Objective-C category, this file allows instances of `RTCPeerConnection` to directly manage state and operations related to video track adapters without modifying the original WebRTC framework class implementation.

---

## Properties

### `videoTrackAdapters`

```objc
@property(nonatomic, strong) NSMutableDictionary<NSString *, id> *videoTrackAdapters;
```

* **Type:** `NSMutableDictionary<NSString *, id> *`
* **Attributes:** `nonatomic`, `strong`
* **Description:** A mutable dictionary property attached to `RTCPeerConnection` instances. It maps `NSString` keys to adapter objects (`id`) associated with video tracks.

---

## Instance Methods

### `addVideoTrackAdapter:`

```objc
- (void)addVideoTrackAdapter:(RTCVideoTrack *)track;
```

* **Parameters:**
  * `track` (`RTCVideoTrack *`): The native WebRTC video track instance for which an adapter should be added.
* **Return Value:** `void`
* **Description:** Declares the method responsible for creating/adding a video track adapter associated with the provided `RTCVideoTrack`.

---

### `removeVideoTrackAdapter:`

```objc
- (void)removeVideoTrackAdapter:(RTCVideoTrack *)track;
```

* **Parameters:**
  * `track` (`RTCVideoTrack *`): The native WebRTC video track instance whose associated adapter should be removed.
* **Return Value:** `void`
* **Description:** Declares the method responsible for removing the video track adapter associated with the provided `RTCVideoTrack`.

---

## How It Works

1. **Category Extension:** The file declares a category on `RTCPeerConnection`, making adapter-management properties and methods available directly on `RTCPeerConnection` objects.
2. **State Storage:** The `videoTrackAdapters` property acts as a container (dictionary) to retain active adapters using string keys.
3. **Adapter Lifecycle:** Callers use `addVideoTrackAdapter:` to set up an adapter for a given `RTCVideoTrack` instance, and `removeVideoTrackAdapter:` to clean up or teardown the adapter when the track is no longer managed.