# Technical Documentation: `H264AndSoftwareVideoEncoderFactory`

**File Path:** `android/src/main/java/com/oney/WebRTCModule/webrtcutils/H264AndSoftwareVideoEncoderFactory.java`  
**Package:** `com.oney.WebRTCModule.webrtcutils`  

---

## Overview

The `H264AndSoftwareVideoEncoderFactory` class is a custom implementation of the WebRTC `VideoEncoderFactory` interface for Android. Its primary purpose is to act as a hybrid video encoder factory that selectively routes video encoding tasks between hardware-accelerated encoding and software encoding:

* **Hardware Encoding:** Used strictly for H.264 video encoding (specifically targeting Constrained High and Constrained Baseline profiles).
* **Software Encoding:** Used for software-supported codecs (such as VP8, VP9, and AV1) delegated via a software encoder proxy.

This architecture mirrors default behavior often seen on iOS WebRTC implementations, providing dedicated hardware acceleration for H.264 while falling back to software encoding for other formats.

---

## Class Architecture & Dependencies

### Interfaces Implemented
* `org.webrtc.VideoEncoderFactory`: WebRTC native interface for creating video encoders and querying supported video codecs.

### Import Dependencies
* `androidx.annotation.Nullable`
* `org.webrtc.EglBase`
* `org.webrtc.HardwareVideoEncoderFactory`
* `org.webrtc.VideoCodecInfo`
* `org.webrtc.VideoEncoder`
* `org.webrtc.VideoEncoderFactory`
* `java.util.ArrayList`, `java.util.Arrays`, `java.util.List`

---

## Member Variables

| Variable Name | Type | Visibility | Description |
| :--- | :--- | :--- | :--- |
| `hardwareVideoEncoderFactory` | `VideoEncoderFactory` | `private final` | An instance of `HardwareVideoEncoderFactory` used to construct and query hardware-accelerated H.264 encoders. |
| `softwareVideoEncoderFactory` | `VideoEncoderFactory` | `private final` | An instance of `SoftwareVideoEncoderFactoryProxy` used to construct and query software-based video encoders. |

---

## Constructor

### `H264AndSoftwareVideoEncoderFactory(@Nullable EglBase.Context eglContext)`

Initializes the internal hardware and software video encoder factories.

* **Parameters:**
  * `eglContext` (`@Nullable EglBase.Context`): The OpenGL ES Context required by hardware encoders for texture-based video processing.
* **Initialization Details:**
  1. `hardwareVideoEncoderFactory`: Instantiated via `new HardwareVideoEncoderFactory(eglContext, false, true)`.
     * Arg 1 (`eglContext`): Passed directly.
     * Arg 2 (`enableIntelVp8`): Set to `false`.
     * Arg 3 (`enableH264HighProfile`): Set to `true`.
  2. `softwareVideoEncoderFactory`: Instantiated via `new SoftwareVideoEncoderFactoryProxy()`.

---

## Methods

### 1. `createEncoder(VideoCodecInfo codecInfo)`

Determines whether to instantiate a hardware or software encoder based on the supplied codec information.

* **Signature:**
  ```java
  @Nullable
  @Override
  public VideoEncoder createEncoder(VideoCodecInfo codecInfo)
  ```
* **Parameters:**
  * `codecInfo` (`VideoCodecInfo`): Metadata containing the codec name and parameters.
* **Returns:**
  * `VideoEncoder`: An instance of a hardware or software video encoder, or `null` if the codec cannot be created.
* **Logic:**
  1. Checks if `codecInfo.name` is equal to `"H264"` (case-insensitive via `equalsIgnoreCase`).
  2. **If H.264:** Delegates creation to `this.hardwareVideoEncoderFactory.createEncoder(codecInfo)`.
  3. **Otherwise:** Delegates creation to `this.softwareVideoEncoderFactory.createEncoder(codecInfo)`.

---

### 2. `getSupportedCodecs()`

Queries both factories and returns a filtered array of supported video codecs. Hardware codecs are filtered specifically for supported H.264 profiles.

* **Signature:**
  ```java
  @Override
  public VideoCodecInfo[] getSupportedCodecs()
  ```
* **Returns:**
  * `VideoCodecInfo[]`: An array containing filtered hardware H.264 codecs followed by software-supported codecs.
* **Logic Flow:**
  1. Initializes a list `codecs` to aggregate `VideoCodecInfo` objects.
  2. Queries `this.hardwareVideoEncoderFactory.getSupportedCodecs()`.
  3. Iterates over the hardware codecs array and checks for `"H264"` codecs (case-insensitive):
     * Retrieves the parameter `VideoCodecInfo.H264_FMTP_PROFILE_LEVEL_ID` from the codec parameters map (`hwCodec.params`).
     * If missing (`null`), skips the entry.
     * If matching `VideoCodecInfo.H264_CONSTRAINED_HIGH_3_1` (case-insensitive), stores reference in `h264High`.
     * If matching `VideoCodecInfo.H264_CONSTRAINED_BASELINE_3_1` (case-insensitive), stores reference in `h264Baseline`.
  4. Appends `h264High` (if non-null) to the `codecs` list.
  5. Appends `h264Baseline` (if non-null) to the `codecs` list.
  6. Queries `this.softwareVideoEncoderFactory.getSupportedCodecs()` and appends all returned software codecs to the `codecs` list.
  7. Converts the aggregated `codecs` list into an array of `VideoCodecInfo` and returns it.

---

## Summary of Codec Routing Behavior

| Codec / Profile | Encoding Method Target |
| :--- | :--- |
| **H.264 (Constrained High Profile 3.1)** | `HardwareVideoEncoderFactory` |
| **H.264 (Constrained Baseline Profile 3.1)** | `HardwareVideoEncoderFactory` |
| **Other Codecs (e.g., VP8, VP9, AV1)** | `SoftwareVideoEncoderFactoryProxy` |