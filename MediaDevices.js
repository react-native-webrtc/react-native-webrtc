'use strict';

import {NativeModules, Platform} from 'react-native';
import EventTarget from 'event-target-shim';

import getUserMedia from './getUserMedia';

const {WebRTCModule} = NativeModules;

const MEDIA_DEVICES_EVENTS = [
    'devicechange'
];

// Source: https://developer.android.com/reference/android/media/AudioAttributes
export const AudioUsageAndroid = {
    USAGE_ALARM: 0x4,
    USAGE_ASSISTANCE_ACCESSIBILITY: 0xB,
    USAGE_ASSISTANCE_NAVIGATION_GUIDANCE: 0xC,
    USAGE_ASSISTANCE_SONIFICATION: 0xD,
    USAGE_ASSISTANT: 0x10,
    USAGE_GAME: 0xE,
    USAGE_MEDIA: 0x1,
    USAGE_NOTIFICATION: 0x5,
    USAGE_NOTIFICATION_COMMUNICATION_DELAYED: 0x9,
    USAGE_NOTIFICATION_COMMUNICATION_INSTANT: 0x8,
    USAGE_NOTIFICATION_COMMUNICATION_REQUEST: 0x7,
    USAGE_NOTIFICATION_EVENT: 0xA,
    USAGE_NOTIFICATION_RINGTONE: 0x6,
    USAGE_UNKNOWN: 0x0,
    USAGE_VOICE_COMMUNICATION: 0x2,
    USAGE_VOICE_COMMUNICATION_SIGNALLING: 0x3,
};

// https://developer.apple.com/documentation/avfoundation/avaudiosessionmode?language=objc
export const AvAudioSessionMode = {
    Default: 1,
    GameChat: 2,
    Measurement: 3,
    MoviePlayback: 4,
    SpokenAudio: 5,
    VideoChat: 6,
    VideoRecording: 7,
    VoiceChat: 8,
    VoicePrompt: 9,
};

// https://developer.apple.com/documentation/avfoundation/avaudiosessioncategory?language=objc
export const AvAudioSessionCategory = {
    Ambient: 1,
    MultiRoute: 2,
    PlayAndRecord: 3,
    Playback: 4,
    Record: 5,
};

// https://developer.apple.com/documentation/avfoundation/avaudiosessioncategoryoptions?language=objc
export const AvAudioSessionCategoryOptions = {
    MixWithOthers: 0x1,
    DuckOthers : 0x2,
    InterruptSpokenAudioAndMixWithOthers : 0x11,
    AllowBluetooth : 0x4,
    AllowBluetoothA2DP : 0x20,
    AllowAirPlay : 0x40,
    DefaultToSpeaker : 0x8,
};

class MediaDevices extends EventTarget(MEDIA_DEVICES_EVENTS) {
    // TODO: implement.
    ondevicechange: ?Function;

    /**
     * W3C "Media Capture and Streams" compatible {@code enumerateDevices}
     * implementation.
     */
    enumerateDevices() {
        return new Promise(resolve => WebRTCModule.enumerateDevices(resolve));
    }

    /**
     * W3C "Media Capture and Streams" compatible {@code getUserMedia}
     * implementation.
     * See: https://www.w3.org/TR/mediacapture-streams/#dom-mediadevices-enumeratedevices
     *
     * @param {*} constraints
     * @returns {Promise}
     */
    getUserMedia(constraints) {
        return getUserMedia(constraints);
    }

    useAudioOutput(audioUsageAndroid, audioUsageIos) {
        if (Platform.OS === 'android') {
            WebRTCModule.useAudioOutput(audioUsageAndroid);
        } else if (Platform.OS === 'ios') {
            WebRTCModule.useAudioOutput(audioUsageIos.mode, audioUsageIos.category, audioUsageIos.categoryOptions);
        }
    }
}

export default new MediaDevices();
