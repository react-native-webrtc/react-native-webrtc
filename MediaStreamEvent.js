'use strict';

import type MediaStream from './MediaStream';

export default class MediaStreamEvent {
  type: string;
  stream: MediaStream;
  constructor(type, eventInitDict) {
    this.type = type.toString();
    Object.assign(this, eventInitDict);
  }
}
