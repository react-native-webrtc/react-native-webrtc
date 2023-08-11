import { Event } from 'event-target-shim';

import type RTCIceCandidate from './RTCIceCandidate';

type RTC_ICECANDIDATE_EVENTS = 'icecandidate' | 'icecandidateerror'

interface IRTCDataChannelEventInitDict extends Event.EventInit {
    candidate: RTCIceCandidate | null
}

export default class RTCIceCandidateEvent<TEventType extends RTC_ICECANDIDATE_EVENTS> extends Event<TEventType> {
    candidate: RTCIceCandidate | null;
    constructor(type: TEventType, eventInitDict: IRTCDataChannelEventInitDict) {
        super(type, eventInitDict);
        this.candidate = eventInitDict?.candidate ?? null;
    }
}
