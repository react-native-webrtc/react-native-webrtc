import { NativeModules, Platform } from 'react-native';

const { WebRTCModule } = NativeModules;

export enum AudioEngineMuteMode {
  Unknown = -1,
  VoiceProcessing = 0,
  RestartEngine = 1,
  InputMixer = 2,
}

export interface AudioEngineAvailability {
  isInputAvailable: boolean;
  isOutputAvailable: boolean;
}

export const AudioEngineAvailability = {
    default: {
        isInputAvailable: true,
        isOutputAvailable: true,
    },
    none: {
        isInputAvailable: false,
        isOutputAvailable: false,
    },
} as const;

/**
 * Apple audio session configuration for a single engine state. Matches the
 * AppleAudioConfiguration shape used by AudioSession.setAppleAudioConfiguration.
 */
export interface AutomaticAppleAudioConfiguration {
  audioCategory?: string;
  audioMode?: string;
  audioCategoryOptions?: string[];
}

/**
 * Native default audio-session policy. When set, the native observer configures
 * the AVAudioSession in willEnable/didDisable without a JS round trip.
 * `recording` is applied while recording is enabled, `playout` while only
 * playout is enabled, and the session is deactivated on full stop when
 * `deactivateOnStop` is true.
 */
export interface AutomaticAudioSessionConfiguration {
  recording: AutomaticAppleAudioConfiguration;
  playout: AutomaticAppleAudioConfiguration;
  deactivateOnStop: boolean;
}

/**
 * Audio Device Module API for controlling audio devices and settings.
 * iOS/macOS only - will throw on Android.
 */
export class AudioDeviceModule {
    /**
     * Push (or clear with null) the native default audio-session configuration
     * policy. When set, the native observer configures the session itself in
     * willEnable/didDisable, removing the JS round trip from the default path.
     * iOS/macOS only, a no-op on Android.
     */
    static setAutomaticAudioSessionConfiguration(config: AutomaticAudioSessionConfiguration | null): void {
        if (Platform.OS === 'android' || !WebRTCModule) {
            return;
        }

        WebRTCModule.audioDeviceModuleSetAutomaticAudioSessionConfiguration(config);
    }

    /**
     * Start audio playback
     */
    static async startPlayout(): Promise<void> {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleStartPlayout();
    }

    /**
     * Stop audio playback
     */
    static async stopPlayout(): Promise<void> {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleStopPlayout();
    }

    /**
     * Start audio recording
     */
    static async startRecording(): Promise<void> {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleStartRecording();
    }

    /**
     * Stop audio recording
     */
    static async stopRecording(): Promise<void> {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleStopRecording();
    }

    /**
     * Initialize and start local audio recording (calls initAndStartRecording)
     */
    static async startLocalRecording(): Promise<void> {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleStartLocalRecording();
    }

    /**
     * Stop local audio recording
     */
    static async stopLocalRecording(): Promise<void> {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleStopLocalRecording();
    }

    /**
     * Mute or unmute the microphone
     */
    static async setMicrophoneMuted(muted: boolean): Promise<void> {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleSetMicrophoneMuted(muted);
    }

    /**
     * Check if microphone is currently muted
     */
    static isMicrophoneMuted(): boolean {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleIsMicrophoneMuted();
    }

    /**
     * Enable or disable voice processing (requires engine restart)
     */
    static async setVoiceProcessingEnabled(enabled: boolean): Promise<void> {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleSetVoiceProcessingEnabled(enabled);
    }

    /**
     * Check if voice processing is enabled
     */
    static isVoiceProcessingEnabled(): boolean {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleIsVoiceProcessingEnabled();
    }

    /**
     * Temporarily bypass voice processing without restarting the engine
     */
    static setVoiceProcessingBypassed(bypassed: boolean): void {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        WebRTCModule.audioDeviceModuleSetVoiceProcessingBypassed(bypassed);
    }

    /**
     * Check if voice processing is currently bypassed
     */
    static isVoiceProcessingBypassed(): boolean {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleIsVoiceProcessingBypassed();
    }

    /**
     * Enable or disable Automatic Gain Control (AGC)
     */
    static setVoiceProcessingAGCEnabled(enabled: boolean): void {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleSetVoiceProcessingAGCEnabled(enabled);
    }

    /**
     * Check if AGC is enabled
     */
    static isVoiceProcessingAGCEnabled(): boolean {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleIsVoiceProcessingAGCEnabled();
    }

    /**
     * Check if audio is currently playing
     */
    static isPlaying(): boolean {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleIsPlaying();
    }

    /**
     * Check if audio is currently recording
     */
    static isRecording(): boolean {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleIsRecording();
    }

    /**
     * Check if the audio engine is running
     */
    static isEngineRunning(): boolean {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleIsEngineRunning();
    }

    /**
     * Set the microphone mute mode
     */
    static async setMuteMode(mode: AudioEngineMuteMode): Promise<void> {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleSetMuteMode(mode);
    }

    /**
     * Get the current mute mode
     */
    static getMuteMode(): AudioEngineMuteMode {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleGetMuteMode();
    }

    /**
     * Enable or disable advanced audio ducking
     */
    static setAdvancedDuckingEnabled(enabled: boolean): void {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleSetAdvancedDuckingEnabled(enabled);
    }

    /**
     * Check if advanced ducking is enabled
     */
    static isAdvancedDuckingEnabled(): boolean {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleIsAdvancedDuckingEnabled();
    }

    /**
     * Set the audio ducking level (0-100)
     */
    static setDuckingLevel(level: number): void {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleSetDuckingLevel(level);
    }

    /**
     * Get the current ducking level
     */
    static getDuckingLevel(): number {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleGetDuckingLevel();
    }

    /**
     * Check if recording always prepared mode is enabled
     */
    static isRecordingAlwaysPreparedMode(): boolean {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleIsRecordingAlwaysPreparedMode();
    }

    /**
     * Enable or disable recording always prepared mode
     */
    static async setRecordingAlwaysPreparedMode(enabled: boolean): Promise<void> {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleSetRecordingAlwaysPreparedMode(enabled);
    }

    /**
     * Get the current engine availability (input/output availability)
     */
    static getEngineAvailability(): AudioEngineAvailability {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleGetEngineAvailability();
    }

    /**
     * Set the engine availability (input/output availability)
     */
    static async setEngineAvailability(availability: AudioEngineAvailability): Promise<void> {
        if (Platform.OS === 'android') {
            throw new Error('AudioDeviceModule is only available on iOS/macOS');
        }

        return WebRTCModule.audioDeviceModuleSetEngineAvailability(availability);
    }
}
