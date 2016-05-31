'use strict';

import {
  NativeModules,
} from 'react-native';
const WebRTCModule = NativeModules.WebRTCModule;

import MediaStream from './MediaStream';
import MediaStreamTrack from './MediaStreamTrack';

function getUserMedia(constraints, success, failure) {
  WebRTCModule.getUserMedia(constraints, (id, tracks) => {
    const stream = new MediaStream(id);
    for (let i = 0; i < tracks.length; i++) {
      stream.addTrack(new MediaStreamTrack(tracks[i]));
    }

    success(stream);
  });
}

module.exports = getUserMedia;
