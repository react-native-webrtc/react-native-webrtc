'use strict';

export default class RTCIceCandidate {
  candidate: string;
  sdpMLineIndex: number;
  sdpMid: string;

  constructor(info) {
    this.candidate = info.candidate;
    this.sdpMLineIndex = info.sdpMLineIndex;
    this.sdpMid = info.sdpMid;
  }

  toJSON() {
    return {
      candidate: this.candidate,
      sdpMLineIndex: this.sdpMLineIndex,
      sdpMid: this.sdpMid,
    };
  }
}
