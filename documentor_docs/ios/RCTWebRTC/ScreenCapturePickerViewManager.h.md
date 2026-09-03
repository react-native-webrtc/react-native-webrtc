# Technical Documentation: `ScreenCapturePickerViewManager.h`

## Overview

The `ScreenCapturePickerViewManager.h` file is an Objective-C header file within the `react-native-webrtc` iOS native codebase. Its primary purpose is to declare the `ScreenCapturePickerViewManager` interface, which extends React Native's `RCTViewManager`. 

This class acts as a bridge header for managing a native iOS screen capture picker view within a React Native application.

---

## Availability Requirements

* **Target Platform:** iOS 12.0+ (`API_AVAILABLE(ios(12))`)
* The `API_AVAILABLE(ios(12))` attribute explicitly marks this component as restricted to devices running iOS 12 or later.

---

## Key Components

### 1. Header Imports
```objc
#import <Foundation/Foundation.h>
#import <React/RCTViewManager.h>
```
* **`<Foundation/Foundation.h>`**: Imports the standard Cocoa Foundation framework providing core Objective-C data structures and types.
* **`<React/RCTViewManager.h>`**: Imports the base React Native view manager class. Subclassing `RCTViewManager` allows React Native to bridge, instantiate, and render custom native iOS UI views in JavaScript.

### 2. Class Interface Declaration
```objc
API_AVAILABLE(ios(12))
@interface ScreenCapturePickerViewManager : RCTViewManager

@end
```
* **`ScreenCapturePickerViewManager`**: A class derived from `RCTViewManager`.
* **Public Interface:** This header file declares no additional public properties, instance variables, or public methods. All internal logic, view instantiation, and property bridging are handled in the corresponding implementation (`.m`) file.

---

## How It Works

1. **Native Module Registration:** By inheriting from `RCTViewManager`, React Native recognizes `ScreenCapturePickerViewManager` as a manager for a native view component.
2. **Platform Guard:** The `API_AVAILABLE(ios(12))` attribute ensures the compiler and runtime enforce that this interface is only referenced or instantiated on iOS 12 and above.
3. **Encapsulation:** The header exposes only the interface declaration to other Objective-C modules, keeping all execution logic encapsulated within the implementation file.