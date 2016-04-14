'use strict';

import type MediaStreamTrack from './MediaStreamTrack';

class MediaStreamTrackEvent {
  type: string;
  track: MediaStreamTrack;
  constructor(type, eventInitDict) {
    this.type = type.toString();
    Object.assign(this, eventInitDict);
  }
}

module.exports = MediaStreamTrackEvent;
