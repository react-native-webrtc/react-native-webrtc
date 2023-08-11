import { Event } from 'event-target-shim';

type RTC_EVENTS = 'connectionstatechange' | 'iceconnectionstatechange' | 'icegatheringstatechange' |
 'negotiationneeded' | 'signalingstatechange' | 'error'

export default class RTCEvent<TEventType extends RTC_EVENTS> extends Event<TEventType> {
    constructor(type: TEventType, eventInitDict?: Event.EventInit) {
        super(type, eventInitDict);
    }
}
