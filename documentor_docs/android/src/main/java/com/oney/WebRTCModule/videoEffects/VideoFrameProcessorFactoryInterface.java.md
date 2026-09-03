# Technical Documentation: `VideoFrameProcessorFactoryInterface.java`

## Overview

The `VideoFrameProcessorFactoryInterface` is a Java interface defined within the `com.oney.WebRTCModule.videoEffects` package. It follows the Factory Design Pattern to establish a standard contract for instantiating objects that implement the `VideoFrameProcessor` interface.

## Package Location

```
android/src/main/java/com/oney/WebRTCModule/videoEffects/VideoFrameProcessorFactoryInterface.java
```

## Purpose

The primary purpose of `VideoFrameProcessorFactoryInterface` is to define a factory method for dynamically creating instances of `VideoFrameProcessor`. This abstracts the instantiation logic of video frame processors from the caller, allowing different factory implementations to construct frame processors as needed.

---

## Interface Definition

```java
package com.oney.WebRTCModule.videoEffects;

/**
 * Factory for creating VideoFrameProcessor instances.
 */
public interface VideoFrameProcessorFactoryInterface {
    /**
     * Dynamically allocates a VideoFrameProcessor instance and returns a pointer to it.
     * The caller takes ownership of the object.
     */
    public VideoFrameProcessor build();
}
```

---

## Key Components

### Methods

#### `build()`

*   **Signature:** `public VideoFrameProcessor build()`
*   **Description:** Dynamically allocates and initializes a new `VideoFrameProcessor` instance and returns it to the caller.
*   **Return Type:** `VideoFrameProcessor` — A new instance of a video frame processor.
*   **Ownership Semantics:** The calling code takes ownership of the returned `VideoFrameProcessor` instance.

---

## How It Works

1. **Abstraction:** Any concrete class implementing `VideoFrameProcessorFactoryInterface` must provide a concrete implementation of the `build()` method.
2. **Instantiation:** When `build()` is invoked on a factory implementation, it constructs and returns a `VideoFrameProcessor` object.
3. **Caller Responsibilities:** The caller receiving the `VideoFrameProcessor` instance assumes ownership of that instance for its lifecycle management.