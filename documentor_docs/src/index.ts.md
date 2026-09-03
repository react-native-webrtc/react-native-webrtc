# Documentation Guide: `src/index.ts`

## Overview

The `src/index.ts` file serves as the main entry point for the React Native WebRTC library. Its primary responsibilities are:

1. **Native Module Validation**: Ensuring the native `WebRTCModule` is linked and available before running code.
2. **Library Initialization**: Enabling root-level logging and initializing native event listeners immediately upon import.
3. **Public API Export**: Re-exporting all core classes, components, utility singletons, PIP utilities, and TypeScript types for consumer use.
4. **Global Scope Registration**: Providing a helper function (`registerGlobals`) to attach standard WebRTC classes and `navigator.mediaDevices` methods to the global runtime object.

---

## Code Execution Flow on Import

When `src/index.ts` is imported, the module executes the following initialization steps synchronously:

### 1. Native Module Verification
```typescript
const { WebRTCModule } = NativeModules;

if (WebRTCModule === null) {
    throw new Error(`WebRTC native module not found.\n${Platform.OS === 'ios' ?
        'Try executing the "pod install" command inside your projects ios folder.' :
        'Try executing the "npm install" command inside your projects folder.'
    }`);
}
```
* Checks if `WebRTCModule` exists on `NativeModules`.
* Throws an error if `WebRTCModule` is `null`. The error message provides platform-specific troubleshooting advice:
  * **iOS (`Platform.OS === 'ios'`)**: Suggests running `pod install` in the `ios` directory.
  * **Other platforms**: Suggests running `npm install` in the project root.

### 2. Module Logging Configuration
```typescript
Logger.enable(`${Logger.ROOT_PREFIX}:*`);
```
* Configures the `Logger` module by enabling logs under the prefix specified by `${Logger.ROOT_PREFIX}:*`.

### 3. Native Listener Setup
```typescript
setupNativeEvents();
```
* Invokes `setupNativeEvents()` early to begin listening for native asynchronous events right when the module is loaded.

---

## Exported Members

The module re-exports a comprehensive set of WebRTC entities, split into classes/components, objects/singletons, functions, and TypeScript types:

### Core Classes & UI Components
* **`RTCIceCandidate`**: Standard WebRTC ICE candidate representation.
* **`RTCPeerConnection`**: Core WebRTC peer connection interface.
* **`RTCSessionDescription`**: SDP session description interface.
* **`RTCCertificate`**: WebRTC security certificate representation.
* **`RTCView`**: React Native component for rendering WebRTC video streams.
* **`RTCPIPView`**: React Native component for Picture-in-Picture rendering.
* **`ScreenCapturePickerView`**: Native view component used for screen capture selection.
* **`RTCRtpEncodingParameters`**: Parameters defining RTP encoding configuration.
* **`RTCRtpTransceiver`**: Interface for RTP transceivers.
* **`RTCRtpReceiver`**: Interface for RTP receivers.
* **`RTCRtpSender`**: Interface for RTP senders.
* **`RTCRtpSendParameters`**: Parameters controlling RTP sending options.
* **`RTCErrorEvent`**: Event class representing WebRTC-related errors.
* **`RTCAudioSession`**: Handler for managing native audio sessions.
* **`MediaStream`**: Interface representing media streams.
* **`MediaStreamTrack`**: Interface representing individual media tracks.

### Singletons & Utilities
* **`mediaDevices`**: Singleton instance providing media device operations (e.g., capture and enumeration).
* **`permissions`**: Utility for handling permissions.

### Helper Functions
* **`registerGlobals`**: Utility function to bind WebRTC APIs to the global context (detailed below).
* **`startIOSPIP`**: Function to trigger Picture-in-Picture mode on iOS.
* **`stopIOSPIP`**: Function to stop Picture-in-Picture mode on iOS.

### Exported Types
* **`MediaTrackSettings`**: Type definition for media track settings.
* **`RTCRtpEncodingParametersInit`**: Initial configuration type for RTP encoding parameters.
* **`RTCRtpSendParametersInit`**: Initial configuration type for RTP send parameters.
* **`RTCVideoViewProps`**: Props type for the `RTCView` component.
* **`RTCIOSPIPOptions`**: Options type for configuring iOS Picture-in-Picture.

---

## Global Registration (`registerGlobals`)

The file defines and exports the `registerGlobals` function. This function mimics browser standard WebRTC implementations by binding library constructs to the global JavaScript scope (`global`).

### Signature
```typescript
function registerGlobals(): void
```

### Behavior & Logic

1. **Navigator Check**:
   Verifies that `global.navigator` exists as an object.
   * If `typeof global.navigator !== 'object'`, it throws an error: `'navigator is not an object'`.

2. **Media Devices Initialization**:
   Ensures `global.navigator.mediaDevices` exists:
   ```typescript
   if (!global.navigator.mediaDevices) {
       global.navigator.mediaDevices = {};
   }
   ```

3. **Media Device Methods Binding**:
   Binds the following methods from the `mediaDevices` singleton to `global.navigator.mediaDevices`:
   * `getUserMedia`
   * `getDisplayMedia`
   * `enumerateDevices`

4. **Class Assignments to `global`**:
   Assigns the following WebRTC constructs directly to the `global` object:
   * `global.RTCIceCandidate`
   * `global.RTCCertificate`
   * `global.RTCPeerConnection`
   * `global.RTCSessionDescription`
   * `global.MediaStream`
   * `global.MediaStreamTrack`
   * `global.MediaStreamTrackEvent`
   * `global.RTCRtpTransceiver`
   * `global.RTCRtpReceiver`
   * `global.RTCRtpSender`
   * `global.RTCErrorEvent`