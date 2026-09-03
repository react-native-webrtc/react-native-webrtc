# Documentation Guide: `MediaProjectionService.java`

## Overview

The `MediaProjectionService` class is an Android foreground `Service` within the `com.oney.WebRTCModule` package. Its primary purpose is to present an ongoing foreground notification while media projection or screen capturing is in progress. Running as a foreground service prevents the Android OS from killing the application process while it operates in the background.

---

## Key Components & Properties

### Fields

* **`TAG`** (`private static final String`):
  Stores the simple name of the class (`MediaProjectionService.class.getSimpleName()`) used for Android logging (`Log.w`, `Log.i`).

* **`NOTIFICATION_ID`** (`static final int`):
  A randomly generated integer identifier calculated as `new Random().nextInt(99999) + 10000` (range: 10,000 to 109,998). It uniquely identifies the persistent foreground notification for this service instance.

* **`startFuture`** (`private static volatile CompletableFuture<Void>`):
  A volatile static `CompletableFuture` reference that tracks the asynchronous startup process of the service. It allows the caller invoking `launch(...)` to wait until `onStartCommand` successfully executes.

---

## Method Specifications

### Static Controller Methods

#### `public static CompletableFuture<Void> launch(Context context)`
Starts the `MediaProjectionService` as a foreground service asynchronously.

* **Behavior**:
  1. Checks if `WebRTCModuleOptions.getInstance().enableMediaProjectionService` is enabled. If `false`, returns an immediately completed `CompletableFuture` with a `null` result.
  2. Creates a new `CompletableFuture<Void>` and assigns it to `startFuture`.
  3. Invokes `MediaProjectionNotification.createNotificationChannel(context)` to ensure the required notification channel exists on the device.
  4. Initiates the service execution:
     * **Android O (API 26) and above**: Calls `context.startForegroundService(intent)`.
     * **Below Android O**: Calls `context.startService(intent)`.
  5. **Exception Handling**:
     * Catches `RuntimeException` (such as `ForegroundServiceStartNotAllowedException` on Android 12 / API 31+). On exception, logs a warning, resets `startFuture` to `null`, completes the future exceptionally with the caught error, and returns the future.
     * Checks if the returned `ComponentName` is `null`. If `null`, resets `startFuture` to `null`, completes the future exceptionally with a `RuntimeException("Media projection service not started")`, and returns the future.
  6. Returns the `CompletableFuture<Void>` which will be completed when `onStartCommand` executes.

#### `public static void abort(Context context)`
Stops the running `MediaProjectionService`.

* **Behavior**:
  1. Checks `WebRTCModuleOptions.getInstance().enableMediaProjectionService`. If `false`, returns immediately without performing any action.
  2. Creates an `Intent` targeting `MediaProjectionService` and calls `context.stopService(intent)`.

---

### Service Lifecycle Methods

#### `@Override public IBinder onBind(Intent intent)`
* **Returns**: `null`
* **Behavior**: Indicates that this is an unbound service. Binding is not supported.

#### `@Override public int onStartCommand(Intent intent, int flags, int startId)`
Triggered when the service is started via an intent.

* **Behavior**:
  1. Calls `MediaProjectionNotification.buildMediaProjectionNotification(this)` to construct the `Notification` object.
  2. Promotes the service to a foreground service using `startForeground(...)`:
     * **Android Q (API 29) and above**: Calls `startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)`.
     * **Below Android Q**: Calls `startForeground(NOTIFICATION_ID, notification)`.
  3. Checks `startFuture`:
     * If `startFuture` is non-null, clears the static reference (`startFuture = null`) and resolves the future by calling `fut.complete(null)`.
  4. **Returns**: `START_NOT_STICKY`. If the OS terminates the service after `onStartCommand` completes, the service will not be automatically recreated unless explicit start intents are pending.

---

## Workflow & Execution Lifecycle

```
Caller                           MediaProjectionService                 MediaProjectionNotification
  |                                         |                                         |
  |--- launch(context) -------------------->|                                         |
  |                                         |--- createNotificationChannel(context) ->|
  |                                         |                                         |
  |                                         |--- startForegroundService / startService
  |                                         |
  |<-- returns CompletableFuture -----------|
  |                                         |
  |                                   [OS Starts Service]
  |                                         |
  |                                         |--- onStartCommand(...)
  |                                         |      |
  |                                         |      |-- buildMediaProjectionNotification ->|
  |                                         |      |<-- returns Notification -------------|
  |                                         |      |
  |                                         |      |-- startForeground(...)
  |                                         |      |
  |<-- CompletableFuture.complete(null) ----|      |
  |                                                |-- returns START_NOT_STICKY
  |                                 
  |--- abort(context) --------------------->|
  |                                         |--- stopService(...)
```

---

## Android Version Compatibility Logic

* **Notification Channel & Foreground Service Initiation**:
  * **Android O (API 26+)**: Uses `context.startForegroundService(intent)`.
  * **Pre-Android O**: Uses `context.startService(intent)`.

* **Foreground Service Types**:
  * **Android Q (API 29+)**: Explicitly passes `ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION` to `startForeground()`.
  * **Pre-Android Q**: Calls standard `startForeground(NOTIFICATION_ID, notification)` without type flags.

* **Foreground Service Restrictions**:
  * Wraps `startForegroundService()` in a try-catch block for `RuntimeException` to handle background execution restrictions introduced in API level 31 (`ForegroundServiceStartNotAllowedException`).