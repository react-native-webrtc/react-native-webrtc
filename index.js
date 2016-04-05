'use strict';

require('./getUserMedia');
var RTCPeerConnection = require('./RTCPeerConnection');
var RTCMediaStream = require('./RTCMediaStream');
var RTCIceCandidate = require('./RTCIceCandidate');
var RTCSessionDescription = require('./RTCSessionDescription');
var RTCView = require('./RTCView');
var RTCSetting = require('./RTCSetting');
var MediaStreamTrack = require('./MediaStreamTrack');
var getUserMedia = require('./getUserMedia');

var WebRTC = {
  RTCPeerConnection,
  RTCMediaStream,
  RTCIceCandidate,
  RTCSessionDescription,
  RTCView,
  RTCSetting,
  MediaStreamTrack,
  getUserMedia,
};

module.exports = WebRTC;
