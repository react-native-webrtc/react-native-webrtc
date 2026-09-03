# Technical Documentation: `ReactBridgeUtil.java`

**File Location:** `android/src/main/java/com/oney/WebRTCModule/ReactBridgeUtil.java`  
**Package:** `com.oney.WebRTCModule`

---

## Overview

The `ReactBridgeUtil` class is a utility helper designed to simplify the extraction and conversion of values from React Native bridge data structures (`ReadableMap`) into standard Java `String` representations. 

It provides safe type inspection and extraction to prevent type-mismatch exceptions when bridge arguments are received from the JavaScript environment.

---

## Class Architecture & Dependencies

### Imports
* `com.facebook.react.bridge.ReadableMap`: A React Native interface used to read key-value pairs passed across the JavaScript-to-Native bridge.
* `com.facebook.react.bridge.ReadableType`: An enum defining the data types contained within a `ReadableMap` (e.g., `Boolean`, `Number`, `String`, etc.).

---

## Method Summary

| Modifier and Type | Method | Description |
| :--- | :--- | :--- |
| `public static String` | `getMapStrValue(ReadableMap map, String key)` | Safely retrieves a value associated with a key in a `ReadableMap` and converts it to a `String`. |

---

## Detailed Method Documentation

### `getMapStrValue(ReadableMap map, String key)`

Extracts a value for the specified `key` from the provided `map` and converts it to its `String` equivalent.

#### Signature
```java
public static String getMapStrValue(ReadableMap map, String key)
```

#### Parameters
* **`map`** (`ReadableMap`): The React Native map object containing the source key-value pair.
* **`key`** (`String`): The key whose associated value is to be fetched.

#### Return Value
* **`String`**: 
  * The string representation of the value if the key exists and holds a supported data type (`Boolean`, `Number`, or `String`).
  * `null` if the key does not exist in the map or if the value type is unsupported/unhandled (e.g., `Map`, `Array`, `Null`).

---

## Execution Logic & Workflow

1. **Key Existence Check:**
   Checks if `map.hasKey(key)` is `true`. If the key does not exist, the method immediately returns `null`.

2. **Type Resolution:**
   Determines the type of the value stored under the key using `map.getType(key)`.

3. **Type-based Conversion:**
   Applies a `switch` statement on the returned `ReadableType`:

   * **`ReadableType.Boolean`:**
     * Retrieves the boolean via `map.getBoolean(key)`.
     * Converts to string via `String.valueOf(...)`.
   * **`ReadableType.Number`:**
     * Retrieves the value as a double via `map.getDouble(key)`.
     * Converts to string via `String.valueOf(...)`.
     * *Note on design logic:* `map.getDouble(key)` is used explicitly because React Native's `ReadableType.Number` does not distinguish between integer and floating-point types. Using `getInt()` on a double value causes an exception, whereas `getDouble()` safely reads both integers and doubles.
   * **`ReadableType.String`:**
     * Retrieves the string directly via `map.getString(key)`.
   * **`default` (Unsupported Types):**
     * Returns `null` for types such as `Map`, `Array`, or `Null`.

---

## Type Mapping Reference

| Input `ReadableType` | React Native Retrieval Method | Result |
| :--- | :--- | :--- |
| `Boolean` | `map.getBoolean(key)` | `"true"` or `"false"` |
| `Number` | `map.getDouble(key)` | String representation of the numeric value (e.g., `"123.0"`) |
| `String` | `map.getString(key)` | String value as defined in the map |
| Other/Unmapped Types | N/A | `null` |