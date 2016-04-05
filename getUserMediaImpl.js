'use strict';

var WebRTCModule = require('react-native').NativeModules.WebRTCModule;
var RTCMediaStream = require('./RTCMediaStream');
var MediaStreamTrack = require('./MediaStreamTrack');

var getUserMediaImpl = function(constraints, success, failure) { // TODO: success, failure
  WebRTCModule.getUserMedia(constraints, (id, tracks) => {
    var stream = new RTCMediaStream(id);
    for (var i = 0; i < tracks.length; i++) {
      stream.addTrack(new MediaStreamTrack(tracks[i]));
    }

    success(stream);
  });
}

module.exports = getUserMediaImpl;
