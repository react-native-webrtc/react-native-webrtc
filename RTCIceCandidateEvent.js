'use strict';

import type RTCIceCandidate from './RTCIceCandidate';

export default class RTCIceCandidateEvent {
  type: string;
  candidate: RTCIceCandidate;
  constructor(type, eventInitDict) {
    this.type = type.toString();
    Object.assign(this, eventInitDict);
  }
}
