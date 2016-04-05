'use strict';

var React = require('react-native');
var {
  DeviceEventEmitter,
  NativeModules,
} = React;
var WebRTCModule = NativeModules.WebRTCModule;

class RTCMediaStream {
  _streamId: number;
  constructor(_streamId) {
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
