# Technical Documentation: `DisplayUtils.java`

**File Path:** `android/src/main/java/com/oney/WebRTCModule/DisplayUtils.java`  
**Package:** `com.oney.WebRTCModule`

---

## Overview

`DisplayUtils` is a utility class within the `react-native-webrtc` Android module. It provides a static helper method to retrieve the real display metrics (physical size and density information) of the device screen associated with a given `Activity`.

---

## Class Definition

```java
public class DisplayUtils
```

`DisplayUtils` contains no instance state or instance methods. It exposes a single static public utility method.

---

## Key Methods

### `getDisplayMetrics(Activity activity)`

Retrieves the real physical display metrics for the device screen using the provided `Activity`.

#### Signature
```java
public static DisplayMetrics getDisplayMetrics(Activity activity)
```

#### Parameters
| Parameter | Type | Description |
| :--- | :--- | :--- |
| `activity` | `android.app.Activity` | The `Activity` instance used to access the application context and system services. |

#### Return Value
* **Type:** `android.util.DisplayMetrics`
* **Description:** An object populated with the real display metrics (e.g., width, height, density, DPI) of the default screen.

---

## How It Works

1. **Metrics Object Initialization:**  
   Instantiates a new, empty `DisplayMetrics` object:
   ```java
   DisplayMetrics displayMetrics = new DisplayMetrics();
   ```

2. **Retrieve `WindowManager`:**  
   Retrieves the system-level `WindowManager` service via the application context of the passed `Activity`:
   ```java
   WindowManager windowManager =
           (WindowManager) activity.getApplication().getSystemService(Context.WINDOW_SERVICE);
   ```

3. **Fetch Real Metrics:**  
   Gets the default display from the `WindowManager` and calls `getRealMetrics()`, passing the `DisplayMetrics` instance to populate it with the raw, physical display dimensions (including system decor areas like status bars or navigation bars):
   ```java
   windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
   ```

4. **Return Results:**  
   Returns the populated `DisplayMetrics` object to the caller.

---

## Dependencies

The class relies on the following Android framework imports:

* `android.app.Activity`: Passed in as a parameter to access application-level resources.
* `android.content.Context`: Used to access the constant key `Context.WINDOW_SERVICE`.
* `android.util.DisplayMetrics`: Data structure used to describe general display information (size, density, fonts).
* `android.view.WindowManager`: System interface used to interact with the device screen display.