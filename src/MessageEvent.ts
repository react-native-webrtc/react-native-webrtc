import { Event } from 'event-target-shim';
export type MessageEventData = string | ArrayBuffer | Blob;

type MESSAGE_EVENTS = 'message' | 'messageerror';

interface IMessageEventInitDict extends Event.EventInit {
    data: MessageEventData;
}

export default class MessageEvent<TEventType extends MESSAGE_EVENTS> extends Event<TEventType> {
    data: MessageEventData;
    constructor(type: TEventType, eventInitDict: IMessageEventInitDict) {
        super(type, eventInitDict);
        this.data = eventInitDict.data;
    }
}
