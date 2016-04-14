'use strict';

const WebRTCModule = require('react-native').NativeModules.WebRTCModule;
const MediaStream = require('./MediaStream');
const MediaStreamTrack = require('./MediaStreamTrack');

function getUserMedia(constraints, success, failure) {
  WebRTCModule.getUserMedia(constraints, (id, tracks) => {
    const stream = new MediaStream(id);
    for (var i = 0; i < tracks.length; i++) {
      stream.addTrack(new MediaStreamTrack(tracks[i]));
    }

    success(stream);
  });
}

module.exports = getUserMedia;
