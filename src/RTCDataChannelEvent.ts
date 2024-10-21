import { Event } from 'event-target-shim';

import type RTCDataChannel from './RTCDataChannel';

type DATA_CHANNEL_EVENTS =  'open'| 'message'| 'bufferedamountlow'| 'closing'| 'close'| 'error' | 'datachannel';

interface IRTCDataChannelEventInitDict extends Event.EventInit {
    channel: RTCDataChannel;
}


/**
 * @eventClass
 * This event is fired whenever the RTCDataChannel has changed in any way.
 * @param {DATA_CHANNEL_EVENTS} type - The type of event.
 * @param {IRTCDataChannelEventInitDict} eventInitDict - The event init properties.
 * @see {@link https://developer.mozilla.org/en-US/docs/Web/API/RTCDataChannel#events MDN} for details.
 */
export default class RTCDataChannelEvent<
TEventType extends DATA_CHANNEL_EVENTS
> extends Event<TEventType> {
    /** @eventProperty */
    channel: RTCDataChannel;
    constructor(type: TEventType, eventInitDict: IRTCDataChannelEventInitDict) {
        super(type, eventInitDict);
        this.channel = eventInitDict.channel;
    }
}
