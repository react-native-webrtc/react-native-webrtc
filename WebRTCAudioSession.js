'use strict';

import { NativeModules, Platform } from 'react-native';
const WebRTCAudioSessionModule = NativeModules.WebRTCAudioSession;
const isIOS = Platform.OS === 'ios';

const WebRTCAudioSession = {

  lockConfiguration() {
    if (isIOS) {
      WebRTCAudioSessionModule.lockForConfiguration();
    }
  },
  
  unlockConfiguration() {
    if (isIOS) {
      WebRTCAudioSessionModule.unlockForConfiguration();
    }
  },

  setManualAudio(manual) {
    if (isIOS) {
      WebRTCAudioSessionModule.setManualAudio(manual);
    }
  },

  isManualAudio() {
    if (isIOS) {
      return WebRTCAudioSessionModule.isManualAudio();
    }
    return false
  },

  isAudioEnabled() {
    if (isIOS) {
      return WebRTCAudioSessionModule.isAudioEnabled();
    }
    return true
  },
  
  startAudio() {
    if (isIOS) {
      return WebRTCAudioSessionModule.startAudio();
    }
  },
  
  stopAudio() {
    if (isIOS) {
      return WebRTCAudioSessionModule.stopAudio();
    }
  }
}

export default WebRTCAudioSession;

