package com.oney.WebRTCModule;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

/**
 * Helper class for creating the media projection notification which is used with
 * {@link MediaProjectionService}.
 */
class MediaProjectionNotification {
    private static final String TAG = MediaProjectionNotification.class.getSimpleName();

    static final String ONGOING_CONFERENCE_CHANNEL_ID = "OngoingConferenceChannel";

    static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        if (context == null) {
            Log.d(TAG, " Cannot create notification channel: no current context");
            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Service.NOTIFICATION_SERVICE);

        NotificationChannel channel = notificationManager.getNotificationChannel(ONGOING_CONFERENCE_CHANNEL_ID);

        if (channel != null) {
            // The channel was already created, no need to do it again.
            return;
        }

        channel = new NotificationChannel(ONGOING_CONFERENCE_CHANNEL_ID,
                context.getString(R.string.ongoing_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setShowBadge(false);

        notificationManager.createNotificationChannel(channel);
    }

    static Notification buildMediaProjectionNotification(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ONGOING_CONFERENCE_CHANNEL_ID);

        builder.setCategory(NotificationCompat.CATEGORY_CALL)
                .setContentTitle(context.getString(R.string.media_projection_notification_title))
                .setContentText(context.getString(R.string.media_projection_notification_text))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(false)
                .setUsesChronometer(false)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setSmallIcon(
                        context.getResources().getIdentifier("ic_notification", "drawable", context.getPackageName()))
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);

        return builder.build();
    }
}
