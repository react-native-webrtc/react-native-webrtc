import { Event } from 'event-target-shim';

import type MediaStreamTrack from './MediaStreamTrack';

type MEDIA_STREAM_EVENTS =  'active'| 'inactive'| 'addtrack'| 'removetrack'

interface IMediaStreamTrackEventInitDict extends Event.EventInit {
  track: MediaStreamTrack;
}

export default class MediaStreamTrackEvent<TEventType extends MEDIA_STREAM_EVENTS> extends Event<TEventType> {
    track: MediaStreamTrack;
    constructor(type: TEventType, eventInitDict: IMediaStreamTrackEventInitDict) {
        super(type, eventInitDict);
        this.track = eventInitDict.track;
    }
}
