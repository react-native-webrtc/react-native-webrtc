import { Event } from 'event-target-shim';

// Import for documentation purposes
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import RTCIceCandidateEvent from './RTCIceCandidateEvent';

type RTC_EVENTS = 'connectionstatechange' | 'iceconnectionstatechange' | 'icegatheringstatechange' |
 'negotiationneeded' | 'signalingstatechange' | 'error'

/**
 * @eventClass
 * This event is fired whenever the RTC_EVENTS has changed except on icecandidate related.
 * @type {RTCIceCandidateEvent} for icecandidate related.
 * @param {RTC_EVENTS} type - The type of event.
 * @see {@link https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection#events MDN} for details.
 */
export default class RTCEvent<TEventType extends RTC_EVENTS> extends Event<TEventType> {
    constructor(type: TEventType, eventInitDict?: Event.EventInit) {
        super(type, eventInitDict);
    }
}
