package com.oney.WebRTCModule;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * Preferences for WebRTC module
 */
public class WebRTCModulePreferences {
    public static class Prefs {
        private final boolean hardwareAccelerationEnabled;

        private Prefs(boolean hardwareAccelerationEnabled) {
            this.hardwareAccelerationEnabled = hardwareAccelerationEnabled;
        }

        public boolean isHardwareAccelerationEnabled() {
            return hardwareAccelerationEnabled;
        }
    }

    private WebRTCModulePreferences() {
    }

    public static Prefs get(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(WebRTCModule.TAG, Context.MODE_PRIVATE);

        return new Prefs(
                prefs.getBoolean("hardwareAccelerationEnabled", true)
        );
    }

    @SuppressLint("ApplySharedPref")
    public static void setHardwareAccelerationEnabled(@NonNull Context context,
                                                      boolean hardwareAccelerationEnabled) {
        context.getSharedPreferences(WebRTCModule.TAG, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("hardwareAccelerationEnabled", hardwareAccelerationEnabled)
                .commit();
    }
}
