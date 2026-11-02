package com.oney.WebRTCModule;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.DynamicsProcessing;
import android.media.audiofx.LoudnessEnhancer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import org.webrtc.audio.JavaAudioDeviceModule;

import java.lang.reflect.Field;

/**
 * Livestream audio: an audio device module that plays remote audio as media, through a voice
 * leveller, and captures a host's microphone as it sounds, with the field trials a livestream
 * plays best with. WebRTCModule builds its peer connection factory with it unless the app set its
 * own {@code audioDeviceModule} on WebRTCModuleOptions.
 *
 * <p>MEDIA PATH: libwebrtc plays as {@code USAGE_VOICE_COMMUNICATION}, through the telephony
 * processing chain, band-limited and on the call volume. This plays as {@code USAGE_MEDIA}, in
 * stereo, on the media volume and the loudspeaker.
 *
 * <p>LEVELLER: the platform's DynamicsProcessing effect on WebRTC's own AudioTrack session, with
 * the chain the iOS device runs in software: input gain, compressor, limiter. Its numbers mirror
 * {@code WRPPlayoutConfigDefault} in ios/RCTWebRTC/livestream/WRPPlayoutProcessor.c; change them
 * together.
 *
 * <p>FOCUS: WebRTC never asks for audio focus, so another app's music kept playing over the
 * stream. This takes focus the way a video player does while it plays or records, and goes quiet
 * for a phone call.
 *
 * <p>MICROPHONE: a host is captured from {@code MIC} rather than {@code VOICE_COMMUNICATION},
 * without the platform's echo canceller and noise suppressor: what the host says goes out as it
 * sounds. libwebrtc's own processing still follows the track's audio constraints. With no echo
 * canceller, audio going both ways over a loudspeaker echoes; such apps set {@code audioSource} to
 * {@code voiceCommunication}, or {@code enabled} to false.
 *
 * <p>Configured by {@code <meta-data>} in the app's manifest, names prefixed
 * {@code com.fluxlabs.webrtc.livestream.}, which the Expo config plugin writes. Every entry is
 * optional: {@code enabled} (true), {@code playoutDelayMs} (0; up to 10000; video only on Android,
 * so opt-in), {@code maxPlayoutDelayMs}, {@code leveller} (true), {@code levellerInputGainDb} (15;
 * 0 to 30), {@code audioFocus} (true), {@code networkResilience} (true), {@code audioSource}
 * ({@code mic}, {@code camcorder}, {@code voiceCommunication} or {@code unprocessed}).
 */
final class LivestreamAudio {
    private static final String TAG = "WebRTCLivestream";

    /** What the config plugin prefixes every meta-data name with. */
    private static final String META_PREFIX = "com.fluxlabs.webrtc.livestream.";

    private static final float THRESHOLD_DB = -20f;
    private static final float RATIO = 4f;
    private static final float KNEE_DB = 6f;

    /**
     * Slow, so the compressor follows the speaker's level rather than each syllable and pauses do
     * not swell with room noise. iOS also holds the gain through pauses, which DynamicsProcessing
     * cannot; at these times that is worth under 1 dB.
     */
    private static final float ATTACK_MS = 30f;
    private static final float RELEASE_MS = 2000f;
    private static final float CEILING_DB = -1f;
    private static final float LIMITER_ATTACK_MS = 1f;
    private static final float LIMITER_RELEASE_MS = 60f;
    private static final float LIMITER_RATIO = 10f;

    /** One band spanning the whole spectrum: this is a leveller, not a multiband EQ. */
    private static final float FULL_BAND_HZ = 20_000f;

    /**
     * DynamicsProcessing is API 28+, and an OEM build can lack it. LoudnessEnhancer is a
     * compressor-backed boost that exists everywhere; this is its target.
     */
    private static final int FALLBACK_LOUDNESS_MB = 600;

    /**
     * libwebrtc sizes its AudioTrack at the platform minimum, which underruns, an audible crackle,
     * whenever the audio thread is late. Twice the minimum costs a few tens of milliseconds of
     * latency, which a one-way stream does not feel.
     */
    private static final String PLAYOUT_BUFFER_TRIAL = "WebRTC-AudioDevicePlayoutBufferSizeFactor";

    private static final AudioAttributes PLAYOUT_ATTRIBUTES = new AudioAttributes.Builder()
                                                                  .setUsage(AudioAttributes.USAGE_MEDIA)
                                                                  .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                                                  .build();

    /** Engine layouts to offer DynamicsProcessing, in order; see {@link #attachLeveller}. */
    private static final int[][] LAYOUTS = {
        {DynamicsProcessing.VARIANT_FAVOR_TIME_RESOLUTION, 0},
        {DynamicsProcessing.VARIANT_FAVOR_TIME_RESOLUTION, 1},
        {DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION, 0},
        {DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION, 1},
    };

    private final Context appContext;
    private final int playoutDelayMs;
    private final int maxPlayoutDelayMs;
    private final boolean leveller;
    private final float levellerInputGainDb;
    private final boolean audioFocus;
    private final boolean networkResilience;
    private final String audioSourceName;
    private final int audioSource;

    @Nullable
    private volatile JavaAudioDeviceModule module;

    // Playout and recording start on WebRTC's audio threads, and JS switches the leveller.
    private final Object lock = new Object();
    @Nullable
    private AudioEffect effect;
    @Nullable
    private Object focusRequest;
    private boolean hasFocus;
    private boolean levellerOn;
    private volatile boolean playing;
    private volatile boolean recording;

    private final AudioManager.OnAudioFocusChangeListener focusListener = change -> {
        JavaAudioDeviceModule adm = module;
        if (adm == null) {
            return;
        }
        switch (change) {
            case AudioManager.AUDIOFOCUS_GAIN:
                adm.setSpeakerMute(false);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // A call, an assistant, a navigation prompt. Muted rather than stopped: a live
                // source will not resume a stopped play session.
                adm.setSpeakerMute(true);
                break;
            default:
                // Left playing on a permanent loss. A live stream has no play button to come back
                // with, so silencing it would strand the viewer for the rest of the stream; the
                // other app is one the viewer just started. Ducking is the system's, on API 26+.
                break;
        }
    };

    private LivestreamAudio(Context context, Bundle meta) {
        appContext = context.getApplicationContext();
        // 10 s is where libwebrtc stops honouring a playout delay.
        playoutDelayMs = (int) clamp(number(meta, "playoutDelayMs", 0), 0, 10_000);
        maxPlayoutDelayMs =
                (int) clamp(number(meta, "maxPlayoutDelayMs", Math.max(2500, playoutDelayMs)), playoutDelayMs, 10_000);
        leveller = bool(meta, "leveller", true);
        levellerInputGainDb = (float) clamp(number(meta, "levellerInputGainDb", 15), 0, 30);
        audioFocus = bool(meta, "audioFocus", true);
        networkResilience = bool(meta, "networkResilience", true);
        levellerOn = leveller;

        String source = string(meta, "audioSource", "mic");
        switch (source) {
            case "camcorder":
                audioSource = MediaRecorder.AudioSource.CAMCORDER;
                break;
            case "voiceCommunication":
                audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
                break;
            case "unprocessed":
                audioSource = MediaRecorder.AudioSource.UNPROCESSED;
                break;
            default:
                source = "mic";
                audioSource = MediaRecorder.AudioSource.MIC;
                break;
        }
        audioSourceName = source;
    }

    /** Livestream audio as the manifest configures it; null where it is off. */
    @Nullable
    static LivestreamAudio fromManifest(Context context) {
        Bundle meta = metaData(context);
        if (!bool(meta, "enabled", true)) {
            Log.i(TAG, "disabled in the manifest; libwebrtc's own audio device stays");
            return null;
        }
        return new LivestreamAudio(context, meta);
    }

    /** {@code fieldTrials} plus the ones livestream audio needs. Trials the app sets win. */
    String fieldTrialsAdding(@Nullable String fieldTrials) {
        StringBuilder trials = new StringBuilder(fieldTrials != null ? fieldTrials : "");
        addTrial(trials, PLAYOUT_BUFFER_TRIAL, "2.0");
        if (networkResilience) {
            // The wait for a resent video packet has had no switch since libwebrtc M155 (at most
            // 200 ms past the jitter); a playout delay is what gives resends their time.
            // A keyframe of a sharp stream arrives as one burst; 256 KB overflows.
            addTrial(trials, "WebRTC-ReceiveBufferSize", "size_bytes:1048576");
        }
        if (playoutDelayMs > 0) {
            // Video only: Android has no hook for holding audio back to match, so lip sync drags
            // the voice along at 80 ms a second and it runs ahead of the picture for the first
            // seconds of every session. Opt-in for that reason.
            addTrial(trials, "WebRTC-ForcePlayoutDelay", "min_ms:" + playoutDelayMs + ",max_ms:" + maxPlayoutDelayMs);
        }
        return trials.toString();
    }

    /** The module the peer connection factory plays and records through. */
    JavaAudioDeviceModule createAudioDeviceModule() {
        boolean voiceProcessing = audioSource == MediaRecorder.AudioSource.VOICE_COMMUNICATION;
        JavaAudioDeviceModule adm =
                JavaAudioDeviceModule.builder(appContext)
                        .setAudioAttributes(PLAYOUT_ATTRIBUTES)
                        // Stereo streams play in stereo; libwebrtc's default is mono.
                        .setUseStereoOutput(true)
                        .setAudioSource(audioSource)
                        .setUseHardwareAcousticEchoCanceler(voiceProcessing)
                        .setUseHardwareNoiseSuppressor(voiceProcessing)
                        .setAudioTrackStateCallback(new JavaAudioDeviceModule.AudioTrackStateCallback() {
                            @Override
                            public void onWebRtcAudioTrackStart() {
                                onPlayoutStarted();
                            }

                            @Override
                            public void onWebRtcAudioTrackStop() {
                                onPlayoutStopped();
                            }
                        })
                        .setAudioRecordStateCallback(new JavaAudioDeviceModule.AudioRecordStateCallback() {
                            @Override
                            public void onWebRtcAudioRecordStart() {
                                onRecordingChanged(true);
                            }

                            @Override
                            public void onWebRtcAudioRecordStop() {
                                onRecordingChanged(false);
                            }
                        })
                        .setEnableVolumeLogger(false)
                        .createAudioDeviceModule();
        module = adm;
        return adm;
    }

    /** The config, and whether audio is playing and recording: {@code livestreamAudioState()} in JS. */
    WritableMap state() {
        WritableMap config = Arguments.createMap();
        config.putDouble("playoutDelayMs", playoutDelayMs);
        config.putDouble("maxPlayoutDelayMs", maxPlayoutDelayMs);
        config.putBoolean("leveller", leveller);
        config.putDouble("levellerInputGainDb", levellerInputGainDb);
        config.putBoolean("manageAudioSession", false);
        config.putBoolean("audioFocus", audioFocus);
        config.putBoolean("networkResilience", networkResilience);
        config.putString("audioSource", audioSourceName);

        WritableMap state = Arguments.createMap();
        state.putBoolean("installed", true);
        state.putBoolean("playing", playing);
        state.putBoolean("recording", recording);
        synchronized (lock) {
            state.putBoolean("levellerEnabled", leveller && levellerOn);
        }
        state.putMap("config", config);
        return state;
    }

    /** Switches the leveller, now and for the next playout; returns whether it is on. */
    boolean setLevellerEnabled(boolean enabled) {
        if (!leveller) {
            return false;
        }
        synchronized (lock) {
            levellerOn = enabled;
            if (effect != null) {
                effect.setEnabled(enabled);
            }
        }
        return enabled;
    }

    private void onPlayoutStarted() {
        AudioTrack track = webRtcAudioTrack();
        synchronized (lock) {
            playing = true;
            releaseEffect();
            if (leveller && track != null) {
                effect = attachLeveller(track.getAudioSessionId());
                if (effect != null) {
                    effect.setEnabled(levellerOn);
                }
            }
            updateFocus();
        }
    }

    private void onPlayoutStopped() {
        synchronized (lock) {
            playing = false;
            releaseEffect();
            updateFocus();
        }
    }

    private void onRecordingChanged(boolean isRecording) {
        synchronized (lock) {
            recording = isRecording;
            updateFocus();
        }
    }

    /**
     * The AudioTrack libwebrtc plays through. JavaAudioDeviceModule exposes neither it nor its
     * session id, so it is read by field name: names the {@code -keep class org.webrtc.** { *; }}
     * rule in consumer-rules.pro preserves in release builds.
     */
    @Nullable
    private AudioTrack webRtcAudioTrack() {
        JavaAudioDeviceModule adm = module;
        if (adm == null) {
            return null;
        }
        try {
            Field outputField = JavaAudioDeviceModule.class.getDeclaredField("audioOutput");
            outputField.setAccessible(true);
            Object output = outputField.get(adm);
            if (output == null) {
                return null;
            }
            Field trackField = output.getClass().getDeclaredField("audioTrack");
            trackField.setAccessible(true);
            Object track = trackField.get(output);
            return track instanceof AudioTrack ? (AudioTrack) track : null;
        } catch (Exception e) {
            Log.w(TAG, "could not reach WebRTC's AudioTrack; playing without the leveller", e);
            return null;
        }
    }

    /**
     * Implementations differ on what DynamicsProcessing's engine accepts: some want unused stages
     * declared with zero bands, some reject a zero-band stage, and not every build takes both
     * resolution variants. So each layout is tried until one is accepted.
     *
     * <p>Before Android 12 only the first layout is tried. A rejected layout has already attached
     * its effect to the session, and nothing can release it until the garbage collector finalizes
     * it; every further try adds another handle on the same effect, whose control changes are
     * called back on a binder thread. Android 10 and 11 can make that callback into a handle being
     * released and abort (SIGABRT in libaudioeffect_jni: 5 crashes in a month, all on Android 10
     * and 11, none on 12 or later). A build that rejects the first layout gets the
     * LoudnessEnhancer, a different effect, so no two handles share one.
     */
    @Nullable
    private AudioEffect attachLeveller(int sessionId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            int tries = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? LAYOUTS.length : 1;
            for (int i = 0; i < tries; i++) {
                int[] layout = LAYOUTS[i];
                try {
                    AudioEffect dynamics = dynamicsProcessing(sessionId, layout[0], layout[1]);
                    Log.i(TAG, "leveller on session " + sessionId + ": DynamicsProcessing " + describe(layout));
                    return dynamics;
                } catch (Exception e) {
                    Log.w(TAG, "DynamicsProcessing rejected " + describe(layout) + ": " + e.getMessage());
                }
            }
            Log.w(TAG, "DynamicsProcessing unavailable, falling back to LoudnessEnhancer");
        }
        try {
            LoudnessEnhancer enhancer = new LoudnessEnhancer(sessionId);
            enhancer.setTargetGain(FALLBACK_LOUDNESS_MB);
            enhancer.setEnabled(true);
            Log.i(TAG, "leveller on session " + sessionId + ": LoudnessEnhancer");
            return enhancer;
        } catch (Exception e) {
            Log.w(TAG, "no leveller available; playing at unity gain", e);
            return null;
        }
    }

    private static String describe(int[] layout) {
        return (layout[0] == DynamicsProcessing.VARIANT_FAVOR_TIME_RESOLUTION ? "time" : "frequency")
                + " resolution, " + layout[1] + "-band unused stages";
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private DynamicsProcessing dynamicsProcessing(int sessionId, int variant, int unusedStageBands) {
        DynamicsProcessing.Mbc mbc = new DynamicsProcessing.Mbc(true, true, 1);
        // Noise gate and expander off.
        mbc.setBand(0,
                new DynamicsProcessing.MbcBand(
                        true, FULL_BAND_HZ, ATTACK_MS, RELEASE_MS, RATIO, THRESHOLD_DB, KNEE_DB, -90f, 1f, 0f, 0f));
        DynamicsProcessing.Config config =
                new DynamicsProcessing.Config
                        .Builder(variant, 2, false, unusedStageBands, true, 1, false, unusedStageBands, true)
                        .setInputGainAllChannelsTo(levellerInputGainDb)
                        .setMbcAllChannelsTo(mbc)
                        .setLimiterAllChannelsTo(new DynamicsProcessing.Limiter(
                                true, true, 0, LIMITER_ATTACK_MS, LIMITER_RELEASE_MS, LIMITER_RATIO, CEILING_DB, 0f))
                        .build();
        DynamicsProcessing dynamics = new DynamicsProcessing(0, sessionId, config);
        dynamics.setEnabled(true);
        return dynamics;
    }

    private void releaseEffect() {
        if (effect != null) {
            effect.release();
            effect = null;
        }
    }

    /** Holds focus while audio plays or the microphone records. Called with the lock held. */
    private void updateFocus() {
        boolean wanted = audioFocus && (playing || recording);
        if (wanted == hasFocus) {
            return;
        }
        AudioManager audioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            return;
        }
        hasFocus = wanted;
        JavaAudioDeviceModule adm = module;
        if (wanted) {
            int result = requestFocus(audioManager);
            if (adm == null) {
                return;
            }
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                adm.setSpeakerMute(false);
            } else if (result == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
                // A call is in progress; AUDIOFOCUS_GAIN arrives when it ends.
                adm.setSpeakerMute(true);
            } else {
                // Refused outright: play anyway rather than leave the stream silent.
                Log.w(TAG, "audio focus refused");
            }
        } else {
            abandonFocus(audioManager);
            if (adm != null) {
                adm.setSpeakerMute(false);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private int requestFocus(AudioManager audioManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest request = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                                                .setAudioAttributes(PLAYOUT_ATTRIBUTES)
                                                .setAcceptsDelayedFocusGain(true)
                                                .setOnAudioFocusChangeListener(
                                                        focusListener, new Handler(Looper.getMainLooper()))
                                                .build();
            focusRequest = request;
            return audioManager.requestAudioFocus(request);
        }
        return audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    @SuppressWarnings("deprecation")
    private void abandonFocus(AudioManager audioManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (focusRequest instanceof AudioFocusRequest) {
                audioManager.abandonAudioFocusRequest((AudioFocusRequest) focusRequest);
            }
            focusRequest = null;
        } else {
            audioManager.abandonAudioFocus(focusListener);
        }
    }

    private static void addTrial(StringBuilder trials, String name, String group) {
        if (trials.indexOf(name + "/") < 0) {
            trials.append(name).append('/').append(group).append('/');
        }
    }

    private static Bundle metaData(Context context) {
        try {
            Bundle meta = context.getPackageManager()
                                  .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA)
                                  .metaData;
            return meta != null ? meta : new Bundle();
        } catch (PackageManager.NameNotFoundException e) {
            return new Bundle();
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // The manifest hands meta-data back as whatever type it parsed the literal as. Bundle.get is
    // deprecated for typed getters, which would each reject the other types.
    @SuppressWarnings("deprecation")
    private static double number(Bundle meta, String key, double fallback) {
        Object value = meta.get(META_PREFIX + key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        return fallback;
    }

    @SuppressWarnings("deprecation")
    private static boolean bool(Bundle meta, String key, boolean fallback) {
        Object value = meta.get(META_PREFIX + key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }
        return fallback;
    }

    @SuppressWarnings("deprecation")
    private static String string(Bundle meta, String key, String fallback) {
        Object value = meta.get(META_PREFIX + key);
        return value instanceof String ? (String) value : fallback;
    }
}
