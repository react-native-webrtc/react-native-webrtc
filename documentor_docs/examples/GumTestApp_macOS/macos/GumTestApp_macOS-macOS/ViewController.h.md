# Technical Documentation: `ViewController.h`

**File Path:** `examples/GumTestApp_macOS/macos/GumTestApp_macOS-macOS/ViewController.h`  
**Language:** Objective-C  
**Framework:** Cocoa (AppKit)

---

## Overview

The `ViewController.h` file is an Objective-C header file that defines the interface for the `ViewController` class in the `GumTestApp_macOS` macOS application. It establishes `ViewController` as a custom subclass of `NSViewController`.

---

## Key Components

### 1. Framework Import
```objc
#import <Cocoa/Cocoa.h>
```
* **Purpose:** Imports the Cocoa framework umbrella header, which includes essential macOS application development frameworks such as `AppKit` and `Foundation`. This import provides access to the `NSViewController` base class.

### 2. Class Interface Declaration
```objc
@interface ViewController : NSViewController

@end
```
* **Class Name:** `ViewController`
* **Base Class:** `NSViewController`
* **Description:** Declares the primary public interface for `ViewController`. By inheriting from `NSViewController`, this class acquires standard macOS view management capabilities. 
* **Members:** No custom properties, methods, protocols, or instance variables are explicitly declared in this header file.

---

## Technical Summary

| Component | Identifier | Description |
| :--- | :--- | :--- |
| **Import** | `<Cocoa/Cocoa.h>` | Provides Cocoa UI and foundation definitions. |
| **Interface** | `ViewController` | Custom class interface declaration. |
| **Superclass** | `NSViewController` | Standard macOS AppKit view controller base class. |