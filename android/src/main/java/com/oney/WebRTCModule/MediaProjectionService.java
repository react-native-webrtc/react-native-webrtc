package com.oney.WebRTCModule;

import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.util.Random;


/**
 * This class implements an Android {@link Service}, a foreground one specifically, and it's
 * responsible for presenting an ongoing notification when a conference is in progress.
 * The service will help keep the app running while in the background.
 *
 * See: https://developer.android.com/guide/components/services
 */
public class MediaProjectionService extends Service {
    private static final String TAG = MediaProjectionService.class.getSimpleName();

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public MediaProjectionService getService() {
            // Return this instance of MediaProjectionService so clients can call public methods
            return MediaProjectionService.this;
        }
    }

    public static void launch(Activity activity, ServiceConnection serviceConnection) {
        if (!WebRTCModuleOptions.getInstance().enableMediaProjectionService) {
            return;
        }

        MediaProjectionNotification.createNotificationChannel(activity);
        Intent intent = new Intent(activity, MediaProjectionService.class);
        activity.bindService(intent, serviceConnection, Context.BIND_IMPORTANT);
        ComponentName componentName;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                componentName = activity.startForegroundService(intent);
            } else {
                componentName = activity.startService(intent);
            }
        } catch (RuntimeException e) {
            // Avoid crashing due to ForegroundServiceStartNotAllowedException (API level 31).
            // See: https://developer.android.com/guide/components/foreground-services#background-start-restrictions
            Log.w(TAG, "Media projection service not started", e);
            return;
        }

        if (componentName == null) {
            Log.w(TAG, "Media projection service not started");
        } else {
            Log.i(TAG, "Media projection service started");
        }
    }

    public static void abort(Context context) {
        if (!WebRTCModuleOptions.getInstance().enableMediaProjectionService) {
            return;
        }

        Intent intent = new Intent(context, MediaProjectionService.class);
        context.stopService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Notification notification = MediaProjectionNotification.buildMediaProjectionNotification(this);

        final Random random = new Random();
        final int id = random.nextInt(Integer.MAX_VALUE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(id, notification);
        }

        return START_NOT_STICKY;
    }
}
