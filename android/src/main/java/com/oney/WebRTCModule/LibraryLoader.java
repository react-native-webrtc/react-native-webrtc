package com.oney.WebRTCModule;

import org.webrtc.Logging;
import org.webrtc.NativeLibraryLoader;

/**
 * Custom library loader. WebRTC's default library loader swallows errors, which
 * makes debugging impossible if all we have is a crash log. Let it throw since
 * the app is going to crash anyway. This way we'll have the information in the
 * backtrace.
 */
public class LibraryLoader implements NativeLibraryLoader {
    private static String TAG = "LibraryLoader";

    @Override
    public boolean load(String name) {
        Logging.d(TAG, "Loading library: " + name);
        System.loadLibrary(name);
        return true;
    }
}
