'use strict';

require('./getUserMedia');
var RTCPeerConnection = require('./RTCPeerConnection');
var RTCMediaStream = require('./RTCMediaStream');
var RTCIceCandidate = require('./RTCIceCandidate');
var RTCSessionDescription = require('./RTCSessionDescription');
var RTCView = require('./RTCView');

var WebRTC = {
  RTCPeerConnection,
  RTCMediaStream,
  RTCIceCandidate,
  RTCSessionDescription,
  RTCView,
};

module.exports = WebRTC;
