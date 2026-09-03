# Documentation: `LibraryLoader.java`

**File Path:** `android/src/main/java/com/oney/WebRTCModule/LibraryLoader.java`  
**Package:** `com.oney.WebRTCModule`

---

## Overview

The `LibraryLoader` class provides a custom implementation of WebRTC's `NativeLibraryLoader` interface. 

By default, WebRTC's internal library loader catches and swallows exceptions when a native C/C++ library fails to load. This masks critical errors and makes diagnosing issues from crash logs difficult. `LibraryLoader` addresses this by delegating the loading operation directly to Java's `System.loadLibrary(name)` without catching exceptions. If library loading fails, an uncaught exception is thrown, ensuring that detailed failure information is preserved in the crash log backtrace.

---

## Key Components

### Class Signature

```java
public class LibraryLoader implements NativeLibraryLoader
```

* **Interface Implemented:** `org.webrtc.NativeLibraryLoader`

---

### Fields

| Field | Type | Modifiers | Value | Description |
|---|---|---|---|---|
| `TAG` | `String` | `private static` | `"LibraryLoader"` | Log tag used when issuing debug log messages through WebRTC's logging utility. |

---

### Methods

#### `load(String name)`

```java
@Override
public boolean load(String name)
```

Attempts to load the specified native dynamic library.

* **Parameters:**
  * `name` (`String`): The name of the native library to load (excluding platform-specific prefixes or extensions such as `lib` or `.so`).
* **Returns:** 
  * `boolean`: Returns `true` if the library was successfully loaded.
* **Exceptions:**
  * `UnsatisfiedLinkError` / `SecurityException`: Thrown directly by `System.loadLibrary(name)` if the library fails to load or cannot be found. This class does not catch these exceptions.

---

## How It Works

1. **Log Debug Message:** The `load` method first logs a debug message using `org.webrtc.Logging.d(...)` with the tag `"LibraryLoader"` and message `"Loading library: <name>"`.
2. **Load Native Library:** It executes `System.loadLibrary(name)` to load the requested shared library into memory.
3. **Exception Behavior:** If `System.loadLibrary(name)` encounters an error (e.g., missing `.so` file, architectural mismatch), it throws an exception. Unlike the default WebRTC library loader, `LibraryLoader` allows this exception to propagate up the stack, providing error details in the application backtrace.
4. **Return Success:** If `System.loadLibrary(name)` completes without throwing an exception, the method returns `true`.