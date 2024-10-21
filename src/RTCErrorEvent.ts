import { Event } from 'event-target-shim';

type RTCPeerConnectionErrorFunc =
    | 'addTransceiver'
    | 'getTransceivers'
    | 'addTrack'
    | 'removeTrack';

/**
 * @brief This class Represents internal error happening on the native side as
 * part of asynchronous invocations to synchronous web APIs.
 */
export default class RTCErrorEvent<TEventType extends RTCPeerConnectionErrorFunc> extends Event<TEventType> {
    readonly func: RTCPeerConnectionErrorFunc;
    readonly message: string;
    constructor(type: TEventType, func: RTCPeerConnectionErrorFunc, message: string) {
        super(type);
        this.func = func;
        this.message = message;
    }
}