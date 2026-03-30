package com.oney.WebRTCModule;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

final class ThreadUtils {
    /**
     * Thread which will be used to call all WebRTC PeerConnection APIs. They
     * they don't run on the calling thread anyway, we are deferring the calls
     * to this thread to avoid (potentially) blocking the calling thread.
     */
    private final ExecutorService executor;

    private ThreadUtils() {
        this.executor = Executors.newSingleThreadExecutor();
    }

    public static ThreadUtils create() {
        return new ThreadUtils();
    }

    /**
     * Runs the given {@link Runnable} on the executor.
     * @param runnable
     */
    public void runOnExecutor(Runnable runnable) {
        try {
            executor.execute(runnable);
        } catch (RejectedExecutionException e) {
            // Task submitted after dispose() - silently drop
        }
    }

    /**
     * Submits the given {@link Callable} to be run on the executor.
     * @param callable
     * @return Future.
     */
    public <T> Future<T> submitToExecutor(Callable<T> callable) {
        try {
            return executor.submit(callable);
        } catch (RejectedExecutionException e) {
            CompletableFuture<T> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    /**
     * Submits the given {@link Runnable} to be run on the executor.
     * @param runnable
     * @return Future.
     */
    public Future<?> submitToExecutor(Runnable runnable) {
        try {
            return executor.submit(runnable);
        } catch (RejectedExecutionException e) {
            CompletableFuture<?> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    public void dispose() {
        executor.shutdownNow();
    }
}
