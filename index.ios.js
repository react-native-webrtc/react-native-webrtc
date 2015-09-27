'use strict';
var WebRTCManager = require('react-native').NativeModules.WebRTCManager;

require('./getUserMedia');
var RTCPeerConnection = require('./RTCPeerConnection');
var RTCMediaStream = require('./RTCMediaStream');
var RTCIceCandidate = require('./RTCIceCandidate');
var RTCSessionDescription = require('./RTCSessionDescription');
var RTCView = require('./RTCView');

var WebRTC = {
  "RTCPeerConnection": RTCPeerConnection,
  "RTCMediaStream" : RTCMediaStream,
  "RTCIceCandidate": RTCIceCandidate,
  "RTCSessionDescription": RTCSessionDescription,
  "RTCView": RTCView
}

module.exports = WebRTC;
