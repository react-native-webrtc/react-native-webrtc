import MediaStreamTrack from './MediaStreamTrack';

export default class RTCRtpReceiver {
    id: string;
    track: MediaStreamTrack;
    
    constructor(id: string, track: MediaStreamTrack) {
        this.id = id;
        this.track = track;
        Object.freeze(this);
    }
}