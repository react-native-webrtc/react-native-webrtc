# Technical Documentation: `ProcessorProvider.java`

## Overview

The `ProcessorProvider` class is located in the `com.oney.WebRTCModule.videoEffects` package. It serves as a static registry and management container for video frame processor factories (`VideoFrameProcessorFactoryInterface`). 

Its primary purpose is to allow dynamic registration, retrieval, and unregistration of video frame processors mapped to specific string identifiers.

---

## File Details

* **File Path:** `android/src/main/java/com/oney.WebRTCModule/videoEffects/ProcessorProvider.java`
* **Package:** `com.oney.WebRTCModule.videoEffects`
* **Access Modifier:** `public`

---

## Static Fields

### `methodMap`
* **Type:** `Map<String, VideoFrameProcessorFactoryInterface>`
* **Access Modifier:** `private static`
* **Description:** A static `HashMap` instance that stores registered `VideoFrameProcessorFactoryInterface` implementations using a unique `String` key/name identifier.

---

## Public Static Methods

### 1. `getProcessor`

Retrieves a new `VideoFrameProcessor` instance registered under the specified name.

```java
public static VideoFrameProcessor getProcessor(String name)
```

#### Parameters
* **`name`** (`String`): The string identifier of the processor factory to look up.

#### Return Value
* **`VideoFrameProcessor`**: The processor object created by calling `.build()` on the matching `VideoFrameProcessorFactoryInterface` instance.
* Returns `null` if the key `name` does not exist in `methodMap`.

#### Execution Logic
1. Checks if `methodMap` contains the key `name`.
2. If found, retrieves the factory interface associated with `name` and calls its `.build()` method to instantiate and return a `VideoFrameProcessor`.
3. If not found, returns `null`.

---

### 2. `addProcessor`

Registers a processor factory interface with a given name key.

```java
public static void addProcessor(String name, VideoFrameProcessorFactoryInterface videoFrameProcessorFactoryInterface)
```

#### Parameters
* **`name`** (`String`): The name/key used to identify the processor factory. Must not be `null`.
* **`videoFrameProcessorFactoryInterface`** (`VideoFrameProcessorFactoryInterface`): The factory instance responsible for building the processor. Must not be `null`.

#### Exceptions Thrown
* **`NullPointerException`**: Thrown if either `name` or `videoFrameProcessorFactoryInterface` is `null`.
  * Exception Message: `"Name or VideoFrameProcessorFactry can not be null"`

#### Execution Logic
1. Validates that both `name` and `videoFrameProcessorFactoryInterface` are non-null.
2. Inserts the key-value pair into `methodMap`.
3. If either argument is `null`, throws a `NullPointerException`.

---

### 3. `removeProcessor`

Unregisters and removes a processor factory from the registry using its name.

```java
public static void removeProcessor(String name)
```

#### Parameters
* **`name`** (`String`): The name/key of the processor factory to be removed. Must not be `null` and must exist in `methodMap`.

#### Exceptions Thrown
* **`RuntimeException`**: Thrown if `name` is `null` or if `methodMap` does not contain a mapping for `name`.
  * Exception Message: `"VideoFrameProcessorFactry with " + name + " does not exist"`

#### Execution Logic
1. Checks if `name` is non-null and currently present in `methodMap`.
2. If valid, removes the key-value pair associated with `name`.
3. If `name` is `null` or not found in `methodMap`, throws a `RuntimeException`.

---

## Workflow Summary

```
                      +-------------------+
                      |   methodMap       |
                      | (Static HashMap)  |
                      +-------------------+
                                ^
         +----------------------+----------------------+
         |                      |                      |
[ addProcessor() ]      [ removeProcessor() ]    [ getProcessor() ]
         |                      |                      |
 Puts factory interface Removes factory entry   Retrieves factory, calls
 into map by key        from map by key         .build() to return
 (Throws NPE if null)   (Throws RuntimeException VideoFrameProcessor
                        if key missing/null)    (Returns null if key missing)
```