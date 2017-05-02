'use strict';

import {NativeModules} from 'react-native';

import MediaStream from './MediaStream';
import MediaStreamError from './MediaStreamError';
import MediaStreamTrack from './MediaStreamTrack';

const {WebRTCModule} = NativeModules;

export default function getEmptyVideoStream() {
  return new Promise((resolve, reject) => {
    WebRTCModule.getEmptyVideoStream((id, tracks) => {
      const stream = new MediaStream(id);
      for (const track of tracks) {
        stream.addTrack(new MediaStreamTrack(track));
      }
      resolve(stream);
    }, reject);
  });
}



