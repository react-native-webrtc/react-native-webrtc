'use strict';

import { NativeModules, Platform } from 'react-native';
const {RCTAudioSession} = NativeModules;
const isIOS = Platform.OS === 'ios';

export const AudioSession = {

  lockConfiguration() {
    if (isIOS) {
      RCTAudioSession.lockForConfiguration();
    }
  },
  
  unlockConfiguration() {
    if (isIOS) {
      RCTAudioSession.unlockForConfiguration();
    }
  },

  setManualAudio(manual) {
    if (isIOS) {
      RCTAudioSession.setManualAudio(manual);
    }
  },

  isManualAudio() {
    if (isIOS) {
      return RCTAudioSession.isManualAudio();
    }
    return false
  },

  isAudioEnabled() {
    if (isIOS) {
      return RCTAudioSession.isAudioEnabled();
    }
    return true
  },
  
  startAudio() {
    if (isIOS) {
      return RCTAudioSession.startAudio();
    }
  },
  
  stopAudio() {
    if (isIOS) {
      return RCTAudioSession.stopAudio();
    }
  }
}

