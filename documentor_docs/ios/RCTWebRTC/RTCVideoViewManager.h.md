# Documentation: `RTCVideoViewManager.h`

**File Path:** `ios/RCTWebRTC/RTCVideoViewManager.h`

## Overview

The `RTCVideoViewManager.h` header file defines the interface for `RTCVideoViewManager`, a React Native view manager component within the `react-native-webrtc` iOS codebase. It also exposes the `RTCVideoViewObjectFit` enumeration, which defines how WebRTC video stream content is scaled and rendered inside a view box (mimicking the standard CSS `object-fit` property).

---

## Dependencies & Imports

* **`<Foundation/Foundation.h>`**: Imports the core Objective-C Foundation framework for standard types, data structures, and runtime features.
* **`<React/RCTViewManager.h>`**: Imports React Native's `RCTViewManager` base class, enabling this module to act as a bridge manager for exposing native iOS views to React Native.

---

## Data Types & Enumerations

### `RTCVideoViewObjectFit`

An enumeration (`NS_ENUM`) based on `NSInteger` that defines video aspect ratio scaling behavior inside the rendered view. This matches W3C specifications for HTML5 video elements and CSS `object-fit` rules.

```objc
typedef NS_ENUM(NSInteger, RTCVideoViewObjectFit) {
    RTCVideoViewObjectFitContain = 1,
    RTCVideoViewObjectFitCover
};
```

#### Values:

| Value | Value Index | Description |
| :--- | :--- | :--- |
| `RTCVideoViewObjectFitContain` | `1` | Resizes the video content to maintain its aspect ratio while entirely fitting within the view bounds (may result in letterboxing/pillarboxing). |
| `RTCVideoViewObjectFitCover` | `2` (default increment) | Resizes the video content to maintain its aspect ratio while completely filling the view bounds (may result in clipping/cropping of edges). |

---

## Class Interface

### `RTCVideoViewManager`

```objc
@interface RTCVideoViewManager : RCTViewManager

@end
```

* **Superclass:** `RCTViewManager`
* **Purpose:** Serves as the header interface for the view manager class responsible for creating and managing WebRTC native video views on iOS for React Native.

---

## Component Functionality Summary

1. **Native-to-React Native Bridge Interface**: By extending `RCTViewManager`, `RTCVideoViewManager` integrates into React Native's bridge system to expose video rendering capabilities to JavaScript components.
2. **Standardized Video Scaling**: Exposes `RTCVideoViewObjectFit` to allow consumer code to explicitly control whether video streams are contained (`contain`) or scaled to fill (`cover`) their container dimensions.