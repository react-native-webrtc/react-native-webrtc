'use strict';

import type MediaStreamError from './MediaStreamError';

class MediaStreamErrorEvent {
  type: string;
  error: ?MediaStreamError;
  constructor(type, eventInitDict) {
    this.type = type.toString();
    Object.assign(this, eventInitDict);
  }
}

module.exports = MediaStreamErrorEvent;
