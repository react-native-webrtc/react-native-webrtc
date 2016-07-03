'use strict';

import type RTCDataChannel from './RTCDataChannel';

export default class RTCDataChannelEvent {
  type: string;
  channel: RTCDataChannel;
  constructor(type, eventInitDict) {
    this.type = type.toString();
    Object.assign(this, eventInitDict);
  }
}
