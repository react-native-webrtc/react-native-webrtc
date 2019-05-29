'use strict';

import MediaStreamTrack from './MediaStreamTrack';

class RTCRtpSender {
  id: string;
  track: MediaStreamTrack;

  constructor(info) {
    this.id = info.senderId;
    this.track = new MediaStreamTrack(info.track);
  }
}

export default RTCRtpSender;
