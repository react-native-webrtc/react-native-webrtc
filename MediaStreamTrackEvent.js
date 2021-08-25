'use strict';

import type MediaStreamTrack from './MediaStreamTrack';
import type MediaStream from './MediaStream';

export default class MediaStreamTrackEvent {
  type: string;
  track: MediaStreamTrack;
  streams: MediaStream[];
  constructor(type, eventInitDict) {
    this.type = type.toString();
    Object.assign(this, eventInitDict);
  }
}
