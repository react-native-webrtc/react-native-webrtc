package com.oney.WebRTCModule;

import android.util.Log;
import android.os.Build.VERSION;

import org.webrtc.EglBase;

public class EglUtils {
    /**
     * The root {@link EglBase} instance shared by the entire application for
     * the sake of reducing the utilization of system resources (such as EGL
     * contexts). It selects between {@link EglBase10} and {@link EglBase14}
     * by performing a runtime check.
     */
    private static EglBase rootEglBase;

    /**
     * Lazily creates and returns the one and only {@link EglBase} which will
     * serve as the root for all contexts that are needed.
     */
    public static synchronized EglBase getRootEglBase() {
        if (rootEglBase == null) {
            // XXX EglBase14 will report that isEGL14Supported() but its
            // getEglConfig() will fail with a RuntimeException with message
            // "Unable to find any matching EGL config". Fall back to EglBase10
            // in the described scenario.
            EglBase eglBase = null;
            int[] configAttributes = EglBase.CONFIG_PLAIN;
            RuntimeException cause = null;

            try {
                // WebRTC internally does this check in isEGL14Supported, but it's no longer exposed
                // in the public API
                if (VERSION.SDK_INT >= 18) {
                    eglBase = EglBase.createEgl14(configAttributes);
                }
            } catch (RuntimeException ex) {
                // Fall back to EglBase10.
                cause = ex;
            }

            if (eglBase == null) {
                try {
                    eglBase = EglBase.createEgl10(configAttributes);
                } catch (RuntimeException ex) {
                    // Neither EglBase14, nor EglBase10 succeeded to initialize.
                    cause = ex;
                }
            }

            if (cause != null) {
                Log.e(EglUtils.class.getName(), "Failed to create EglBase", cause);
            } else {
                rootEglBase = eglBase;
            }
        }

        return rootEglBase;
    }

    public static EglBase.Context getRootEglBaseContext() {
        EglBase eglBase = getRootEglBase();

        return eglBase == null ? null : eglBase.getEglBaseContext();
    }
}
