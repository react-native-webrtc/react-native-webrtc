# Technical Documentation: `SoftwareVideoDecoderFactoryProxy.java`

**File Location:** `android/src/main/java/com/oney/WebRTCModule/webrtcutils/SoftwareVideoDecoderFactoryProxy.java`  
**Package:** `com.oney.WebRTCModule.webrtcutils`  
**Interface Implemented:** `org.webrtc.VideoDecoderFactory`

---

## 1. Overview

`SoftwareVideoDecoderFactoryProxy` is a proxy factory class that wraps the native WebRTC `SoftwareVideoDecoderFactory`. It implements a **lazy initialization pattern** to defer the creation of the underlying `SoftwareVideoDecoderFactory` until it is explicitly needed.

---

## 2. Purpose

Starting with WebRTC milestone **M111**, the default `SoftwareVideoDecoderFactory` relies on Java Native Interface (JNI) dependencies that require `PeerConnectionFactory` to be initialized first. If a `SoftwareVideoDecoderFactory` instance is instantiated before `PeerConnectionFactory` initialization completes, runtime errors or crashes occur.

`SoftwareVideoDecoderFactoryProxy` addresses this issue by acting as a lightweight placeholder (`proxy`) that implements `VideoDecoderFactory`. It delays the actual instantiation of `SoftwareVideoDecoderFactory` until the first time a decoder method (`createDecoder` or `getSupportedCodecs`) is invoked.

---

## 3. Class Structure & Fields

### Private Fields

* **`private VideoDecoderFactory factory`**
  * **Type:** `org.webrtc.VideoDecoderFactory`
  * **Description:** Holds the internal reference to the actual `SoftwareVideoDecoderFactory` instance once initialized. Remains `null` until first accessed.

---

## 4. Key Methods

### `private synchronized VideoDecoderFactory getFactory()`
* **Access Modifier:** `private`
* **Synchronization:** `synchronized` (Thread-safe)
* **Return Type:** `VideoDecoderFactory`
* **Description:** Performs the lazy initialization of the `factory` field.
* **Logic:**
  1. Checks if `factory` is `null`.
  2. If `null`, instantiates a new `org.webrtc.SoftwareVideoDecoderFactory()`.
  3. Returns the initialized `factory` instance.

---

### `@Nullable @Override public VideoDecoder createDecoder(VideoCodecInfo videoCodecInfo)`
* **Access Modifier:** `public`
* **Annotations:** `@Nullable`, `@Override`
* **Parameters:** 
  * `videoCodecInfo` (`VideoCodecInfo`): Metadata describing the video codec to create a decoder for.
* **Return Type:** `VideoDecoder` (Nullable)
* **Description:** Implementation of `VideoDecoderFactory.createDecoder`. Delegates the decoder creation request to the internal factory retrieved via `getFactory()`.

---

### `@Override public VideoCodecInfo[] getSupportedCodecs()`
* **Access Modifier:** `public`
* **Annotations:** `@Override`
* **Parameters:** None
* **Return Type:** `VideoCodecInfo[]`
* **Description:** Implementation of `VideoDecoderFactory.getSupportedCodecs`. Retrieves the list of video codecs supported by the software decoder by delegating to the internal factory retrieved via `getFactory()`.

---

## 5. How It Works

```
                     [ Caller / WebRTC Engine ]
                                 │
                   Calls createDecoder() / getSupportedCodecs()
                                 │
                                 ▼
              ┌─────────────────────────────────────┐
              │ SoftwareVideoDecoderFactoryProxy   │
              └─────────────────────────────────────┘
                                 │
                         Calls getFactory()
                                 │
                ┌────────────────┴────────────────┐
                ▼                                 ▼
      [ factory == null ]               [ factory != null ]
                │                                 │
    Instantiates new                      Returns existing 
 SoftwareVideoDecoderFactory()                factory
                │                                 │
                └────────────────┬────────────────┘
                                 │
                                 ▼
               Delegates call to SoftwareVideoDecoderFactory
                                 │
                                 ▼
                  Returns VideoDecoder or Codec Info
```

1. **Instantiation:** `SoftwareVideoDecoderFactoryProxy` is safely instantiated without triggering any WebRTC JNI native calls.
2. **Method Invocation:** When WebRTC requests codec support or decoder creation:
   - Either `createDecoder(...)` or `getSupportedCodecs()` is called.
   - The method calls `getFactory()`.
3. **Lazy Initialization:** `getFactory()` checks if `factory` is initialized. If `null`, it instantiates `SoftwareVideoDecoderFactory`. The `synchronized` keyword ensures this step is thread-safe.
4. **Delegation:** The proxy forwards the original call to the underlying `SoftwareVideoDecoderFactory` instance and returns the result.