'use strict';

class RTCMediaStream {
  _streamId: number;
  constructor(_streamId) {
    this._streamId = _streamId;
  }
  toURL() {
    return this._streamId;
  }
}

module.exports = RTCMediaStream;
