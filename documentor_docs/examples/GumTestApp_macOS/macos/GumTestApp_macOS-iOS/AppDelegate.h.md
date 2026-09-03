# Technical Documentation: `AppDelegate.h`

**File Path:** `examples/GumTestApp_macOS/macos/GumTestApp_macOS-iOS/AppDelegate.h`

---

## Overview

The `AppDelegate.h` file is an Objective-C header file that defines the interface for the `AppDelegate` class in the iOS target of the `GumTestApp_macOS` application. It declares the class's inheritance, the protocols it adopts to manage the application lifecycle and integrate with React Native, and its primary UI window property.

---

## Key Components

### 1. Framework & Module Imports

```objc
#import <React/RCTBridgeDelegate.h>
#import <UIKit/UIKit.h>
```

* **`<React/RCTBridgeDelegate.h>`**: Imports the definition for the `RCTBridgeDelegate` protocol from the React Native framework. This protocol allows the class to act as a delegate for configuring and controlling the React Native JavaScript bridge.
* **`<UIKit/UIKit.h>`**: Imports the standard iOS UI framework, providing access to essential iOS classes and protocols such as `UIResponder`, `UIApplicationDelegate`, and `UIWindow`.

---

### 2. Class Declaration & Protocols

```objc
@interface AppDelegate : UIResponder <UIApplicationDelegate, RCTBridgeDelegate>
```

* **Class Name:** `AppDelegate`
* **Superclass:** `UIResponder` — The base class for objects that receive and handle events in an iOS application.
* **Adopted Protocols:**
  * **`UIApplicationDelegate`**: Defines methods for responding to application lifecycle events (such as launch, active states, and termination) managed by iOS.
  * **`RCTBridgeDelegate`**: Defines methods required to configure the React Native bridge (such as specifying the source URL for the JavaScript bundle).

---

### 3. Class Properties

```objc
@property (nonatomic, strong) UIWindow *window;
```

* **`window`**: 
  * **Type:** `UIWindow *`
  * **Attributes:** `nonatomic`, `strong`
  * **Purpose:** Represents the main window of the iOS application. This property is required by the `UIApplicationDelegate` protocol to manage the visual canvas and display the app's root view controller.

---

## Summary of Usage

This header file serves as the public interface contract for the iOS application delegate (`AppDelegate`). It specifies that `AppDelegate` handles iOS application lifecycle events through `UIApplicationDelegate`, interfaces with the React Native framework via `RCTBridgeDelegate`, and owns a reference to the main `UIWindow`. Implementation details for these protocols are defined in the corresponding implementation file (`AppDelegate.m`).