# Documentation: `VideoFrameProcessor.h`

**File Path:** `ios/RCTWebRTC/videoEffects/VideoFrameProcessor.h`

---

## Purpose

The `VideoFrameProcessor.h` header file defines an Objective-C protocol named `VideoFrameProcessorDelegate`. This protocol provides an interface for intercepting, processing, or transforming video frames captured by a WebRTC video capturer (`RTCVideoCapturer`) before they are passed further down the video pipeline.

---

## Dependencies

The file imports two core headers from the WebRTC framework:

*   `<WebRTC/RTCVideoCapturer.h>`: Provides the `RTCVideoCapturer` class, which handles capturing video streams from device hardware or other sources.
*   `<WebRTC/RTCVideoFrame.h>`: Provides the `RTCVideoFrame` class, representing an individual video frame within the WebRTC framework.

---

## Protocol Definition

### `VideoFrameProcessorDelegate`

An Objective-C protocol that delegates must implement to handle and process captured video frames.

```objc
@protocol VideoFrameProcessorDelegate

- (RTCVideoFrame *)capturer:(RTCVideoCapturer *)capturer didCaptureVideoFrame:(RTCVideoFrame *)frame;

@end
```

### Protocol Methods

#### `capturer:didCaptureVideoFrame:`

This method is invoked when an `RTCVideoCapturer` instance captures a new `RTCVideoFrame`.

*   **Signature:**
    ```objc
    - (RTCVideoFrame *)capturer:(RTCVideoCapturer *)capturer didCaptureVideoFrame:(RTCVideoFrame *)frame;
    ```

*   **Parameters:**
    | Parameter | Type | Description |
    | :--- | :--- | :--- |
    | `capturer` | `RTCVideoCapturer *` | The capturer instance that captured the video frame. |
    | `frame` | `RTCVideoFrame *` | The original `RTCVideoFrame` object captured by the capturer. |

*   **Return Value:**
    *   `RTCVideoFrame *`: The processed `RTCVideoFrame` object. Implementations may return the original frame unmodified, a modified version of the input frame, or an entirely new `RTCVideoFrame` instance.

---

## How It Works

1. **Protocol Adoption:** A class adopting `VideoFrameProcessorDelegate` implements the `capturer:didCaptureVideoFrame:` method.
2. **Frame Interception:** When the video capturer captures a frame, it passes the `RTCVideoCapturer` instance and the raw `RTCVideoFrame` to the delegate via `capturer:didCaptureVideoFrame:`.
3. **Frame Processing & Hand-off:** The delegate receives the frame, applies any desired modifications or processing logic, and returns an `RTCVideoFrame` object to be used by the caller.