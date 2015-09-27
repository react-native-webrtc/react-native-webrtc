'use strict';

class RTCIceCandidateEvent {
  type: string;
  target: any;
  candidate: candidate;
  constructor(type, eventInit) {
    this.type = type;
    this.target = eventInit.target;
    this.candidate = eventInit.candidate;
  }
}

module.exports = RTCIceCandidateEvent;
