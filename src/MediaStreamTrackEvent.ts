
import type MediaStreamTrack from './MediaStreamTrack';

export default class MediaStreamTrackEvent {
    type: string;
    track: MediaStreamTrack;
    constructor(type, eventInitDict: { track: MediaStreamTrack }) {
        this.type = type.toString();
        this.track = eventInitDict.track;
    }
}
