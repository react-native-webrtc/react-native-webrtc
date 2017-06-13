package com.goodbaby.media;

import android.annotation.SuppressLint;
import android.media.AudioRecord;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.Log;

import com.goodbaby.audiodata.MicrophoneObserver;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;

/**
 * Re-sends data from read method using observer.
 *
 * @author Daniel Cerny
 */
@SuppressLint("Unused")
public class SharedAudioRecord extends AudioRecord {

    private static final String TAG = "SharedAudioRecord";

    /**
     * ReadMode interface is hidden at {@link AudioRecord} class.
     *
     * But constants are reused, because they are public (luckily).
     */
    @IntDef({
            READ_BLOCKING,
            READ_NON_BLOCKING
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ReadMode {
    }

    public SharedAudioRecord(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes) throws IllegalArgumentException {
        super(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
        Log.d(TAG, "<ctor>");
    }

    @Override
    public int read(@NonNull ByteBuffer audioBuffer, int sizeInBytes) {
        final int bufferReadResult = super.read(audioBuffer, sizeInBytes);
        onRead(bufferReadResult, audioBuffer.duplicate(), sizeInBytes);
        return bufferReadResult;
    }

    @Override
    public int read(@NonNull ByteBuffer audioBuffer, int sizeInBytes, @ReadMode int readMode) {
        final int bufferReadResult = super.read(audioBuffer, sizeInBytes, readMode);
        onRead(bufferReadResult, audioBuffer.duplicate(), sizeInBytes);
        return bufferReadResult;
    }

    /**
     * Calls after WebRtcAudioRecorder reads from microphone.
     *
     * @param bufferReadResult The result of the read procedure.
     * @param byteBuffer       The buffer.
     * @param sizeInBytes      Size of the buffer.
     */
    private void onRead(int bufferReadResult, @NonNull ByteBuffer byteBuffer, int sizeInBytes) {
        MicrophoneObserver.getInstance().dispatchOnAudioDataChanged(bufferReadResult, byteBuffer, sizeInBytes);
    }
}
