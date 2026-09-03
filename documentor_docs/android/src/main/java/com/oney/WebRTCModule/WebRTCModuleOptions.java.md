# Technical Documentation: `WebRTCModuleOptions.java`

## Overview

The `WebRTCModuleOptions` class is a singleton configuration container within the `com.oney.WebRTCModule` package. It provides public fields to store customizable options, custom factories, logging settings, and feature flags used by the WebRTC Android module.

## File Information

* **File Path:** `android/src/main/java/com/oney/WebRTCModule/WebRTCModuleOptions.java`
* **Package:** `com.oney.WebRTCModule`

---

## Design Pattern

`WebRTCModuleOptions` implements a basic **Singleton** pattern using lazy initialization. This guarantees a single shared instance of the configuration across the module.

---

## Class Architecture & Members

### Static Fields

| Name | Type | Visibility | Description |
| :--- | :--- | :--- | :--- |
| `instance` | `WebRTCModuleOptions` | `private static` | Holds the single static instance of `WebRTCModuleOptions`. |

---

### Public Instance Fields

The class exposes seven public fields that hold references to WebRTC configuration objects and flags:

| Field Name | Data Type | Import Source | Description |
| :--- | :--- | :--- | :--- |
| `videoEncoderFactory` | `VideoEncoderFactory` | `org.webrtc.VideoEncoderFactory` | Reference to a custom video encoder factory. |
| `videoDecoderFactory` | `VideoDecoderFactory` | `org.webrtc.VideoDecoderFactory` | Reference to a custom video decoder factory. |
| `audioDeviceModule` | `AudioDeviceModule` | `org.webrtc.audio.AudioDeviceModule` | Reference to a custom audio device module. |
| `injectableLogger` | `Loggable` | `org.webrtc.Loggable` | Custom logger implementation for WebRTC native logs. |
| `loggingSeverity` | `Logging.Severity` | `org.webrtc.Logging.Severity` | Defines the severity level for WebRTC logging. |
| `fieldTrials` | `String` | `java.lang.String` | Configuration string for WebRTC field trials. |
| `enableMediaProjectionService` | `boolean` | Primitive `boolean` | Flag indicating whether the MediaProjection service feature is enabled. |

---

### Methods

#### `getInstance()`

```java
public static WebRTCModuleOptions getInstance()
```

* **Purpose:** Provides access to the global `WebRTCModuleOptions` instance.
* **Logic:** 
  1. Checks if `instance` is `null`.
  2. If `null`, instantiates a new `WebRTCModuleOptions` object and assigns it to `instance`.
  3. Returns `instance`.
* **Return Value:** The single `WebRTCModuleOptions` instance.

---

## How It Works

1. **Instantiation:** Call `WebRTCModuleOptions.getInstance()` to obtain the global configuration object.
2. **Configuration:** Since the member variables (`videoEncoderFactory`, `audioDeviceModule`, `loggingSeverity`, etc.) are public fields, components can directly assign or read these values on the returned instance.