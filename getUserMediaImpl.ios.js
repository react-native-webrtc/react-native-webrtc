'use strict';

var WebRTCManager = require('react-native').NativeModules.WebRTCManager;
var RTCMediaStream = require('./RTCMediaStream');

var getUserMediaImpl = function(constraints, success, failure) { // TODO: success, failure
  WebRTCManager.getUserMedia(constraints, (id) => {
    var stream = new RTCMediaStream(id);
    success(stream);
  });
}

module.exports = getUserMediaImpl;
