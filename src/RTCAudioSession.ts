import { NativeModules, Platform } from 'react-native';

const { WebRTCModule } = NativeModules;

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
