'use strict';

import {NativeModules} from 'react-native';
const {WebRTCModule} = NativeModules;
const isIOS = Platform.OS === 'ios';

export const AudioSession = {

  lockConfiguration = () => {
    if (isIOS) {
      WebRTCModule.lockConfiguration();
    }
  },
  
  unlockConfiguration = () => {
    if (isIOS) {
      WebRTCModule.unlockConfiguration();
    }
  },

  setManualAudio = (manual) => {
    if (isIOS) {
      WebRTCModule.setManualAudio(manual);
    }
  },

  isManualAudio = () => {
    if (isIOS) {
      return WebRTCModule.isManualAudio();
    }
    return false
  },

  isAudioEnabled = () => {
    if (isIOS) {
      return WebRTCModule.isAudioEnabled();
    }
    return true
  },
  
  startAudio = async () => {
    if (isIOS) {
      return await WebRTCModule.startAudio();
    }
  },
  
  stopAudio = async () => {
    if (isIOS) {
      return await WebRTCModule.stopAudio();
    }
  }
}

