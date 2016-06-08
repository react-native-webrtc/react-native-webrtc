'use strict';

class RTCIceCandidateEvent {
  type: string;
  candidate;
  constructor(type, eventInitDict) {
    this.type = type.toString();

    this.candidate = null;
    if (eventInitDict && eventInitDict.candidate) {
      this.candidate = eventInitDict.candidate;
    }
  }
}

module.exports = RTCIceCandidateEvent;
