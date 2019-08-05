'use strict';

import { NativeModules, Platform } from 'react-native';
const { WebRTCAudioSession } = NativeModules;
const isIOS = Platform.OS === 'ios';

export const AudioSession = {

  lockConfiguration() {
    if (isIOS) {
      WebRTCAudioSession.lockForConfiguration();
    }
  },
  
  unlockConfiguration() {
    if (isIOS) {
      WebRTCAudioSession.unlockForConfiguration();
    }
  },

  setManualAudio(manual) {
    if (isIOS) {
      WebRTCAudioSession.setManualAudio(manual);
    }
  },

  isManualAudio() {
    if (isIOS) {
      return WebRTCAudioSession.isManualAudio();
    }
    return false
  },

  isAudioEnabled() {
    if (isIOS) {
      return WebRTCAudioSession.isAudioEnabled();
    }
    return true
  },
  
  startAudio() {
    if (isIOS) {
      return WebRTCAudioSession.startAudio();
    }
  },
  
  stopAudio() {
    if (isIOS) {
      return WebRTCAudioSession.stopAudio();
    }
  }
}

