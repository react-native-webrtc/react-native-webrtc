package com.oney.WebRTCModule.foregroundService;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.oney.WebRTCModule.voip.CallNotificationManager;
import com.oney.WebRTCModule.voip.VoipForegroundRequest;

public class WebRTCForegroundService extends Service {
    private static final int FOREGROUND_SERVICE_ID = CallNotificationManager.NOTIFICATION_ID;

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public WebRTCForegroundService getService() {
            return WebRTCForegroundService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        restartService(intent);
        return START_NOT_STICKY;
    }

    public void restartService(Intent intent) {
        if (intent == null) {
            return;
        }

        int foregroundServiceType = 0;
        int[] foregroundServiceTypesArray = intent.getIntArrayExtra("foregroundServiceTypes");
        if (foregroundServiceTypesArray != null) {
            for (int value : foregroundServiceTypesArray) {
                foregroundServiceType |= value;
            }
        }

        // Call mode: an active Core-Telecom call owns the notification slot.
        // Post the ongoing CallStyle notification through startForeground()
        // (FGS-attached is what makes it valid on Android 14+ without a
        // full-screen intent) instead of the generic room notification.
        VoipForegroundRequest voipRequest = ForegroundServiceController.getInstance().getVoipRequest();
        if (voipRequest.isActive() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CallNotificationManager callNotificationManager = new CallNotificationManager();
            Notification notification;
            if (voipRequest.isConnecting()) {
                notification = callNotificationManager.buildConnecting(this, voipRequest.getDisplayName());
            } else if (voipRequest.isHeld()) {
                notification = callNotificationManager.buildHeld(
                        this, voipRequest.getDisplayName(), voipRequest.getConnectedAtMs());
            } else {
                notification = callNotificationManager.buildOngoing(
                        this, voipRequest.getDisplayName(), voipRequest.getConnectedAtMs());
            }
            startForegroundWithNotification(notification, foregroundServiceType);
            return;
        }

        String channelId = intent.getStringExtra("channelId");
        String channelName = intent.getStringExtra("channelName");
        String notificationTitle = intent.getStringExtra("notificationTitle");
        String notificationContent = intent.getStringExtra("notificationContent");
        String importance = intent.getStringExtra("importance");
        boolean onlyAlertOnce = intent.getBooleanExtra("onlyAlertOnce", false);
        if (importance == null) {
            importance = "high";
        }

        if (channelId == null || channelName == null || notificationTitle == null || notificationContent == null) {
            return;
        }

        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        PendingIntent pendingIntent = launchIntent == null
                ? null
                : PendingIntent.getActivity(
                        this, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, channelId)
                                            .setContentTitle(notificationTitle)
                                            .setContentText(notificationContent)
                                            .setContentIntent(pendingIntent)
                                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                                            .setPriority(builderPriorityFor(importance))
                                            .setOnlyAlertOnce(onlyAlertOnce)
                                            .build();

        createNotificationChannel(channelId, channelName, importance);
        startForegroundWithNotification(notification, foregroundServiceType);
    }

    private static int channelImportanceFor(String importance) {
        return "low".equals(importance) ? NotificationManager.IMPORTANCE_LOW : NotificationManager.IMPORTANCE_HIGH;
    }

    private static int builderPriorityFor(String importance) {
        return "low".equals(importance) ? NotificationCompat.PRIORITY_LOW : NotificationCompat.PRIORITY_HIGH;
    }

    private void startForegroundWithNotification(Notification notification, int foregroundServiceType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(FOREGROUND_SERVICE_ID, notification, foregroundServiceType);
        } else {
            startForeground(FOREGROUND_SERVICE_ID, notification);
        }
        ForegroundServiceController.getInstance().onServiceForegrounded();
    }

    private void createNotificationChannel(String channelId, String channelName, String importance) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel serviceChannel =
                new NotificationChannel(channelId, channelName, channelImportanceFor(importance));
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }
}
