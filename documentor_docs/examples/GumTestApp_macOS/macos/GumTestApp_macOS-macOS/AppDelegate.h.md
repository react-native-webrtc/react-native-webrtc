# Technical Documentation: `AppDelegate.h`

**File Path:** `examples/GumTestApp_macOS/macos/GumTestApp_macOS-macOS/AppDelegate.h`

---

## Overview

The `AppDelegate.h` file is an Objective-C header file for the macOS application delegate class (`AppDelegate`). It defines the public interface and contract for the main application delegate in a React Native macOS project environment.

---

## Key Components

### 1. Framework Imports
```objc
#import <Cocoa/Cocoa.h>
```
* **`<Cocoa/Cocoa.h>`**: Imports the core macOS Cocoa framework, providing essential base classes and interfaces required for building native macOS applications, including `NSObject` and `NSApplicationDelegate`.

---

### 2. Forward Declarations
```objc
@class RCTBridge;
```
* **`@class RCTBridge;`**: Forward-declares the `RCTBridge` class (the core React Native bridge). Using `@class` avoids importing the entire React Native header file (`<React/RCTBridge.h>`) in this header file, which optimizes compilation time and prevents circular dependency issues.

---

### 3. Class Interface & Protocol Conformance
```objc
@interface AppDelegate : NSObject <NSApplicationDelegate>
```
* **`AppDelegate`**: The class name for the main application delegate.
* **`NSObject`**: The root base class for Objective-C objects.
* **`<NSApplicationDelegate>`**: A protocol declaration indicating that `AppDelegate` handles application lifecycle events (such as launch, termination, and window management) dispatched by macOS (`NSApplication`).

---

### 4. Properties
```objc
@property (nonatomic, readonly) RCTBridge *bridge;
```
Declares a single public property on `AppDelegate`:

* **`bridge`**: A pointer to an `RCTBridge` instance.
* **Attributes**:
  * `nonatomic`: Accessors generated for this property are not thread-safe, offering better performance in standard single-threaded property access contexts.
  * `readonly`: Restricts public access to getter operations only. The property cannot be directly overwritten or reassigned from outside this class.

---

## Summary of Structure

| Element | Identifier | Description |
| :--- | :--- | :--- |
| **Import** | `<Cocoa/Cocoa.h>` | Provides foundation and app kit capabilities for macOS. |
| **Forward Class** | `RCTBridge` | Represents the React Native bridge instance class. |
| **Class** | `AppDelegate` | Inherits from `NSObject`, acts as the main application delegate. |
| **Protocol** | `NSApplicationDelegate` | Handles macOS system/application lifecycle events. |
| **Property** | `bridge` | A `nonatomic`, `readonly` reference to an `RCTBridge` object. |