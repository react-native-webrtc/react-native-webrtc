/*
 *  Copyright (c) 2015 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc.voiceengine;

import org.webrtc.Logging;
import org.webrtc.ThreadUtils;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaRecorder.AudioSource;
import android.os.Process;
import android.support.annotation.Nullable;

import com.goodbaby.media.SharedAudioRecord;

import java.lang.System;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 *  Modified WebRTC audio recorder (originally distributed as a part of libjingle library).
 *
 *  Should be used by Child station part of BabyCam application to keep audio recording working in situations where
 *  the recording is terminated for some reason.
 *
 *  The goal is to have audio data all the time the Child camera is running to be able to analyze the audio stream,
 *  because when Parent station part goes offline, WebRTC internally cuts Child's part microphone.
 *  We need fresh audio data when Child part camera is running to have a chance to analyze the sound.
 *  For example: We will notify other dead clients when child is crying using push notification.
 *
 *  Differences compared to original implementation of {@link WebRtcAudioRecord}:
 *
 *  - When {@link #startRecording} is called, {@link AudioRecordThread} starts normally when called first time.
 *  {@link #shouldPublishData} is set to true at this time (@{link .shouldPublishData} field described next time).
 *
 *  - When {@link #stopRecording} is called, {@link AudioRecordThread} is kept running, only {@link #shouldPublishData}
 *  is changed to false. {@link #shouldPublishData} acts as a switch to not propagate new audio data record to the
 *  underlying C++ part. They use {@linbk .nativeDataIsRecorded} method to propagate new the audio data to the C++.
 *
 *  - When our custom {@link #stopRecordingInternal()} is called {@link AudioRecordThread} should be stopped. This should not be
 *  called from WebRTC library - only application will call to interrupt audio recording. For example: Will stop when
 *  Child part (child camera view) is not running anymore.
 *
 *  Notice: {@link #startRecording} and {@link #stopRecording} Java methods are called directly through JNI by C++ layer
 *  of WebRTC library.
 *
 *  Full list of methods that could be called from C++ code (taken from "audio_record_jni.h" file at WebRTC repo).
 *  <pre>
 *      int InitRecording(int sample_rate, size_t channels);
 *      bool StartRecording();
 *      bool StopRecording();
 *      bool EnableBuiltInAEC(bool enable);
 *      bool EnableBuiltInNS(bool enable);
 *  </pre>
 *
 *  @author Daniel Cerny
 */
public class WebRtcAudioRecord {

  private static WebRtcAudioRecord instance;

  private static final boolean DEBUG = false;

  private static final String TAG = "WebRtcAudioRecordModified";

  // Default audio data format is PCM 16 bit per sample.
  // Guaranteed to be supported by all devices.
  private static final int BITS_PER_SAMPLE = 16;

  // Requested size of each recorded buffer provided to the client.
  private static final int CALLBACK_BUFFER_SIZE_MS = 10;

  // Average number of callbacks per second.
  private static final int BUFFERS_PER_SECOND = 1000 / CALLBACK_BUFFER_SIZE_MS;

  // We ask for a native buffer size of BUFFER_SIZE_FACTOR * (minimum required
  // buffer size). The extra space is allocated to guard against glitches under
  // high load.
  private static final int BUFFER_SIZE_FACTOR = 2;

  // The AudioRecordJavaThread is allowed to wait for successful call to join()
  // but the wait times out afther this amount of time.
  private static final long AUDIO_RECORD_THREAD_JOIN_TIMEOUT_MS = 2000;

  private final long nativeAudioRecord;
  private final Context context;

  private WebRtcAudioEffects effects = null;

  private ByteBuffer byteBuffer;

  private SharedAudioRecord audioRecord = null;
  private AudioRecordThread audioThread = null;

  private static volatile boolean microphoneMute = false;
  private byte[] emptyBytes;

  /**
   * Switch for propagation of fresh recorded data to the C++ layer.
   *
   * Recorded data is propagated by native method {@link #nativeDataIsRecorded(int, long)}, which is called in
   * {@link AudioRecordThread#run()}.
   */
  private boolean shouldPublishData = false;

  // Audio recording error handler functions.
  public static interface WebRtcAudioRecordErrorCallback {
    void onWebRtcAudioRecordInitError(String errorMessage);
    void onWebRtcAudioRecordStartError(String errorMessage);
    void onWebRtcAudioRecordError(String errorMessage);
  }

  private static WebRtcAudioRecordErrorCallback errorCallback = null;

  public static void setErrorCallback(WebRtcAudioRecordErrorCallback errorCallback) {
    Logging.d(TAG, "Set error callback");
    WebRtcAudioRecord.errorCallback = errorCallback;
  }

  /**
   * Audio thread which keeps calling ByteBuffer.read() waiting for audio
   * to be recorded. Feeds recorded data to the native counterpart as a
   * periodic sequence of callbacks using DataIsRecorded().
   * This thread uses a Process.THREAD_PRIORITY_URGENT_AUDIO priority.
   */
  private class AudioRecordThread extends Thread {
    private volatile boolean keepAlive = true;

    public AudioRecordThread(String name) {
      super(name);
    }

    @Override
    public void run() {
      Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
      Logging.d(TAG, "AudioRecordThread" + WebRtcAudioUtils.getThreadInfo());
      assertTrue(audioRecord.getRecordingState() == SharedAudioRecord.RECORDSTATE_RECORDING);

      long lastTime = System.nanoTime();
      while (keepAlive) {
        int bytesRead = audioRecord.read(byteBuffer, byteBuffer.capacity());
        if (bytesRead == byteBuffer.capacity()) {
          if (microphoneMute) {
            byteBuffer.clear();
            byteBuffer.put(emptyBytes);
          }
          if (shouldPublishData) {
            nativeDataIsRecorded(bytesRead, nativeAudioRecord);
          }
        } else {
          String errorMessage = "AudioRecord.read failed: " + bytesRead;
          Logging.e(TAG, errorMessage);
          if (bytesRead == SharedAudioRecord.ERROR_INVALID_OPERATION) {
            keepAlive = false;
            reportWebRtcAudioRecordError(errorMessage);
          }
        }
        if (DEBUG) {
          long nowTime = System.nanoTime();
          long durationInMs = TimeUnit.NANOSECONDS.toMillis((nowTime - lastTime));
          lastTime = nowTime;
          Logging.d(TAG, "bytesRead[" + durationInMs + "] " + bytesRead);
        }
      }

      try {
        if (audioRecord != null) {
          audioRecord.stop();
        }
      } catch (IllegalStateException e) {
        Logging.e(TAG, "AudioRecord.stop failed: " + e.getMessage());
      }
    }

    // Stops the inner thread loop and also calls AudioRecord.stop().
    // Does not block the calling thread.
    public void stopThread() {
      Logging.d(TAG, "stopThread");
      keepAlive = false;
    }
  }

  /**
   * Kinda hack warning.
   *
   * Returns instance when constructor was perfomed by C++ layer.
   *
   * @return The instance.
   */
  @Nullable
  public static WebRtcAudioRecord getInstance() {
    return instance;
  }

  /**
   * Creates instance.
   *
   * Instantiated by C++ layer.
   *
   * Reference to the instance is cached in {@link #instance}, when constructor performs.
   * This is kind of hack, because we cannot implement regular singleton pattern when we cannot execute the constructor.
   *
   * @param context The context.
   * @param nativeAudioRecord C++ layer recorder implementation handle/address/? - I don't care just now.
   */
  WebRtcAudioRecord(Context context, long nativeAudioRecord) {
    Logging.d(TAG, "ctor" + WebRtcAudioUtils.getThreadInfo());
    this.context = context;
    this.nativeAudioRecord = nativeAudioRecord;
    if (DEBUG) {
      WebRtcAudioUtils.logDeviceInfo(TAG);
    }
    effects = WebRtcAudioEffects.create();
    instance = this;
  }

  private boolean enableBuiltInAEC(boolean enable) {
    Logging.d(TAG, "enableBuiltInAEC(" + enable + ')');
    if (effects == null) {
      Logging.e(TAG, "Built-in AEC is not supported on this platform");
      return false;
    }
    return effects.setAEC(enable);
  }

  private boolean enableBuiltInNS(boolean enable) {
    Logging.d(TAG, "enableBuiltInNS(" + enable + ')');
    if (effects == null) {
      Logging.e(TAG, "Built-in NS is not supported on this platform");
      return false;
    }
    return effects.setNS(enable);
  }

  private int initRecording(int sampleRate, int channels) {
    Logging.d(TAG, "initRecording(sampleRate=" + sampleRate + ", channels=" + channels + ")");
    if (!WebRtcAudioUtils.hasPermission(context, android.Manifest.permission.RECORD_AUDIO)) {
      reportWebRtcAudioRecordInitError("RECORD_AUDIO permission is missing");
      return -1;
    }
    if (audioRecord != null) {
      reportWebRtcAudioRecordInitError("InitRecording called twice without StopRecording.");
      return -1;
    }
    final int bytesPerFrame = channels * (BITS_PER_SAMPLE / 8);
    final int framesPerBuffer = sampleRate / BUFFERS_PER_SECOND;
    byteBuffer = ByteBuffer.allocateDirect(bytesPerFrame * framesPerBuffer);
    Logging.d(TAG, "byteBuffer.capacity: " + byteBuffer.capacity());
    emptyBytes = new byte[byteBuffer.capacity()];
    // Rather than passing the ByteBuffer with every callback (requiring
    // the potentially expensive GetDirectBufferAddress) we simply have the
    // the native class cache the address to the memory once.
    nativeCacheDirectBufferAddress(byteBuffer, nativeAudioRecord);

    // Get the minimum buffer size required for the successful creation of
    // an AudioRecord object, in byte units.
    // Note that this size doesn't guarantee a smooth recording under load.
    final int channelConfig = channelCountToConfiguration(channels);
    int minBufferSize =
            SharedAudioRecord.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
    if (minBufferSize == SharedAudioRecord.ERROR || minBufferSize == SharedAudioRecord.ERROR_BAD_VALUE) {
      reportWebRtcAudioRecordInitError("AudioRecord.getMinBufferSize failed: " + minBufferSize);
      return -1;
    }
    Logging.d(TAG, "AudioRecord.getMinBufferSize: " + minBufferSize);

    // Use a larger buffer size than the minimum required when creating the
    // AudioRecord instance to ensure smooth recording under load. It has been
    // verified that it does not increase the actual recording latency.
    int bufferSizeInBytes = Math.max(BUFFER_SIZE_FACTOR * minBufferSize, byteBuffer.capacity());
    Logging.d(TAG, "bufferSizeInBytes: " + bufferSizeInBytes);
    try {
      audioRecord = new SharedAudioRecord(AudioSource.VOICE_COMMUNICATION, sampleRate, channelConfig,
          AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes);
    } catch (IllegalArgumentException e) {
      reportWebRtcAudioRecordInitError("AudioRecord ctor error: " + e.getMessage());
      releaseAudioResources();
      return -1;
    }
    if (audioRecord == null || audioRecord.getState() != SharedAudioRecord.STATE_INITIALIZED) {
      reportWebRtcAudioRecordInitError("Failed to create a new AudioRecord instance");
      releaseAudioResources();
      return -1;
    }
    if (effects != null) {
      effects.enable(audioRecord.getAudioSessionId());
    }
    logMainParameters();
    logMainParametersExtended();
    return framesPerBuffer;
  }

  /**
   * Starts recording of audio.
   *
   * Sets {@link #shouldPublishData} to true, to propagate fresh recorded audio data to the C++ layer.
   *
   * @return
   */
  private boolean startRecording() {
    shouldPublishData = true;
    return startRecordingInternal();
  }

  private boolean startRecordingInternal() {
    Logging.d(TAG, "startRecordingInternal");
    if (audioThread != null && audioThread.isAlive() && audioRecord.getState() == SharedAudioRecord.RECORDSTATE_RECORDING) {
      return true;
    }
    try {
      audioRecord.startRecording();
    } catch (IllegalStateException e) {
      reportWebRtcAudioRecordStartError("AudioRecord.startRecording failed: " + e.getMessage());
      return false;
    }
    if (audioRecord.getRecordingState() != SharedAudioRecord.RECORDSTATE_RECORDING) {
      reportWebRtcAudioRecordStartError("AudioRecord.startRecording failed - incorrect state :"
              + audioRecord.getRecordingState());
      return false;
    }
    audioThread = new AudioRecordThread("AudioRecordJavaThread");
    audioThread.start();
    return true;
  }

  /**
   * Method does not exactly do what the name declares:
   * Name should be kept, because method is called from C++ right under {@link #stopRecording} name.
   *
   * Original code of this method has been moved to {@link #stopRecordingInternal()}.
   *
   * @return Returns always true. Boolean return value type kept due to C++ caller interface expectations.
   */
  private boolean stopRecording() {
    Logging.d(TAG, "stopRecording");
    shouldPublishData = false;
    return true;
  }

  /**
   * Stops recording.
   */
  public void stopAudioRecording() {
    stopRecordingInternal();
  }

  /**
   * Replacement for {@link #stopRecording()}, which had to be modified under original name.
   */
  private void stopRecordingInternal() {
    Logging.d(TAG, "stopRecordingInternal");
    if (audioThread != null) {
      audioThread.stopThread();
      if (!ThreadUtils.joinUninterruptibly(audioThread, AUDIO_RECORD_THREAD_JOIN_TIMEOUT_MS)) {
        Logging.e(TAG, "Join of AudioRecordJavaThread timed out");
      }
    }
    audioThread = null;
    if (effects != null) {
      effects.release();
    }
    releaseAudioResources();
  }

  private void logMainParameters() {
    Logging.d(TAG, "AudioRecord: "
            + "session ID: " + audioRecord.getAudioSessionId() + ", "
            + "channels: " + audioRecord.getChannelCount() + ", "
            + "sample rate: " + audioRecord.getSampleRate());
  }

  private void logMainParametersExtended() {
    if (WebRtcAudioUtils.runningOnMarshmallowOrHigher()) {
      Logging.d(TAG, "AudioRecord: "
              // The frame count of the native AudioRecord buffer.
              + "buffer size in frames: " + audioRecord.getBufferSizeInFrames());
    }
  }

  // Helper method which throws an exception  when an assertion has failed.
  private static void assertTrue(boolean condition) {
    if (!condition) {
      throw new AssertionError("Expected condition to be true");
    }
  }

  private int channelCountToConfiguration(int channels) {
    return (channels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO);
  }

  private native void nativeCacheDirectBufferAddress(ByteBuffer byteBuffer, long nativeAudioRecord);

  private native void nativeDataIsRecorded(int bytes, long nativeAudioRecord);

  // Sets all recorded samples to zero if |mute| is true, i.e., ensures that
  // the microphone is muted.
  public static void setMicrophoneMute(boolean mute) {
    Logging.w(TAG, "setMicrophoneMute(" + mute + ")");
    microphoneMute = mute;
  }

  // Releases the native AudioRecord resources.
  private void releaseAudioResources() {
    if (audioRecord != null) {
      audioRecord.release();
      audioRecord = null;
    }
  }

  private void reportWebRtcAudioRecordInitError(String errorMessage) {
    Logging.e(TAG, "Init recording error: " + errorMessage);
    if (errorCallback != null) {
      errorCallback.onWebRtcAudioRecordInitError(errorMessage);
    }
  }

  private void reportWebRtcAudioRecordStartError(String errorMessage) {
    Logging.e(TAG, "Start recording error: " + errorMessage);
    if (errorCallback != null) {
      errorCallback.onWebRtcAudioRecordStartError(errorMessage);
    }
  }

  private void reportWebRtcAudioRecordError(String errorMessage) {
    Logging.e(TAG, "Run-time recording error: " + errorMessage);
    if (errorCallback != null) {
      errorCallback.onWebRtcAudioRecordError(errorMessage);
    }
  }
}
