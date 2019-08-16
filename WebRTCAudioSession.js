'use strict';

import { NativeModules, Platform } from 'react-native';
const WebRTCAudioSessionModule = NativeModules.WebRTCAudioSession;
const isIOS = Platform.OS === 'ios';

const WebRTCAudioSession = {

  isEngaged() {
    if (isIOS) {
      return WebRTCAudioSessionModule.isEngaged();
    }
    return false
  },

  engageVOIPMode() {
    if (isIOS) {
      return WebRTCAudioSessionModule.engageVoipAudioSession();
    }
  },

  engageVideoMode() {
    if (isIOS) {
      return WebRTCAudioSessionModule.engageVideoAudioSession();
    }
  },

  disengage() {
    if (isIOS) {
      return WebRTCAudioSessionModule.disengageAudioSession();
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

