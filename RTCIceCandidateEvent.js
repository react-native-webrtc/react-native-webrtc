'use strict';

class RTCIceCandidateEvent {
  type: string;
  target;
  candidate;
  constructor(type, eventInitDict) {
    this.type = type.toString();
    Object.assign(this, eventInitDict);
  }
}

module.exports = RTCIceCandidateEvent;
