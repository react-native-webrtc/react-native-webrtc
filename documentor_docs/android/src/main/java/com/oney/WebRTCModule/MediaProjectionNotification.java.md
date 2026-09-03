# Technical Documentation: `MediaProjectionNotification.java`

## Overview

The `MediaProjectionNotification` class is a package-private utility class within the `com.oney.WebRTCModule` package. Its primary purpose is to manage the Android system notification required for media projection operations (such as screen capture/sharing) used in conjunction with `MediaProjectionService`.

It provides static helper methods to:
1. Create a dedicated Android Notification Channel (for Android 8.0 / API level 26 and higher).
2. Construct a `Notification` instance configured for media projection foreground service requirements.

---

## Class Information

* **Package:** `com.oney.WebRTCModule`
* **Access Modifier:** Package-private (`class MediaProjectionNotification`)
* **Dependencies:**
  * `android.app.Notification`
  * `android.app.NotificationChannel`
  * `android.app.NotificationManager`
  * `android.app.Service`
  * `android.content.Context`
  * `android.os.Build`
  * `android.util.Log`
  * `androidx.core.app.NotificationCompat`

---

## Constants

### `ONGOING_CONFERENCE_CHANNEL_ID`
* **Type:** `static final String`
* **Value:** `"OngoingConferenceChannel"`
* **Description:** Identifies the notification channel used for media projection and ongoing calls/conferences.

### `TAG`
* **Type:** `private static final String`
* **Value:** `MediaProjectionNotification.class.getSimpleName()`
* **Description:** Used as the log tag for logging output within this class.

---

## Methods

### `createNotificationChannel(Context context)`

Creates the Android Notification Channel required to display notifications on Android 8.0 (API level 26 / `Build.VERSION_CODES.O`) and above.

#### Signature
```java
static void createNotificationChannel(Context context)
```

#### Detailed Logic Execution
1. **API Level Check:** Checks if `Build.VERSION.SDK_INT` is less than `Build.VERSION_CODES.O`. If true, the method returns early since notification channels are not supported or required on earlier Android versions.
2. **Context Validation:** Checks if the provided `context` is `null`. If `null`, it logs a debug message (`Cannot create notification channel: no current context`) and exits.
3. **NotificationManager Retrieval:** Obtains the system `NotificationManager` service via `context.getSystemService(Service.NOTIFICATION_SERVICE)`.
4. **Existing Channel Check:** Queries whether `ONGOING_CONFERENCE_CHANNEL_ID` already exists on the system using `notificationManager.getNotificationChannel(...)`. If it exists, execution stops to prevent re-creation.
5. **Channel Instantiation and Configuration:**
   * Creates a new `NotificationChannel` with ID `ONGOING_CONFERENCE_CHANNEL_ID`, name pulled from resource `R.string.ongoing_notification_channel_name`, and importance set to `NotificationManager.IMPORTANCE_DEFAULT`.
   * Disables notification lights: `channel.enableLights(false)`
   * Disables vibration: `channel.enableVibration(false)`
   * Disables badge icon: `channel.setShowBadge(false)`
6. **Registration:** Registers the configured channel with the Android OS using `notificationManager.createNotificationChannel(channel)`.

---

### `buildMediaProjectionNotification(Context context)`

Constructs and builds a `Notification` object formatted for ongoing media projection operations.

#### Signature
```java
static Notification buildMediaProjectionNotification(Context context)
```

#### Returns
* `Notification`: The constructed Android notification object.

#### Notification Configuration Properties
The method configures a `NotificationCompat.Builder` targeted at `ONGOING_CONFERENCE_CHANNEL_ID` with the following explicit parameters:

| Parameter / Builder Method | Value / Setting | Description |
| :--- | :--- | :--- |
| `setCategory` | `NotificationCompat.CATEGORY_CALL` | Identifies the notification category as an active call. |
| `setContentTitle` | `context.getString(R.string.media_projection_notification_title)` | Title text resolved from string resources. |
| `setContentText` | `context.getString(R.string.media_projection_notification_text)` | Body text resolved from string resources. |
| `setPriority` | `NotificationCompat.PRIORITY_LOW` | Notification priority level. |
| `setOngoing` | `false` | Sets ongoing status flag to false. |
| `setUsesChronometer` | `false` | Disables displaying a timer/chronometer. |
| `setAutoCancel` | `true` | Allows notification to be automatically dismissed when tapped. |
| `setVisibility` | `NotificationCompat.VISIBILITY_PUBLIC` | Shows full notification content on all lockscreens. |
| `setOnlyAlertOnce` | `true` | Prevents repeating sound/vibration alerts upon updates. |
| `setSmallIcon` | Icon resolved dynamically via `context.getResources().getIdentifier("ic_notification", "drawable", context.getPackageName())` | Dynamically resolves the resource ID for the icon drawable named `"ic_notification"`. |
| `setForegroundServiceBehavior` | `NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE` | Forces immediate display when bound to a foreground service. |

---

## Required Application Resources

This class relies on the following application resources being present in the project:

* **Strings:**
  * `R.string.ongoing_notification_channel_name`: Name displayed to the user in Android system channel settings.
  * `R.string.media_projection_notification_title`: Title text displayed in the notification.
  * `R.string.media_projection_notification_text`: Body content text displayed in the notification.
* **Drawables:**
  * A drawable resource named `"ic_notification"` within the application package (`context.getPackageName()`).