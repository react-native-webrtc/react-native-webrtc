'use strict';

export default class RTCEvent {
  type: string;
  constructor(type, eventInitDict) {
    this.type = type.toString();
    Object.assign(this, eventInitDict);
  }
}
