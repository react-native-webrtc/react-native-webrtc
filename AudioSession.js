'use strict';

import { NativeModules, Platform } from 'react-native';
const {RCTAudioSession} = NativeModules;
const isIOS = Platform.OS === 'ios';

export const AudioSession = {

  lockConfiguration = () => {
    if (isIOS) {
      RCTAudioSession.lockForConfiguration();
    }
  },
  
  unlockConfiguration = async () => {
    if (isIOS) {
      RCTAudioSession.unlockForConfiguration();
    }
  },

  setManualAudio = async (manual) => {
    if (isIOS) {
      RCTAudioSession.setManualAudio(manual);
    }
  },

  isManualAudio = async () => {
    if (isIOS) {
      return RCTAudioSession.isManualAudio();
    }
    return false
  },

  isAudioEnabled = async () => {
    if (isIOS) {
      return await RCTAudioSession.isAudioEnabled();
    }
    return true
  },
  
  startAudio = async () => {
    if (isIOS) {
      await RCTAudioSession.startAudio();
    }
  },
  
  stopAudio = async () => {
    if (isIOS) {
      await RCTAudioSession.stopAudio();
    }
  }
}

