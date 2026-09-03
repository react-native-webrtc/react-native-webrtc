# Technical Documentation: `ThreadUtils.java`

**File Path:** `android/src/main/java/com/oney/WebRTCModule/ThreadUtils.java`  
**Package:** `com.oney.WebRTCModule`  
**Access Modifier:** Package-private (`final class ThreadUtils`)

---

## 1. Overview and Purpose

`ThreadUtils` is a package-private utility class designed to manage thread execution for WebRTC operations within the Android native module. 

Calling WebRTC `PeerConnection` APIs directly on main/UI threads or JavaScript threads can lead to thread-blocking issues. `ThreadUtils` addresses this by maintaining a dedicated, single-threaded execution queue (`ExecutorService`). Tasks submitted to this class are executed sequentially off the calling thread, preventing UI freezing or non-responsive application behavior.

---

## 2. Architecture and Class Signature

```java
package com.oney.WebRTCModule;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

final class ThreadUtils {
    // Class implementation
}
```

* **`final` Modifier:** The class cannot be extended or inherited.
* **Package-Private Access:** The class is not marked `public`, restricting its instantiation and direct usage strictly to classes within the `com.oney.WebRTCModule` package.

---

## 3. Fields

### `executor`
```java
private static final ExecutorService executor = Executors.newSingleThreadExecutor();
```
* **Type:** `ExecutorService`
* **Access:** `private static final`
* **Description:** A thread-safe, single-threaded executor initialized at class-loading time via `Executors.newSingleThreadExecutor()`. It guarantees that all submitted tasks execute sequentially on a dedicated background thread in the order they are received.

---

## 4. Methods

### 4.1 `getExecutor()`
```java
public static ExecutorService getExecutor()
```
* **Description:** Returns the shared static `ExecutorService` instance.
* **Parameters:** None.
* **Return Value:** `ExecutorService` — The internal single-thread executor instance.

---

### 4.2 `runOnExecutor(Runnable runnable)`
```java
public static void runOnExecutor(Runnable runnable)
```
* **Description:** Executes a `Runnable` task asynchronously on the background thread without returning a tracking result.
* **Parameters:**
  * `runnable` (`Runnable`): The unit of work to be executed on the single background thread.
* **Return Value:** `void`
* **Implementation:** Invokes `executor.execute(runnable)`.

---

### 4.3 `submitToExecutor(Callable<T> callable)`
```java
public static <T> Future<T> submitToExecutor(Callable<T> callable)
```
* **Description:** Submits a value-returning task (`Callable`) for execution on the background thread and returns a `Future` representing the pending result of the task.
* **Type Parameters:** `<T>` — The return type of the callable task.
* **Parameters:**
  * `callable` (`Callable<T>`): The task to execute that produces a result or throws an exception.
* **Return Value:** `Future<T>` — A handle to monitor completion or retrieve the result of type `T`.
* **Implementation:** Invokes `executor.submit(callable)`.

---

### 4.4 `submitToExecutor(Runnable runnable)`
```java
public static Future<?> submitToExecutor(Runnable runnable)
```
* **Description:** Submits a `Runnable` task for execution on the background thread and returns a `Future` object representing the task. This allows the caller to check completion or wait for execution finish using `.get()`.
* **Parameters:**
  * `runnable` (`Runnable`): The task to execute.
* **Return Value:** `Future<?>` — A handle to monitor or wait for task completion (returns `null` upon successful completion when calling `.get()`).
* **Implementation:** Invokes `executor.submit(runnable)`.

---

## 5. How It Works

1. **Initialization:** When `ThreadUtils` is loaded into memory, `Executors.newSingleThreadExecutor()` constructs a single background thread with an unbounded queue.
2. **Task Queuing:** 
   * Calling `runOnExecutor()` pushes a task into the queue for fire-and-forget execution via `execute()`.
   * Calling `submitToExecutor()` pushes a `Callable` or `Runnable` task into the queue via `submit()`, returning a `Future` handle to the caller.
3. **Execution Guarantee:** All tasks dispatched to `ThreadUtils` are processed one at a time, strictly sequentially, on the underlying worker thread managed by `executor`.