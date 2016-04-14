'use strict';

class MediaStreamEvent {
  type: string;
  stream;
  constructor(type, eventInitDict) {
    this.type = type.toString();
    Object.assign(this, eventInitDict);
  }
}

module.exports = MediaStreamEvent;
