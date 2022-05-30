
import MediaStream from './MediaStream';
import type MediaStreamTrack from './MediaStreamTrack';
import RTCRtpReceiver from './RTCRtpReceiver';
import RTCRtpTransceiver from './RTCRtpTransceiver';

export default class RTCTrackEvent {
    type: string;
    track: MediaStreamTrack;
    streams: MediaStream[] = [];
    transceiver: RTCRtpTransceiver;
    receiver: RTCRtpReceiver;

    constructor(type: string, eventInitDict: { track: MediaStreamTrack, streams: MediaStream[], transceiver: RTCRtpTransceiver, receiver: RTCRtpReceiver}) {
        this.type = type.toString();
        this.track = eventInitDict.track;
        this.streams = eventInitDict.streams;
        this.transceiver = eventInitDict.transceiver;
        this.receiver = eventInitDict.receiver;
    }
}
