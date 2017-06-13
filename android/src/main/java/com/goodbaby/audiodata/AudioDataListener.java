package com.goodbaby.audiodata;

import android.support.annotation.NonNull;

import java.nio.ByteBuffer;

/**
 * Listener for audio data.
 *
 * @author Daniel Cerny
 */

public interface AudioDataListener {

    /**
     * When result from {@link android.media.AudioRecord} comes.
     *
     * Params description taken from original {@link android.media.AudioRecord} javadoc.
     *
     * @param bufferReadResult zero or the positive number of bytes that were read, or one of the following
     *    error codes. The number of bytes will not exceed sizeInBytes and will be truncated to be
     *    a multiple of the frame size.
     * @param audioBuffer the direct buffer to which the recorded audio data is written.
     * Data is written to audioBuffer.position().
     * @param sizeInBytes the number of requested bytes. It is recommended but not enforced
     *    that the number of bytes requested be a multiple of the frame size (sample size in
     *    bytes multiplied by the channel count).
     */
    void onAudioDataChanged(int bufferReadResult, @NonNull ByteBuffer audioBuffer, int sizeInBytes);
}
