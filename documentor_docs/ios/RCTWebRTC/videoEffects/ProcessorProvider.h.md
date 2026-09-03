# Documentation: `ProcessorProvider.h`

**File Path:** `ios/RCTWebRTC/videoEffects/ProcessorProvider.h`

## Overview

The `ProcessorProvider` class provides a static interface for managing and retrieving video frame processors by name. It acts as a centralized registry or lookup interface for objects that conform to the `VideoFrameProcessorDelegate` protocol.

---

## Dependencies

* `#import "VideoFrameProcessor.h"`
  Imports the header file that defines the `VideoFrameProcessorDelegate` protocol required by the class methods.

---

## Interface Definition

* **Class Name:** `ProcessorProvider`
* **Base Class:** `NSObject`

---

## Class Methods

All methods in `ProcessorProvider` are static class methods (`+`).

### 1. `getProcessor:`

```objc
+ (NSObject<VideoFrameProcessorDelegate> *)getProcessor:(NSString *)name;
```

* **Description:** Retrieves a registered video frame processor instance associated with the specified name.
* **Parameters:**
  * `name` (`NSString *`): The string identifier associated with the desired processor.
* **Returns:**
  * `NSObject<VideoFrameProcessorDelegate> *`: An object conforming to the `VideoFrameProcessorDelegate` protocol associated with the given name.

---

### 2. `addProcessor:forName:`

```objc
+ (void)addProcessor:(NSObject<VideoFrameProcessorDelegate> *)processor forName:(NSString *)name;
```

* **Description:** Registers a video frame processor under a specified string identifier.
* **Parameters:**
  * `processor` (`NSObject<VideoFrameProcessorDelegate> *`): The processor instance to register. Must conform to the `VideoFrameProcessorDelegate` protocol.
  * `name` (`NSString *`): The string identifier to associate with the processor.
* **Returns:** `void`

---

### 3. `removeProcessor:`

```objc
+ (void)removeProcessor:(NSString *)name;
```

* **Description:** Unregisters and removes a video frame processor from the registry using its associated name.
* **Parameters:**
  * `name` (`NSString *`): The string identifier of the processor to be removed.
* **Returns:** `void`

---

## Summary of Functionality

`ProcessorProvider.h` defines a static registry interface enabling three core operations:
1. **Adding** a video frame processor mapped to a unique string key (`addProcessor:forName:`).
2. **Retrieving** a video frame processor by its string key (`getProcessor:`).
3. **Removing** a registered video frame processor by its string key (`removeProcessor:`).