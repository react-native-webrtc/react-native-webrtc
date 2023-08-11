import { Event } from 'event-target-shim';
export type MessageEventData = string | ArrayBuffer | Blob;

interface IMessageEventInitDict extends Event.EventInit {
    data: MessageEventData;
}

export default class MessageEvent<TEventType extends string = string> extends Event<TEventType> {
    data: MessageEventData;
    constructor(type, eventInitDict: IMessageEventInitDict) {
        super(type, eventInitDict);
        this.data = eventInitDict.data;
    }
}
