# Technical Documentation: `AppDelegate.h`

**File Path:** `examples/GumTestApp_macOS/ios/GumTestApp_macOS/AppDelegate.h`

---

## Overview

The `AppDelegate.h` file is an Objective-C header file that defines the interface for the `AppDelegate` class in the iOS target of the application. It establishes the class hierarchy, protocol conformances, and public properties required to manage the iOS application lifecycle and integrate with the React Native framework.

---

## Key Components

### 1. Framework Imports

```objc
#import <React/RCTBridgeDelegate.h>
#import <UIKit/UIKit.h>
```

* **`<React/RCTBridgeDelegate.h>`**: Imports the definition of the `RCTBridgeDelegate` protocol from the React Native library. This protocol allows the `AppDelegate` to act as a delegate for managing the JavaScript bridge in React Native.
* **`<UIKit/UIKit.h>`**: Imports the core Apple `UIKit` framework, which provides the base infrastructure and classes required for iOS graphical user interfaces and application management.

---

### 2. Class Interface & Protocol Conformance

```objc
@interface AppDelegate : UIResponder <UIApplicationDelegate, RCTBridgeDelegate>
```

* **`AppDelegate`**: The class declaration for the application delegate.
* **`UIResponder`**: The base class from which `AppDelegate` inherits. `UIResponder` provides event-handling capabilities for responder objects in iOS.
* **Protocols Adopted**:
  * **`<UIApplicationDelegate>`**: Adopts the standard UIKit application delegate protocol, which declares methods used to handle application lifecycle events (such as launch, backgrounding, and foregrounding).
  * **`<RCTBridgeDelegate>`**: Adopts the React Native bridge delegate protocol, enabling the app delegate to provide configuration details (such as the JavaScript bundle URL) to the React Native bridge.

---

### 3. Properties

```objc
@property (nonatomic, strong) UIWindow *window;
```

* **`window`**: Declares the main window property for the application.
  * **Type**: `UIWindow *` (a pointer to a UIKit window object that presents views on screen).
  * **Attributes**:
    * `nonatomic`: Specifies that accessors for this property are non-atomic (not thread-safe), which is standard performance practice for UI elements managed on the main thread.
    * `strong`: Specifies a strong reference, ensuring the `AppDelegate` retains ownership of the `UIWindow` instance throughout the application lifecycle.

---

## How It Works

1. **Header Declaration**: This file serves as the interface header (`.h`) for `AppDelegate`. It defines the structure and contract that the implementation file (`AppDelegate.m`) will execute.
2. **Integration Point**: By inheriting from `UIResponder` and conforming to `UIApplicationDelegate`, the class integrates directly into the standard iOS application boot sequence.
3. **React Native Bridge Coupling**: Adopting `RCTBridgeDelegate` allows React Native to communicate back to the app delegate for runtime configuration (such as loading the JavaScript bundle).
4. **UI Hierarchy Root**: Declaring the `window` property gives the iOS runtime and React Native a root container in which to load and render the application's user interface.