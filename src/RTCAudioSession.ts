import { Platform } from 'react-native';

import WebRTCModule from './NativeWebRTCModule';

export default class RTCAudioSession {
    /**
     * To be called when CallKit activates the audio session.
     */
    static audioSessionDidActivate() {
        // Only valid for iOS
        if (Platform.OS === 'ios') {
            WebRTCModule.audioSessionDidActivate();
        }
    }

    /**
     * To be called when CallKit deactivates the audio session.
     */
    static audioSessionDidDeactivate() {
        // Only valid for iOS
        if (Platform.OS === 'ios') {
            WebRTCModule.audioSessionDidDeactivate();
        }
    }
}
