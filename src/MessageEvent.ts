
export type MessageEventData = string | ArrayBuffer | Blob;

export default class MessageEvent {
    type: string;
    data: MessageEventData;
    constructor(type: string, eventInitDict: { data: MessageEventData }) {
        this.type = type.toString();
        this.data = eventInitDict.data;
    }
}
