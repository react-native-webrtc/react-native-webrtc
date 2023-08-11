import { Event } from 'event-target-shim';
export type MessageEventData = string | ArrayBuffer | Blob;

type MESSAGE_EVENTS = 'message' | 'messageerror';

interface IMessageEventInitDict extends Event.EventInit {
    data: MessageEventData;
}

/**
 * @eventClass
 * This event is fired whenever the RTCDataChannel send message.
 * @param {MESSAGE_EVENTS} type - The type of event.
 * @param {IMessageEventInitDict} eventInitDict - The event init properties.
 * @see
 * {@link https://developer.mozilla.org/en-US/docs/Web/API/RTCDataChannel/message_event#event_type MDN} for details.
 */
export default class MessageEvent<TEventType extends MESSAGE_EVENTS> extends Event<TEventType> {
    /** @eventProperty */
    data: MessageEventData;
    constructor(type: TEventType, eventInitDict: IMessageEventInitDict) {
        super(type, eventInitDict);
        this.data = eventInitDict.data;
    }
}
