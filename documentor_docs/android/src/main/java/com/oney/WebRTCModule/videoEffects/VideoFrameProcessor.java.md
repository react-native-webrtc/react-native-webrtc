# Technical Documentation: `VideoFrameProcessor.java`

## Overview

**File Path:** `android/src/main/java/com/oney/WebRTCModule/videoEffects/VideoFrameProcessor.java`  
**Package:** `com.oney.WebRTCModule.videoEffects`  
**Type:** `Interface`

`VideoFrameProcessor` is a Java interface designed to define a contract for applying image processing algorithms to WebRTC video frames (`VideoFrame`). Classes implementing this interface take an incoming video frame, perform custom video processing operations, and return a processed frame for rendering.

---

## Interface Summary

```java
public interface VideoFrameProcessor
```

### Purpose
Provides a standard method contract (`process`) for intercepts and transforms raw video frames before they are rendered or sent downstream.

---

## Method Details

### `process`

```java
public VideoFrame process(VideoFrame frame, SurfaceTextureHelper textureHelper);
```

#### Description
Applies image processing logic to an incoming `VideoFrame` and returns the resulting `VideoFrame`. 

#### Parameters

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `frame` | `org.webrtc.VideoFrame` | The raw input video frame that requires processing. |
| `textureHelper` | `org.webrtc.SurfaceTextureHelper` | A WebRTC helper instance used for surface texture creation, management, or GL context operations during frame processing. |

#### Return Value
* **Type:** `org.webrtc.VideoFrame`
* **Description:** The processed video frame ready to be rendered or passed further through the WebRTC pipeline.

---

## Memory & Resource Management Rules

The interface specifies explicit resource ownership rules via Javadoc annotations:

1. **Object Ownership:** The caller takes ownership of the returned `VideoFrame` object.
2. **Frame Release Responsibility:** The caller is responsible for releasing the reference to the returned `VideoFrame` once rendering or downstream processing is complete.

---

## Dependencies

This interface depends directly on the WebRTC library types:
* `org.webrtc.VideoFrame`: Represents a single video frame in WebRTC.
* `org.webrtc.SurfaceTextureHelper`: Manages OpenGL contexts and texture utilities for WebRTC frames.