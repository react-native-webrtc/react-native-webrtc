'use strict';

var RTCPeerConnection = require('./RTCPeerConnection');
var RTCIceCandidate = require('./RTCIceCandidate');
var RTCSessionDescription = require('./RTCSessionDescription');
var RTCView = require('./RTCView');
var RTCSetting = require('./RTCSetting');
var MediaStream = require('./MediaStream');
var MediaStreamTrack = require('./MediaStreamTrack');
var getUserMedia = require('./getUserMedia');

var WebRTC = {
  RTCPeerConnection,
  RTCIceCandidate,
  RTCSessionDescription,
  RTCView,
  RTCSetting,
  MediaStream,
  MediaStreamTrack,
  getUserMedia,
};

module.exports = WebRTC;
