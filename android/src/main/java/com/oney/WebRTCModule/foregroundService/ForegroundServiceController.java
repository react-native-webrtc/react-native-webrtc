package com.oney.WebRTCModule.foregroundService;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.oney.WebRTCModule.voip.VoipForegroundRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ForegroundServiceController {
    private static final String TAG = ForegroundServiceController.class.getSimpleName();

    private static ForegroundServiceController instance;

    private ReactApplicationContext reactContext;
    private Context appContext;

    private boolean cameraRequested = false;
    private boolean microphoneRequested = false;
    private boolean screenSharingAllowed = false;
    private boolean screenShareActive = false;

    private VoipForegroundRequest voipRequest = VoipForegroundRequest.INACTIVE;

    private String channelId = "com.fishjam.foregroundservice.channel";
    private String channelName = "Fishjam Notifications";
    private String notificationTitle = "[PLACEHOLDER] Tap to return to the call.";
    private String notificationContent = "[PLACEHOLDER] Your video call is ongoing";
    private String importance = "high";
    private boolean onlyAlertOnce = false;

    private volatile CompletableFuture<Void> foregroundedFuture;

    private ForegroundServiceController() {}

    public static synchronized ForegroundServiceController getInstance() {
        if (instance == null) {
            instance = new ForegroundServiceController();
        }
        return instance;
    }

    public void setContext(ReactApplicationContext reactContext) {
        this.reactContext = reactContext;
        this.appContext = reactContext.getApplicationContext();
    }

    // Called by WebRTCForegroundService after startForeground() completes.
    public void onServiceForegrounded() {
        if (this.foregroundedFuture != null) {
            this.foregroundedFuture.complete(null);
        }
    }

    public synchronized void start(ReadableMap config, Promise promise) {
        cameraRequested = config.hasKey("enableCamera") && config.getBoolean("enableCamera");
        microphoneRequested = config.hasKey("enableMicrophone") && config.getBoolean("enableMicrophone");
        screenSharingAllowed = config.hasKey("enableScreenSharing") && config.getBoolean("enableScreenSharing");

        if (config.hasKey("channelId")) channelId = config.getString("channelId");
        if (config.hasKey("channelName")) channelName = config.getString("channelName");
        if (config.hasKey("notificationTitle")) notificationTitle = config.getString("notificationTitle");
        if (config.hasKey("notificationContent")) notificationContent = config.getString("notificationContent");
        if (config.hasKey("importance")) importance = config.getString("importance");
        if (config.hasKey("onlyAlertOnce")) onlyAlertOnce = config.getBoolean("onlyAlertOnce");

        applyState();
        promise.resolve(null);
    }

    public synchronized void stop(Promise promise) {
        cameraRequested = false;
        microphoneRequested = false;
        screenSharingAllowed = false;
        screenShareActive = false;
        applyState();
        promise.resolve(null);
    }

    // Called from GetUserMediaImpl after the user grants screen capture consent.
    public void onScreenShareStarted(Context context) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        synchronized (this) {
            screenShareActive = true;
            foregroundedFuture = future;
            applyState();
        }

        // Block until WebRTCForegroundService.startForeground() completes (or timeout).
        // This prevents createScreenStream() from calling getMediaProjection() before the
        // mediaProjection FGS type is active.
        try {
            future.get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.w(TAG, "Timed out waiting for service to foreground with mediaProjection type");
        }
    }

    // Called from ScreenCaptureController.dispose() when screen sharing stops.
    public synchronized void onScreenShareStopped(Context context) {
        screenShareActive = false;
        applyState();
    }

    public synchronized void setVoipRequest(VoipForegroundRequest request) {
        voipRequest = request;
        applyState();
    }

    public synchronized void setVoipHeld(boolean held) {
        if (!voipRequest.isActive()) return;
        voipRequest = voipRequest.withHeld(held);
        applyState();
    }

    public synchronized VoipForegroundRequest getVoipRequest() {
        return voipRequest;
    }

    private void applyState() {
        Context context = appContext != null ? appContext : reactContext;
        if (context == null) return;

        boolean screenShareNeedsService = screenSharingAllowed && screenShareActive;
        boolean cameraNeeded = cameraRequested || voipRequest.needsCamera();
        boolean microphoneNeeded = microphoneRequested || voipRequest.needsMicrophone();
        int[] types = buildForegroundServiceTypes(cameraNeeded, microphoneNeeded, screenShareNeedsService);

        if (types.length == 0 && !screenShareNeedsService) {
            Intent serviceIntent = new Intent(context, WebRTCForegroundService.class);
            context.stopService(serviceIntent);
            return;
        }

        Intent serviceIntent = new Intent(context, WebRTCForegroundService.class);
        serviceIntent.putExtra("channelId", channelId);
        serviceIntent.putExtra("channelName", channelName);
        serviceIntent.putExtra("notificationTitle", notificationTitle);
        serviceIntent.putExtra("notificationContent", notificationContent);
        serviceIntent.putExtra("importance", importance);
        serviceIntent.putExtra("onlyAlertOnce", onlyAlertOnce);
        serviceIntent.putExtra("foregroundServiceTypes", types);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to start foreground service", e);
            CompletableFuture<Void> f = foregroundedFuture;
            if (f != null && !f.isDone()) {
                f.complete(null);
            }
        }
    }

    private int[] buildForegroundServiceTypes(
            boolean enableCamera, boolean enableMicrophone, boolean enableScreenSharing) {
        List<Integer> serviceTypes = new ArrayList<>();

        if (enableCamera && hasPermission(Manifest.permission.CAMERA)) {
            serviceTypes.add(ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA);
        }
        if (enableMicrophone && hasPermission(Manifest.permission.RECORD_AUDIO)) {
            serviceTypes.add(ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        }
        if (enableScreenSharing && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            serviceTypes.add(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        }

        int[] result = new int[serviceTypes.size()];
        for (int i = 0; i < serviceTypes.size(); i++) {
            result[i] = serviceTypes.get(i);
        }
        return result;
    }

    private boolean hasPermission(String permission) {
        Context context = appContext != null ? appContext : reactContext;
        return context != null
                && ContextCompat.checkSelfPermission(context, permission)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }
}
