
import {NativeModules} from 'react-native';

const { WebRTCModule } = NativeModules;

/**
 * Sets the play back mode for webrtc library. 
 * Setting the playback mode disables microphone permission when viewing webrtc stream
 * @param {boolean} isPlaybackMode 
 */
export function setPlaybackMode(isPlaybackMode) {
    WebRTCModule.setPlaybackMode(isPlaybackMode);
}