import { Event } from 'event-target-shim';

export default class RTCEvent<TEventType extends string = string> extends Event<TEventType> {
    constructor(type, eventInitDict?: Event.EventInit) {
        super(type, eventInitDict);
    }
}
