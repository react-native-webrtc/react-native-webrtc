import { Event } from "event-target-shim"
import type RTCIceCandidate from './RTCIceCandidate';

interface IRTCDataChannelEventInitDict extends Event.EventInit {
    candidate: RTCIceCandidate | null
}
  
export default class RTCIceCandidateEvent<TEventType extends string = string> extends Event<TEventType> {
    candidate: RTCIceCandidate | null;
    constructor(type, eventInitDict: IRTCDataChannelEventInitDict) {
        super(type, eventInitDict);
        this.candidate = eventInitDict?.candidate ?? null;
    }
}
