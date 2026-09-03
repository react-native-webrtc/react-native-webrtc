# Technical Documentation: `H264AndSoftwareVideoDecoderFactory.java`

## Overview

The `H264AndSoftwareVideoDecoderFactory` class is a custom WebRTC video decoder factory for Android located at `android/src/main/java/com/oney/WebRTCModule/webrtcutils/H264AndSoftwareVideoDecoderFactory.java`. 

Its primary purpose is to mirror the default iOS WebRTC decoder behavior by combining:
1. **Hardware acceleration** for specific H.264 profiles (Constrained Baseline and Constrained High).
2. **Software decoding** for all other codecs supported by the system's software factory proxy (such as VP8, VP9, and AV1).

---

## Class Architecture & Package

- **Package:** `com.oney.WebRTCModule.webrtcutils`
- **Implemented Interface:** `org.webrtc.VideoDecoderFactory`

---

## Member Variables

| Variable | Type | Description |
| :--- | :--- | :--- |
| `hardwareVideoDecoderFactory` | `VideoDecoderFactory` | Instance of `HardwareVideoDecoderFactory` responsible for handling hardware-accelerated video decoding. |
| `softwareVideoDecoderFactory` | `VideoDecoderFactory` | Instance of `SoftwareVideoDecoderFactoryProxy` responsible for handling software-based video decoding. |

---

## Constructor Documentation

### `H264AndSoftwareVideoDecoderFactory(@Nullable EglBase.Context eglContext)`

Initializes the hardware and software decoder factories.

- **Parameters:**
  - `eglContext` (`EglBase.Context`, optional): The OpenGLES context used by `HardwareVideoDecoderFactory` to render decoded frames via hardware textures. Can be `null`.
- **Implementation Logic:**
  - Instantiates `this.hardwareVideoDecoderFactory` using `new HardwareVideoDecoderFactory(eglContext)`.
  - Instantiates `this.softwareVideoDecoderFactory` using `new SoftwareVideoDecoderFactoryProxy()`.

---

## Methods

### `createDecoder(VideoCodecInfo codecInfo)`

Creates and returns a `VideoDecoder` instance appropriate for the provided codec information.

- **Parameters:**
  - `codecInfo` (`VideoCodecInfo`): Object containing details about the codec to be decoded (e.g., codec name, parameters).
- **Returns:**
  - `VideoDecoder`: An instantiated decoder object, or `null` if the codec is unsupported.
- **Logic:**
  1. Checks if `codecInfo.name` is equal to `"H264"` (case-insensitive).
  2. If the codec is **H.264**, delegates decoder creation to `hardwareVideoDecoderFactory.createDecoder(codecInfo)`.
  3. For **all other codecs**, delegates creation to `softwareVideoDecoderFactory.createDecoder(codecInfo)`.

---

### `getSupportedCodecs()`

Retrieves an array of all video codecs supported by this combined factory.

- **Returns:**
  - `VideoCodecInfo[]`: Array of supported codecs, prioritizing supported H.264 profiles followed by software codecs.
- **Logic:**
  1. Initializes a list `codecs` to store matching `VideoCodecInfo` items.
  2. Fetches the list of hardware codecs supported by calling `this.hardwareVideoDecoderFactory.getSupportedCodecs()`.
  3. Iterates through the hardware codecs:
     - Filters for codecs whose name is `"H264"` (case-insensitive).
     - Inspects the `VideoCodecInfo.H264_FMTP_PROFILE_LEVEL_ID` parameter from the codec's parameters map (`hwCodec.params`).
     - Ignores codecs where the profile level is `null`.
     - Identifies and captures the **Constrained High** profile (`VideoCodecInfo.H264_CONSTRAINED_HIGH_3_1`).
     - Identifies and captures the **Constrained Baseline** profile (`VideoCodecInfo.H264_CONSTRAINED_BASELINE_3_1`).
  4. Appends the identified hardware H.264 profiles to `codecs` in the following order (if present):
     - `h264High`
     - `h264Baseline`
  5. Fetches all software codecs by calling `this.softwareVideoDecoderFactory.getSupportedCodecs()` and appends them to the end of `codecs`.
  6. Converts `codecs` to a `VideoCodecInfo[]` array and returns it.

---

## Execution Flow Summary

```
                    ┌───────────────────────────────┐
                    │     createDecoder(codecInfo)  │
                    └───────────────┬───────────────┘
                                    │
                         Is codec name "H264"?
                        /                     \
                      YES                      NO
                      /                         \
  ┌───────────────────────────────┐   ┌───────────────────────────────┐
  │ hardwareVideoDecoderFactory   │   │ softwareVideoDecoderFactory   │
  │     .createDecoder(...)       │   │     .createDecoder(...)       │
  └───────────────────────────────┘   └───────────────────────────────┘
```