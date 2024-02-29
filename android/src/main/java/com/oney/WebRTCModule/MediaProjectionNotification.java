package com.oney.WebRTCModule;

import static com.oney.WebRTCModule.NotificationUtils.ONGOING_CONFERENCE_CHANNEL_ID;

import android.app.Notification;
import android.content.Context;

import androidx.core.app.NotificationCompat;

import android.util.Log;


/**
 * Helper class for creating the media projection notification which is used with
 * {@link MediaProjectionService}.
 */
class MediaProjectionNotification {
    private static final String TAG = MediaProjectionNotification.class.getSimpleName();

    static Notification buildMediaProjectionNotification(Context context) {

        if (context == null) {
            Log.d(TAG, " Cannot create notification: no current context");
            return null;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ONGOING_CONFERENCE_CHANNEL_ID);

        builder
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentTitle(context.getString(R.string.media_projection_notification_title))
            .setContentText(context.getString(R.string.media_projection_notification_text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .setUsesChronometer(false)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setSmallIcon(context.getResources().getIdentifier("", "drawable", context.getPackageName()))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);

        return builder.build();
    }
}
