import { Event } from 'event-target-shim';

import MediaStream from './MediaStream';
import type MediaStreamTrack from './MediaStreamTrack';
import RTCRtpReceiver from './RTCRtpReceiver';
import RTCRtpTransceiver from './RTCRtpTransceiver';

interface IRTCTrackEventInitDict extends Event.EventInit {
    streams: MediaStream[]
    transceiver: RTCRtpTransceiver
}

export default class RTCTrackEvent<TEventType extends string = string> extends Event<TEventType> {
    readonly streams: MediaStream[] = [];
    readonly transceiver: RTCRtpTransceiver;
    readonly receiver: RTCRtpReceiver | null;
    readonly track: MediaStreamTrack | null;

    constructor(type, eventInitDict: IRTCTrackEventInitDict) {
        super(type, eventInitDict);
        this.streams = eventInitDict.streams;
        this.transceiver = eventInitDict.transceiver;
        this.receiver = eventInitDict.transceiver.receiver;
        this.track = eventInitDict.transceiver.receiver ? eventInitDict.transceiver.receiver.track : null;
    }
}
