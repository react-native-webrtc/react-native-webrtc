'use strict';

class RTCIceCandidateEvent {
  type: string;
  candidate;
  constructor(type, eventInitDict) {
    this.type = type.toString();
    Object.assign(this, eventInitDict);
  }
}

module.exports = RTCIceCandidateEvent;
