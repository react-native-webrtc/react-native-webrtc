# Technical Documentation: `WebRTCModuleOptions.h`

## Overview

The `WebRTCModuleOptions.h` header file defines the `WebRTCModuleOptions` interface. This class serves as a global configuration singleton for setting up WebRTC module options on iOS. It provides configurable properties for video codecs (encoders and decoders), audio devices, field trials, logging levels, and camera access options during iOS multitasking.

---

## Class Details

- **Class Name:** `WebRTCModuleOptions`
- **Inherits From:** `NSObject`
- **Design Pattern:** Singleton
- **Nullability Annotations:** Enclosed within `NS_ASSUME_NONNULL_BEGIN` and `NS_ASSUME_NONNULL_END` macros.

---

## Class Methods

### `+ (instancetype _Nonnull)sharedInstance;`

- **Description:** Provides access to the shared singleton instance of `WebRTCModuleOptions`.
- **Return Value:** A non-null instance of `WebRTCModuleOptions`.

---

## Properties

All properties are declared using `nonatomic`.

| Property Name | Type | Memory Attribute | Nullability | Description |
| :--- | :--- | :--- | :--- | :--- |
| `videoDecoderFactory` | `id<RTCVideoDecoderFactory>` | `strong` | `nullable` | Custom video decoder factory adhering to the `RTCVideoDecoderFactory` protocol. |
| `videoEncoderFactory` | `id<RTCVideoEncoderFactory>` | `strong` | `nullable` | Custom video encoder factory adhering to the `RTCVideoEncoderFactory` protocol. |
| `audioDevice` | `id<RTCAudioDevice>` | `strong` | `nullable` | Custom audio device implementation adhering to the `RTCAudioDevice` protocol. |
| `fieldTrials` | `NSDictionary *` | `strong` | `nullable` | Key-value mapping representing WebRTC field trial configurations. |
| `loggingSeverity` | `RTCLoggingSeverity` | `assign` | N/A (Primitive/Enum) | Sets the WebRTC framework logging output verbosity using the `RTCLoggingSeverity` type. |
| `enableMultitaskingCameraAccess` | `BOOL` | `assign` | N/A (Primitive) | Boolean flag that determines whether camera capture remains enabled during iOS multitasking operations. |

---

## How It Works

1. **Singleton Access:** Applications configure global WebRTC behaviors by retrieving the static shared instance via `[WebRTCModuleOptions sharedInstance]`.
2. **Configuration Customization:** Developers can supply custom video codec factories (`videoDecoderFactory`, `videoEncoderFactory`) or a custom audio module (`audioDevice`) prior to initializing WebRTC peer connections or streams.
3. **Runtime Flags:**
   - Setting `fieldTrials` allows supplying WebRTC experiment configuration keys/values.
   - Adjusting `loggingSeverity` controls the depth of WebRTC logging output.
   - Setting `enableMultitaskingCameraAccess` controls camera usage permissions when the application moves between background/foreground state or engages in multitasking modes.