package com.oney.WebRTCModule;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

final class ThreadUtils {
    /**
     * Thread which will be used to call all WebRTC PeerConnection APIs. They
     * they don't run on the calling thread anyway, we are deferring the calls
     * to this thread to avoid (potentially) blocking the calling thread.
     */
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Runs the given {@link Runnable} on the executor.
     * @param runnable
     */
    public static void runOnExecutor(Runnable runnable) {
        executor.execute(runnable);
    }

    /**
     * Submits the given {@link Callable} to be run on the executor.
     * @param callable
     * @return Future.
     */
    public static <T> Future<T> submitToExecutor(Callable<T> callable) {
        return executor.submit(callable);
    }

    /**
     * Submits the given {@link Runnable} to be run on the executor.
     * @param runnable
     * @return Future.
     */
    public static Future<?> submitToExecutor(Runnable runnable) {
        return executor.submit(runnable);
    }
}
