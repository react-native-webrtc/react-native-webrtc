package com.oney.WebRTCModule;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.util.Rational;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// Big shootout for expo-video developers

final public class PictureInPictureUtils {
    static Rect calculateRectHint(View view){
        Rect hint = new Rect();
        view.getGlobalVisibleRect(hint);
        int[] location = new int[2];

        view.getLocationOnScreen(location);

        // getGlobalVisibleRect doesn't take into account the offset for the notch, we use the screen location
        // of the view to calculate the rectHint.
        // We only apply this correction on the y axis due to something that looks like a bug in the Android SDK.
        // If the video screen and home screen have the same orientation this works correctly,
        // but if the home screen doesn't support landscape and the video screen does, we have to
        // ignore the offset for the notch on the x axis even though it's present on the video screen
        // because there will be no offset on the home screen
        // there is no way to check the orientation support of the home screen, so we make the bet that
        // it won't support landscape (as most android home screens do by default)
        // This doesn't have any serious consequences if we are wrong with the guess, the transition will be a bit off though
        int height = hint.bottom - hint.top;
        hint.top = location[1];
        hint.bottom = hint.top + height;
        return hint;
    }

    static void applySourceRectHint(@Nullable Activity activity, View view){
        if (Build.VERSION.SDK_INT >= 31 && isPictureInPictureSupported(activity)) {
            Rect hint = calculateRectHint(view);
            runWithPiPMisconfigurationSoftHandling(()->{
                activity.setPictureInPictureParams(new PictureInPictureParams.Builder().setSourceRectHint(hint).build());
            });
        }
    }

    static void applyAutoEnter(@Nullable Activity activity, Boolean autoEnterPiP){
        if (Build.VERSION.SDK_INT >= 31 && isPictureInPictureSupported(activity)) {
            runWithPiPMisconfigurationSoftHandling(()->{
                activity.setPictureInPictureParams(new PictureInPictureParams.Builder().setAutoEnterEnabled(autoEnterPiP).build());
            });
        }
    }

    public static boolean isPictureInPictureSupported(Activity currentActivity) {
        if(currentActivity == null) return false;
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                currentActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
    }

    static void applyAspectRatio(Activity currentActivity, Rational rational){
        Rational finalAspectRatio = getFinalAspectRatio(rational);
        runWithPiPMisconfigurationSoftHandling(()->{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isPictureInPictureSupported(currentActivity)) {
                currentActivity.setPictureInPictureParams(new PictureInPictureParams.Builder().setAspectRatio(finalAspectRatio).build());
            }
        });
    }

    static void safeEnterPictureInPicture(@Nullable Activity currentActivity){
        runWithPiPMisconfigurationSoftHandling(()->{
            if(!isPictureInPictureSupported(currentActivity)) return;
            currentActivity.enterPictureInPictureMode();
        });
    }

    private static @NonNull Rational getFinalAspectRatio(Rational rational) {
        Rational aspectRatio = rational;

        if(aspectRatio.isNaN()){
            aspectRatio = new Rational(150,200);
        }
        Rational maximumRatio = new Rational(239, 100);
        Rational minimumRatio = new Rational(100, 239);

        if (aspectRatio.floatValue() > maximumRatio.floatValue()) {
            aspectRatio = maximumRatio;
        } else if (aspectRatio.floatValue() < minimumRatio.floatValue()) {
            aspectRatio = minimumRatio;
        }

        return aspectRatio;
    }

    public static void runWithPiPMisconfigurationSoftHandling(@NonNull Runnable block) {
        try {
            block.run();
        } catch (IllegalStateException e) {
            Log.e("com.oney.WebRTCModule", "Current activity does not support picture-in-picture.");
        }
    }
}
