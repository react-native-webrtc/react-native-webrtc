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
  }
  toURL() {
    return this._streamId;
  }
  release() {
    WebRTCModule.mediaStreamRelease(this._streamId);
  }
}

module.exports = RTCMediaStream;
