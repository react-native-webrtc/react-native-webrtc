'use strict';

import {NativeModules} from 'react-native';

const {WebRTCModule} = NativeModules;

export default function stopAudioRecording() {
  WebRTCModule.stopAudioRecording()
}
