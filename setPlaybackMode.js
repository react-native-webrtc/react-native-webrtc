
import { Platform } from 'react-native';
import {NativeModules} from 'react-native';

const { PlaybackModeModule } = NativeModules;

/**
 * Sets the play back mode for webrtc library. 
 * Setting the playback mode disables microphone permission when viewing webrtc stream
 * @param {boolean} isPlaybackMode 
 */
export default function setPlaybackMode(isPlaybackMode) {
    if (Platform.OS === "ios") {
        if(isPlaybackMode) {
            PlaybackModeModule.setPlaybackMode();
        } else {
            PlaybackModeModule.resetPlaybackMode();
        } 
    }
}