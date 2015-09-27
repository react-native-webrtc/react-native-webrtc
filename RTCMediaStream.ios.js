'use strict';

var RTCMediaStreamBase = require('./RTCMediaStreamBase');

class RTCMediaStream extends RTCMediaStreamBase {
  objectId: number;
  constructorImpl(paramters) {
    var firstParamter = paramters[0];
    this.objectId = firstParamter;
  }
}

module.exports = RTCMediaStream;
