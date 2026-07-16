package com.oney.WebRTCModule.voip

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

/**
 * Plays the outgoing-call ringback ("dialtone") while an outgoing call is
 * connecting, until [stop] is called.
 *
 * Synthesizes the standard call ringback with [ToneGenerator] on the voice-call
 * stream — no bundled asset, and correct in-call routing by construction.
 * Every method is a graceful no-op on failure, so callers never need to guard.
 */
object DialtonePlayer {
    private const val TAG = "FishjamVoip.Dialtone"

    /** ToneGenerator volume (0-100). */
    private const val TONE_VOLUME = 80

    @Volatile
    private var tone: ToneGenerator? = null

    /** Starts the ringback. No-op if already playing. */
    @Synchronized
    fun play() {
        if (tone != null) return
        try {
            val tg = ToneGenerator(AudioManager.STREAM_VOICE_CALL, TONE_VOLUME)
            tone = tg
            // TONE_SUP_RINGTONE carries its own on/off cadence and repeats until stopped.
            tg.startTone(ToneGenerator.TONE_SUP_RINGTONE)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to start ringback tone: ${e.localizedMessage}")
            tone?.release()
            tone = null
        }
    }

    /** Stops and releases the ringback. Safe to call when nothing is playing. */
    @Synchronized
    fun stop() {
        val tg = tone ?: return
        tone = null
        try {
            tg.stopTone()
            tg.release()
        } catch (e: Throwable) {
            Log.e(TAG, "Error stopping ringback tone: ${e.localizedMessage}")
        }
    }
}
