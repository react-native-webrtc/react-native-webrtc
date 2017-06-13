package com.goodbaby.audiodata;

import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Microphone audio data observer.
 *
 * @author Daniel Cerny
 */

public class MicrophoneObserver implements AudioDataObserver {

    private static MicrophoneObserver sInstance = null;

    private List<WeakReference<AudioDataListener>> mWeakRefs = new ArrayList<>();

    /**
     * Prevents instantiation.
     */
    private MicrophoneObserver() {
    }

    /**
     * Singleton.
     *
     * @return Returns {@link MicrophoneObserver} instance.
     */
    public static MicrophoneObserver getInstance() {
        if (sInstance == null) {
            sInstance = new MicrophoneObserver();
        }
        return sInstance;
    }

    @Override
    public void registerListener(AudioDataListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (mWeakRefs) {
            if (!isRegistered(listener)) {
                mWeakRefs.add(new WeakReference(listener));
            }
        }
    }

    @Override
    public void unregisterListener(AudioDataListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (mWeakRefs) {
            removeListener(listener);
        }
    }

    /**
     * Checks if listener is already registered.
     *
     * @param listener The listener.
     * @return True if registered, false otherwise.
     */
    private boolean isRegistered(AudioDataListener listener) {
        for(WeakReference weakRef : mWeakRefs) {
            if(weakRef.get() == listener) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the listener.
     *
     * @param listener The listener.
     */
    private void removeListener(AudioDataListener listener) {
        WeakReference toRemove = null;
        for(WeakReference weakRef : mWeakRefs) {
            if(weakRef.get() == listener) {
                toRemove = weakRef;
            }
        }
        if(toRemove != null) {
            mWeakRefs.remove(toRemove);
        }
    }

    /**
     * Removes all dead listeners.
     *
     * Removes weak references that returns null on {@link WeakReference#get()}).
     */
    private void removeDeadListeners() {
        List<WeakReference> toRemove = new ArrayList<>();
        for(WeakReference weakRef : mWeakRefs) {
            if(weakRef.get() == null) {
                toRemove.add(weakRef);
            }
        }
        mWeakRefs.removeAll(toRemove);
    }

    /**
     * Returns all registered listeners in a thread safe manner.
     *
     * @return All previously registered listeners.
     * @see #registerListener(AudioDataListener)
     * @see #unregisterListener(AudioDataListener)
     */
    private List<WeakReference<AudioDataListener>> getListenersThreadSafe() {
        synchronized (mWeakRefs) {
            removeDeadListeners();
            return new ArrayList<>(mWeakRefs);
        }
    }

    @Override
    public void dispatchOnAudioDataChanged(int bufferReadResult, @NonNull ByteBuffer byteBuffer, int sizeInBytes) {
        final List<WeakReference<AudioDataListener>> weakRefs = getListenersThreadSafe();
        for (WeakReference<AudioDataListener> weakRef : weakRefs) {
            AudioDataListener listener = weakRef.get();
            if (listener != null) {
                listener.onAudioDataChanged(bufferReadResult, byteBuffer, sizeInBytes);
            }
        }
    }

}
