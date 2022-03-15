
import type MediaStream from './MediaStream';

export default class MediaStreamEvent {
    type: string;
    stream: MediaStream;
    constructor(type, eventInitDict: { stream: MediaStream }) {
        this.type = type.toString();
        this.stream = eventInitDict.stream;
    }
}
