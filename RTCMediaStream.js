'use strict';

var EventTarget = require('event-target-shim');
var React = require('react-native');
var {
  DeviceEventEmitter,
  NativeModules,
} = React;
var WebRTCModule = NativeModules.WebRTCModule;

const MEDIA_STREAM_EVENTS = [
  'active',
  'inactive',
  'addtrack',
  'removetrack',
];

class RTCMediaStream extends EventTarget(MEDIA_STREAM_EVENTS) {
  _streamId: number;
  constructor(_streamId) {
    super();
    this._streamId = _streamId;
    this.tracks = [];
  }
  toURL() {
    return this._streamId;
  }
  addTrack(track) {
    this.tracks.push(track);
  }
  getTracks() {
    return this.tracks;
  }
  getAudioTracks() {
    return this.tracks.filter(t => t.kind == 'audio');
  }
  getVideoTracks() {
    return this.tracks.filter(t => t.kind == 'video');
  }
  release() {
    WebRTCModule.mediaStreamRelease(this._streamId);
  }
}

module.exports = RTCMediaStream;
