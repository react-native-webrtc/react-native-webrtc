# Developer Documentation: `SoftwareVideoEncoderFactoryProxy.java`

## Overview

* **File Path:** `android/src/main/java/com/oney/WebRTCModule/webrtcutils/SoftwareVideoEncoderFactoryProxy.java`
* **Package:** `com.oney.WebRTCModule.webrtcutils`
* **Interface Implemented:** `org.webrtc.VideoEncoderFactory`

`SoftwareVideoEncoderFactoryProxy` is a thread-safe proxy class that implements WebRTC's `VideoEncoderFactory` interface. It provides a lazy-initialization wrapper around WebRTC's native `SoftwareVideoEncoderFactory`.

---

## Purpose

Starting with WebRTC milestone M111, `org.webrtc.SoftwareVideoEncoderFactory` relies on JNI bindings that require `PeerConnectionFactory` to be fully initialized beforehand. Instantiating `SoftwareVideoEncoderFactory` directly prior to `PeerConnectionFactory` initialization results in runtime crashes or errors.

`SoftwareVideoEncoderFactoryProxy` solves this timing issue by deferring the creation of the underlying `SoftwareVideoEncoderFactory` until an encoder method (`createEncoder` or `getSupportedCodecs`) is explicitly called for the first time.

---

## Key Components

### Class Declaration
```java
public class SoftwareVideoEncoderFactoryProxy implements VideoEncoderFactory
```
Implements `org.webrtc.VideoEncoderFactory` so it can be passed anywhere a WebRTC video encoder factory is required.

### Private Fields

* `private VideoEncoderFactory factory;`
  * Holds the cached reference to the `SoftwareVideoEncoderFactory` once initialized. Defaults to `null`.

### Methods

#### `private synchronized VideoEncoderFactory getFactory()`
* **Purpose:** Handles the lazy initialization of the internal `VideoEncoderFactory`.
* **Behavior:** Checks if `factory` is `null`. If so, it instantiates a new `SoftwareVideoEncoderFactory()`. It returns the non-null `factory` instance.
* **Concurrency:** Marked as `synchronized` to ensure thread-safe initialization across concurrent WebRTC calls.

#### `@Nullable @Override public VideoEncoder createEncoder(VideoCodecInfo videoCodecInfo)`
* **Parameters:** `VideoCodecInfo videoCodecInfo` — Information regarding the codec to create (e.g., VP8, VP9, H264).
* **Returns:** `VideoEncoder` (or `null` if the encoder cannot be created).
* **Behavior:** Obtains the underlying factory via `getFactory()` and delegates the `createEncoder(videoCodecInfo)` call to it.

#### `@Override public VideoCodecInfo[] getSupportedCodecs()`
* **Returns:** `VideoCodecInfo[]` — An array of supported software video codecs.
* **Behavior:** Obtains the underlying factory via `getFactory()` and delegates the `getSupportedCodecs()` call to it.

---

## How It Works

1. **Instantiation:** `SoftwareVideoEncoderFactoryProxy` is created early in the application/module lifecycle without instantiating any native WebRTC JNI dependencies. Its internal field `factory` remains `null`.
2. **First Interaction:** WebRTC invokes either `getSupportedCodecs()` or `createEncoder(...)` on the proxy instance.
3. **Lazy Initialization:**
   * The proxy calls its private `getFactory()` method.
   * `getFactory()` locks the method (`synchronized`), initializes `factory = new SoftwareVideoEncoderFactory()`, and returns it.
4. **Delegation:** The requested method call is forwarded to the newly created `SoftwareVideoEncoderFactory` instance.
5. **Subsequent Calls:** Any subsequent calls reuse the already initialized `factory` reference without re-creating the native `SoftwareVideoEncoderFactory`.