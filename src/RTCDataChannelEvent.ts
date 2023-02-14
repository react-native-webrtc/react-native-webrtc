
import type RTCDataChannel from './RTCDataChannel';

export default class RTCDataChannelEvent {
    type: string;
    channel: RTCDataChannel;
    constructor(type, eventInitDict: { channel: RTCDataChannel }) {
        this.type = type.toString();
        this.channel = eventInitDict.channel;
    }
}
