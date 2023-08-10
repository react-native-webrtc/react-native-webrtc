import { Event } from "event-target-shim"
import type RTCDataChannel from './RTCDataChannel';

type Options = {
    [K in "noImplicitAny" | "strictNullChecks" | "strictFunctionTypes"]?: boolean
}
interface IRTCDataChannelEventInitDict extends Event.EventInit {
    channel: RTCDataChannel;
}

export default class RTCDataChannelEvent<
TEventType extends string = string
> extends Event<TEventType> {
    channel: RTCDataChannel;
    constructor(type, eventInitDict: IRTCDataChannelEventInitDict) {
        super(type, eventInitDict);
        this.channel = eventInitDict.channel;
    }
}
