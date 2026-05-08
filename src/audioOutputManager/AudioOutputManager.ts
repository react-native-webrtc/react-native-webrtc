import { NativeModules, Platform } from 'react-native';

import { addListener, removeListener } from '../EventEmitter';

/** Normalized audio device categories across iOS and Android. */
export enum AudioDeviceType {
    earpiece = 'earpiece',
    speaker = 'speaker',
    bluetooth = 'bluetooth',
    wiredHeadset = 'wiredHeadset',
    usb = 'usb',
    hdmi = 'hdmi',
    airplay = 'airplay',
    carAudio = 'carAudio',
    hearingAid = 'hearingAid',
    lineOut = 'lineOut',
    unknown = 'unknown',
}

/** Describes a single audio output device. */
export type AudioDevice = {
    /** Normalized device category. */
    type: AudioDeviceType;
    /** Platform-specific device type string (e.g. AVAudioSession port type on iOS). */
    nativeType: string;
    /** Human-readable device name. */
    name: string;
    /** Unique device identifier. */
    id: string;
};

/** Payload emitted when the active audio output or the list of available devices changes. */
export type AudioOutputChangedInfo = {
    currentAudioOutput: AudioDevice | null;
    availableAudioOutputs: AudioDevice[];
};

const { WebRTCModule } = NativeModules;

function ensurePlatform(expected: string, methodName: string): void {
    if (Platform.OS !== expected) {
        throw new Error(
            `AudioOutputManager.${expected}.${methodName} is not available on ${Platform.OS}`,
        );
    }
}

/** Imperative API for querying and controlling audio output routing. */
export const AudioOutputManager = {
    /** Returns all audio output devices currently reachable by the system. */
    getAvailableAudioOutputs(): Promise<AudioDevice[]> {
        return WebRTCModule.getAvailableAudioOutputs();
    },

    /** Returns the device audio is currently routed to, or `null` if unknown. */
    getCurrentAudioOutput(): Promise<AudioDevice | null> {
        return WebRTCModule.getCurrentAudioOutput();
    },

    /** Subscribes to audio output changes. Returns an unsubscribe function. */
    onAudioOutputChanged(
        handler: (info: AudioOutputChangedInfo) => void,
    ): () => void {
        const listener = {};
        addListener(
            listener,
            'audioOutputChanged',
            handler as (event: unknown) => void,
        );
        return () => removeListener(listener);
    },

    ios: {
        /** Presents the native iOS audio route picker (AVRoutePickerView). */
        showAudioRoutePicker(): void {
            ensurePlatform('ios', 'showAudioRoutePicker');
            WebRTCModule.showAudioRoutePicker();
        },

        /**
         * Forces audio output to the built-in speaker or resets to the default route.
         *
         * @param output `'speaker'` to route to the built-in speaker, `'none'` to restore the default route.
         */
        overrideAudioOutput(output: 'speaker' | 'none'): Promise<void> {
            ensurePlatform('ios', 'overrideAudioOutput');
            return WebRTCModule.overrideAudioOutput(output);
        },
    },

    android: {
        /**
         * Routes audio to a specific device.
         *
         * The returned promise resolves only once the device is confirmed active.
         * It rejects with one of the following error codes:
         *
         * - `E_AUDIO_OUTPUT_SELECT` — invalid device ID, device unavailable, or the
         *   underlying `AudioManager` call failed.
         * - `E_AUDIO_OUTPUT_SUPERSEDED` — a newer `selectAudioOutput` call was made
         *   before this one completed. Callers may safely ignore this code.
         * - `E_AUDIO_OUTPUT_TIMEOUT` — the system did not confirm the route change
         *   within the platform timeout.
         * - `E_AUDIO_OUTPUT_CANCELLED` — the audio output observer was stopped
         *   (e.g. module teardown) before the route change completed.
         *
         * @param deviceId The {@link AudioDevice.id} of the target device.
         */
        selectAudioOutput(deviceId: string): Promise<void> {
            ensurePlatform('android', 'selectAudioOutput');
            return WebRTCModule.selectAudioOutput(deviceId);
        },
    },
};
