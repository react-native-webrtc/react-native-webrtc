'use strict';

var WebRTCModule = require('react-native').NativeModules.WebRTCModule;
var RTCMediaStream = require('./RTCMediaStream');

var getUserMediaImpl = function(constraints, success, failure) { // TODO: success, failure
  WebRTCModule.getUserMedia(constraints, (id) => {
    var stream = new RTCMediaStream(id);
    success(stream);
  });
}

module.exports = getUserMediaImpl;
