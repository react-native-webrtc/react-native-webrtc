import { Event } from 'event-target-shim';

import type RTCDataChannel from './RTCDataChannel';

type DATA_CHANNEL_EVENTS =  'open'| 'message'| 'bufferedamountlow'| 'closing'| 'close'| 'error' | 'datachannel';

interface IRTCDataChannelEventInitDict extends Event.EventInit {
    channel: RTCDataChannel;
}

export default class RTCDataChannelEvent<
TEventType extends DATA_CHANNEL_EVENTS
> extends Event<TEventType> {
    channel: RTCDataChannel;
    constructor(type: TEventType, eventInitDict: IRTCDataChannelEventInitDict) {
        super(type, eventInitDict);
        this.channel = eventInitDict.channel;
    }
}
