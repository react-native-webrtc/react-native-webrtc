import RTCEvent from './RTCEvent';

type RTCPeerConnectionErrorFunc =
    | 'addTransceiver'
    | 'getTransceivers'
    | 'addTrack'
    | 'removeTrack';

/**
 * @brief This class Represents internal error happening on the native side as
 * part of asynchronous invocations to synchronous web APIs.
 */
export default class RTCErrorEvent extends RTCEvent {
    readonly func: RTCPeerConnectionErrorFunc;
    readonly message: string;
    constructor(type: string, func: RTCPeerConnectionErrorFunc, message: string) {
        super(type);
        this.func = func;
        this.message = message;
    }
}