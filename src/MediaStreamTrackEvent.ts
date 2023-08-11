import { Event } from 'event-target-shim';

import type MediaStreamTrack from './MediaStreamTrack';

interface IMediaStreamTrackEventInitDict extends Event.EventInit {
  track: MediaStreamTrack;
}

export default class MediaStreamTrackEvent<TEventType extends string = string> extends Event<TEventType> {
    track: MediaStreamTrack;
    constructor(type, eventInitDict: IMediaStreamTrackEventInitDict) {
        super(type, eventInitDict);
        this.track = eventInitDict.track;
    }
}
