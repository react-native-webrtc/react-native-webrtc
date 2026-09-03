# Technical Documentation: `RTCVideoViewManager.java`

## Overview

The `RTCVideoViewManager` class is an Android React Native `SimpleViewManager` implementation located in `com.oney.WebRTCModule`. Its primary purpose is to expose the native `WebRTCView` custom Android view to JavaScript, registering it under the component name **`RTCVideoView`**. 

It manages the lifecycle of `WebRTCView` instances, exposes configurable UI properties via React Native `@ReactProp` annotations, and registers custom direct events (specifically `onDimensionsChange`) to communicate view changes back to React Native.

---

## Class Header

* **Package:** `com.oney.WebRTCModule`
* **Superclass:** `SimpleViewManager<WebRTCView>`

---

## Class Constants

| Constant | Type | Value | Description |
| :--- | :--- | :--- | :--- |
| `REACT_CLASS` | `String` | `"RTCVideoView"` | Identifies the name used to export this native view module to JavaScript. |

---

## Core Methods

### View Lifecycle Methods

#### `getName()`
```java
@Override
public String getName()
```
* **Description:** Returns the component name (`REACT_CLASS`) recognized by React Native's UI manager.
* **Returns:** `"RTCVideoView"`

#### `createViewInstance(ThemedReactContext context)`
```java
@Override
public WebRTCView createViewInstance(ThemedReactContext context)
```
* **Description:** Instantiates and returns a new native `WebRTCView` instance bound to the provided React context.
* **Parameters:** 
  * `context` (`ThemedReactContext`): The React context associated with the view lifecycle.
* **Returns:** A new `WebRTCView` object.

---

### React Native View Properties (`@ReactProp`)

These methods expose native setter methods on `WebRTCView` to JavaScript through prop definitions.

#### `setMirror(WebRTCView view, boolean mirror)`
```java
@ReactProp(name = "mirror")
public void setMirror(WebRTCView view, boolean mirror)
```
* **Prop Name:** `mirror`
* **Description:** Controls whether the video content rendered within the specified `WebRTCView` is horizontally mirrored.
* **Delegates To:** `view.setMirror(mirror)`

#### `setObjectFit(WebRTCView view, String objectFit)`
```java
@ReactProp(name = "objectFit")
public void setObjectFit(WebRTCView view, String objectFit)
```
* **Prop Name:** `objectFit`
* **Description:** Specifies how the video content scales and fits within the bounds of `WebRTCView` (resembling the CSS `object-fit` property).
* **Delegates To:** `view.setObjectFit(objectFit)`

#### `setStreamURL(WebRTCView view, String streamURL)`
```java
@ReactProp(name = "streamURL")
public void setStreamURL(WebRTCView view, String streamURL)
```
* **Prop Name:** `streamURL`
* **Description:** Sets the stream URL string representing the video stream source to display in `WebRTCView`.
* **Delegates To:** `view.setStreamURL(streamURL)`

#### `setZOrder(WebRTCView view, int zOrder)`
```java
@ReactProp(name = "zOrder")
public void setZOrder(WebRTCView view, int zOrder)
```
* **Prop Name:** `zOrder`
* **Description:** Sets the visual rendering z-order stacking layer for the `WebRTCView`.
* **Delegates To:** `view.setZOrder(zOrder)`

#### `setOnDimensionsChange(WebRTCView view, boolean onDimensionsChange)`
```java
@ReactProp(name = "onDimensionsChange")
public void setOnDimensionsChange(WebRTCView view, boolean onDimensionsChange)
```
* **Prop Name:** `onDimensionsChange`
* **Description:** Enables or disables dimension change event tracking on the `WebRTCView`.
* **Delegates To:** `view.setOnDimensionsChange(onDimensionsChange)`

---

### Custom Event Registration

#### `getExportedCustomDirectEventTypeConstants()`
```java
@Override
public Map<String, Object> getExportedCustomDirectEventTypeConstants()
```
* **Description:** Maps direct native events dispatched from the underlying `WebRTCView` to event handler prop names in React Native.
* **Registered Events:**
  * **Event Key:** `"onDimensionsChange"`
  * **Registration Name:** `"onDimensionsChange"`
* **Returns:** A `Map<String, Object>` containing event mapping constants.

---

## Component Integration Summary

```
   React Native JS Layer
          │
          │ Props: mirror, objectFit, streamURL, zOrder, onDimensionsChange
          ▼
┌──────────────────────────────┐
│    RTCVideoViewManager       │ (Extends SimpleViewManager<WebRTCView>)
└──────────────┬───────────────┘
               │ Instantiates & Sets Props
               ▼
┌──────────────────────────────┐
│         WebRTCView           │ (Native Android View Instance)
└──────────────────────────────┘
```