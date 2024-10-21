import { Event } from 'event-target-shim';

import type RTCIceCandidate from './RTCIceCandidate';

type RTC_ICECANDIDATE_EVENTS = 'icecandidate' | 'icecandidateerror'

interface IRTCDataChannelEventInitDict extends Event.EventInit {
    candidate: RTCIceCandidate | null
}

/**
 * @eventClass
 * This event is fired whenever the icecandidate related RTC_EVENTS changed.
 * @type {RTCIceCandidateEvent} for icecandidate related.
 * @param {RTC_ICECANDIDATE_EVENTS} type - The type of event.
 * @param {IRTCDataChannelEventInitDict} eventInitDict - The event init properties.
 * @see {@link https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection#events MDN} for details.
 */
export default class RTCIceCandidateEvent<TEventType extends RTC_ICECANDIDATE_EVENTS> extends Event<TEventType> {
    /** @eventProperty */
    candidate: RTCIceCandidate | null;
    constructor(type: TEventType, eventInitDict: IRTCDataChannelEventInitDict) {
        super(type, eventInitDict);
        this.candidate = eventInitDict?.candidate ?? null;
    }
}
