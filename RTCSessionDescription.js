'use strict';

export default class RTCSessionDescription {
  sdp: string;
  type: string;

  constructor(info = {type: null, sdp: ''}) {
    this.sdp = info.sdp;
    this.type = info.type;
  }
  toJSON() {
    return {sdp: this.sdp, type: this.type};
  }
}
