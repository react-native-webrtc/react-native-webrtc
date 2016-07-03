'use strict';

import {NativeModules} from 'react-native';

import MediaStream from './MediaStream';
import MediaStreamTrack from './MediaStreamTrack';

const {WebRTCModule} = NativeModules;

export default function getUserMedia(constraints, success, failure) {
  WebRTCModule.getUserMedia(constraints, (id, tracks) => {
    const stream = new MediaStream(id);
    for (let i = 0; i < tracks.length; i++) {
      stream.addTrack(new MediaStreamTrack(tracks[i]));
    }

    success(stream);
  });
}
