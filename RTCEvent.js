'use strict';

class RTCEvent {
  type: string;
  target: any;
  constructor(type, eventInit) {
    this.type = type;
    this.target = eventInit.target;
  }
}

module.exports = RTCEvent;
