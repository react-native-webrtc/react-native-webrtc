# Technical Documentation: `WebRTCModulePackage.java`

## Overview

The `WebRTCModulePackage` class serves as the entry point for registering the native WebRTC module and its corresponding custom view manager within the React Native Android framework. By implementing React Native's `ReactPackage` interface, this class exposes native Java functionality to the JavaScript environment.

- **File Path:** `android/src/main/java/com/oney/WebRTCModule/WebRTCModulePackage.java`
- **Package:** `com.oney.WebRTCModule`

---

## Class Definition

```java
public class WebRTCModulePackage implements ReactPackage
```

The class implements `com.facebook.react.ReactPackage`, requiring it to provide implementations for registering native modules and UI view managers with the React Native bridge.

---

## Method Documentation

### 1. `createNativeModules`

Registers native modules that expose Java APIs to JavaScript.

```java
@Override
public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
    return Arrays.<NativeModule>asList(new WebRTCModule(reactContext));
}
```

* **Parameters:**
  * `reactContext` (`ReactApplicationContext`): The React Native application context, providing access to application-level operations and bridge communication.
* **Returns:**
  * `List<NativeModule>`: A list containing an instance of `WebRTCModule` initialized with the given `reactContext`.
* **Purpose:** Exposes the non-UI native business logic defined in `WebRTCModule` to React Native.

---

### 2. `createViewManagers`

Registers custom UI components (View Managers) that manage native Android views rendered in React Native.

```java
@Override
public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
    return Arrays.<ViewManager>asList(new RTCVideoViewManager());
}
```

* **Parameters:**
  * `reactContext` (`ReactApplicationContext`): The React Native application context.
* **Returns:**
  * `List<ViewManager>`: A list containing an instance of `RTCVideoViewManager`.
* **Purpose:** Registers the `RTCVideoViewManager` class, enabling React Native to instantiate and render video view UI elements natively on Android.

---

## Dependencies & Imports

| Import Class | Source Package | Purpose |
| :--- | :--- | :--- |
| `ReactPackage` | `com.facebook.react` | Interface for bundling Native Modules and View Managers. |
| `NativeModule` | `com.facebook.react.bridge` | Base interface for native modules exposed to JS. |
| `ReactApplicationContext` | `com.facebook.react.bridge` | Context object for React Native application state. |
| `ViewManager` | `com.facebook.react.uimanager` | Base class responsible for managing native views in JS. |
| `Arrays` | `java.util` | Helper utility to construct fixed-size lists. |
| `List` | `java.util` | Standard Java list interface returned by package methods. |

---

## Summary of Operations

When the React Native host application initializes native packages:
1. React Native invokes `createNativeModules(...)`, which instantiates `WebRTCModule` and returns it inside a list to register native methods for JavaScript bridge calls.
2. React Native invokes `createViewManagers(...)`, which instantiates `RTCVideoViewManager` and returns it inside a list to register native video rendering views for JavaScript UI components.