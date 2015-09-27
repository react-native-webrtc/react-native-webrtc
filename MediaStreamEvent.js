'use strict';

class MediaStreamEvent {
  type: string;
  target: any;
  stream: stream;
  constructor(type, eventInit) {
    this.type = type;
    this.target = eventInit.target;
    this.stream = eventInit.stream;
  }
}

module.exports = MediaStreamEvent;
