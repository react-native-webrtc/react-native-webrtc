package com.oney.WebRTCModule;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class ThreadUtils {
    /**
     * Thread which will be used to call all WebRTC PeerConnection APIs. They
     * they don't run on the calling thread anyway, we are deferring the calls
     * to this thread to avoid (potentially) blocking the calling thread.
     */
    private static final ExecutorService executor
        = Executors.newSingleThreadExecutor();

    /**
     * Runs the given {@link Runnable} on the executor.
     * @param runnable
     */
    public static void runOnExecutor(Runnable runnable) {
        executor.execute(runnable);
    }

    /**
     * Runs the given {@link Runnable} on the executor and synchronously waits for it to finish.
     * @param runnable
     */
    public  static <T> T runOnExecutorAndWait(Callable<T> callable) throws ExecutionException, InterruptedException {
        return executor.submit(callable).get();
    }
}
