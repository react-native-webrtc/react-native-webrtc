'use strict';

class RTCEvent {
  type: string;
  constructor(type, eventInitDict) {
    this.type = type.toString();
    Object.assign(this, eventInitDict);
  }
}

module.exports = RTCEvent;
