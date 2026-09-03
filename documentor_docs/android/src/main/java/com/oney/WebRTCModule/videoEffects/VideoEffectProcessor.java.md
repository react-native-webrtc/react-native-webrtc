# Technical Documentation: `VideoEffectProcessor.java`

**File Path:** `android/src/main/java/com/oney/WebRTCModule/videoEffects/VideoEffectProcessor.java`

---

## 1. Overview

The `VideoEffectProcessor` class is an implementation of WebRTC's `VideoProcessor` interface. Its primary purpose is to act as an intermediary processing pipeline between a video capturer and a downstream `VideoSink`. It receives captured `VideoFrame` objects, sequentially passes them through a chain of `VideoFrameProcessor` instances, and forwards the resulting frame to the configured `VideoSink`.

---

## 2. Class Signature and Dependencies

```java
public class VideoEffectProcessor implements VideoProcessor
```

### Imports
* `org.webrtc.SurfaceTextureHelper`: Utility used to manage texture buffers and GL contexts for frame processing.
* `org.webrtc.VideoFrame`: Represents individual video frames passed through the WebRTC pipeline.
* `org.webrtc.VideoProcessor`: WebRTC interface for processing captured video frames before delivery to a sink.
* `org.webrtc.VideoSink`: WebRTC target interface that receives processed `VideoFrame` objects.
* `java.util.List`: List collection for managing ordered processors.

---

## 3. Fields

| Field Name | Type | Access Modifier | Description |
| :--- | :--- | :--- | :--- |
| `mSink` | `VideoSink` | `private` | The downstream video sink destination where processed frames are sent. |
| `textureHelper` | `SurfaceTextureHelper` | `final private` | Helper instance used to manage GL texture contexts during frame processing. |
| `videoFrameProcessors` | `List<VideoFrameProcessor>` | `final private` | An ordered list of frame processors applied to each incoming frame. |

---

## 4. Constructor

```java
public VideoEffectProcessor(List<VideoFrameProcessor> processors, SurfaceTextureHelper textureHelper)
```

* **Parameters:**
  * `processors`: A `List` of `VideoFrameProcessor` objects that will process frames sequentially.
  * `textureHelper`: A `SurfaceTextureHelper` object passed down to individual processors during frame transformation.
* **Behavior:** Assigns the provided list of processors and texture helper to the class's `final` fields.

---

## 5. Method Reference

### `setSink(VideoSink sink)`

```java
@Override
public void setSink(VideoSink sink)
```
* **Purpose:** Assigns the target downstream `VideoSink` where final video frames will be delivered.
* **Implementation:** Stores the incoming `sink` reference in `mSink`.

---

### `onCapturerStarted(boolean success)`

```java
@Override
public void onCapturerStarted(boolean success) {}
```
* **Purpose:** WebRTC lifecycle callback invoked when the frame capturer starts.
* **Implementation:** No-op (empty implementation).

---

### `onCapturerStopped()`

```java
@Override
public void onCapturerStopped() {}
```
* **Purpose:** WebRTC lifecycle callback invoked when the frame capturer stops.
* **Implementation:** No-op (empty implementation).

---

### `onFrameCaptured(VideoFrame frame)`

```java
@Override
public void onFrameCaptured(VideoFrame frame)
```

* **Purpose:** Receives a captured raw `VideoFrame` from WebRTC, processes it sequentially through all configured `VideoFrameProcessor` objects, and passes the resulting frame to `mSink`.

#### Step-by-Step Processing Logic & Memory Lifecycle

1. **Retain Initial Frame:**
   Calls `frame.retain()` to increase the reference count of the incoming frame, ensuring it is not garbage collected or recycled while processing.

2. **Sequential Frame Processing Loop:**
   Initializes `outputFrame` to the initial `frame`. Iterates through each `VideoFrameProcessor` in `videoFrameProcessors`:
   * Saves `outputFrame` into `inputFrame`.
   * Executes `processor.process(inputFrame, textureHelper)` to produce a new `outputFrame`.
   * Calls `inputFrame.release()` to decrement the reference count on the input frame.
   * **Null Check Fallback:**
     * If `outputFrame == null` (indicating processing failed or was aborted by a processor):
       * Passes the original retained `frame` directly to `mSink.onFrame(frame)`.
       * Immediately returns from the method, skipping remaining processors.

3. **Frame Delivery & Cleanup:**
   * If all processors complete successfully, the final `outputFrame` is passed to `mSink.onFrame(outputFrame)`.
   * Calls `outputFrame.release()` to release the local reference held on the output frame.

---

## 6. Execution Flow Diagram

```text
Incoming raw VideoFrame
       │
       ▼
 frame.retain()
       │
       ▼
 [ Loop through videoFrameProcessors ]
       │
       ├──► processor.process(inputFrame, textureHelper)
       │         │
       │         ├─► outputFrame == null? ──► YES ──► mSink.onFrame(original frame) ──► Return
       │         │
       │         └─► NO
       │              │
       │              ▼
       │      inputFrame.release()
       │              │
       └──────────────┴─► Repeat for next processor
       │
       ▼
 (Loop Complete)
       │
       ▼
 mSink.onFrame(outputFrame)
       │
       ▼
 outputFrame.release()
```