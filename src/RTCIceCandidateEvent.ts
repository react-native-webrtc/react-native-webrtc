
import type RTCIceCandidate from './RTCIceCandidate';

export default class RTCIceCandidateEvent {
    type: string;
    candidate: RTCIceCandidate | null;
    constructor(type, eventInitDict) {
        this.type = type.toString();
        this.candidate = eventInitDict?.candidate ?? null;
    }
}
