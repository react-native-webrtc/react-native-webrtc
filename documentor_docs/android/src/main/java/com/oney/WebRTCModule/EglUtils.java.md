# Technical Documentation: `EglUtils.java`

**File Path:** `android/src/main/java/com/oney/WebRTCModule/EglUtils.java`  
**Package:** `com.oney.WebRTCModule`  

---

## 1. Overview

`EglUtils` is a utility class designed to manage and provide a shared root Embedded-System Graphics Library (`EglBase`) instance for WebRTC operations on Android. 

By maintaining a single static `EglBase` context shared across the entire application, `EglUtils` minimizes hardware resource consumption (specifically EGL contexts) and provides standard fallback logic across different Android API levels and hardware capabilities.

---

## 2. Class Architecture & Member Variables

### Fields

*   `private static EglBase rootEglBase`
    *   **Type:** `org.webrtc.EglBase`
    *   **Access:** `private static`
    *   **Description:** Holds the singleton instance of the application's root `EglBase`. It is lazily initialized upon the first invocation of `getRootEglBase()`.

---

## 3. Method Documentation

### `public static synchronized EglBase getRootEglBase()`

Lazily initializes and returns the application-wide root `EglBase` object. Thread safety is ensured via the `synchronized` modifier.

#### Initialization & Fallback Logic
1. **Null Check:** If `rootEglBase` is already initialized, it is returned immediately.
2. **Configuration Attributes:** Uses `EglBase.CONFIG_PLAIN` as the default EGL configuration attributes.
3. **EGL14 Attempt:**
   * Checks if `android.os.Build.VERSION.SDK_INT >= 18`.
   * If true, attempts to instantiate an EGL14 context via `EglBase.createEgl14(configAttributes)`.
   * Catches any `RuntimeException` if EGL14 initialization fails (e.g., if EGL14 is supported by the OS level but fails to find a matching EGL configuration).
4. **EGL10 Fallback Attempt:**
   * If `eglBase` remains `null` (due to SDK < 18 or an exception during EGL14 creation), attempts to fall back to an EGL10 context using `EglBase.createEgl10(configAttributes)`.
   * Catches any `RuntimeException` if EGL10 creation fails.
5. **Error Logging & Assignment:**
   * If an exception was caught during creation (`cause != null`), logs the error using `Log.e(EglUtils.class.getName(), "Failed to create EglBase", cause)`.
   * If creation succeeds (`cause == null`), sets `rootEglBase = eglBase`.
6. **Return:** Returns `rootEglBase` (which will be `null` if both initialization attempts failed).

---

### `public static EglBase.Context getRootEglBaseContext()`

A convenience wrapper method to retrieve the `EglBase.Context` from the root `EglBase` instance.

#### Behavior
1. Calls `getRootEglBase()` to obtain the shared `EglBase` instance.
2. Evaluates whether the returned `EglBase` is `null`:
   * **If `null`:** Returns `null`.
   * **If non-null:** Calls and returns `eglBase.getEglBaseContext()`.

---

## 4. Initialization Execution Flow

```
+-------------------------------------------------------+
|             getRootEglBaseContext() Called            |
+-------------------------------------------------------+
                           |
                           v
+-------------------------------------------------------+
|               getRootEglBase() Called                 |
+-------------------------------------------------------+
                           |
                 Is rootEglBase null?
                 /                  \
              (Yes)                 (No)
                |                     |
                v                     v
     Check API Level >= 18?      Return rootEglBase
        /               \
     (Yes)              (No)
       |                  |
Attempt EGL14 creation    |
       |                  |
  Failed/Passed           |
       \                  /
        v                v
   Is eglBase still null?
        /               \
     (Yes)              (No)
       |                  |
Attempt EGL10 creation    |
       |                  |
  Failed/Passed           |
       \                  /
        v                v
     Did any step throw RuntimeException?
        /               \
     (Yes)              (No)
       |                  |
   Log error      Assign rootEglBase = eglBase
       \                  /
        v                v
          Return rootEglBase
```

---

## 5. Summary of Key Dependencies

* **`org.webrtc.EglBase`**: WebRTC interface providing context management for rendering and video processing pipelines.
* **`android.os.Build.VERSION`**: Used for runtime Android SDK version checks to determine API 18 (`JELLY_BEAN_MR2`) compatibility for EGL14.
* **`android.util.Log`**: Handles logging when an `EglBase` instance fails to instantiate.