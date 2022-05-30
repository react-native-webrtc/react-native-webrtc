import {NativeModules} from 'react-native';
import RTCRtpSender from './RTCRtpSender';
import RTCRtpReceiver from './RTCRtpReceiver';

const {WebRTCModule} = NativeModules;

export default class RTCRtpTransceiver {
    _peerConnectionId: number;
    _sender: RTCRtpSender | null;
    _receiver: RTCRtpReceiver

    _id: string;
    _mid: string | null;
    _direction: string;
    _currentDirection: string;
    _stopped: boolean;

    constructor(args: {
        pId: number,
        id: string,
        isStopped: boolean,
        direction: string,
        currentDirection: string,
        mid?: string, 
        sender: RTCRtpSender,
        receiver: RTCRtpReceiver,
    }) {
        this._peerConnectionId = args.pId;
        this._id = args.id;
        this._mid = args.mid ? args.mid : null;
        this._direction = args.direction;
        this._currentDirection = args.currentDirection;
        this._stopped = args.isStopped;
        this._sender = args.sender? new RTCRtpSender({ pId: args.pId, id: args.sender.id, track: args.sender.track }) : null;
        this._receiver = new RTCRtpReceiver({ id: args.id, track: args.receiver.track });
    }

    get id() {
        return this._id;
    }

    get mid() {
        return this._mid;
    }

    get isStopped() {
        return this._stopped;
    }

    get direction() {
        return this._direction;
    }

    set direction(val) {
        if (this._stopped) {
            throw Error('Transceiver Stopped');
        }

        if (this._direction === val) {
            return;
        }

        const successful = WebRTCModule.peerConnectionTransceiverSetDirection(this._peerConnectionId, this.id, val)
        if (successful) {
            this._currentDirection = val;
            this._direction = val;
        }
    }

    get currentDirection() {
        return this._currentDirection;
    }

    get sender() {
        return this._sender;
    }

    get receiver() {
        return this._receiver;
    }

    stop() {
        if (this._stopped) {
            return;
        }
        const successful = WebRTCModule.peerConnectionTransceiverStop(this._peerConnectionId, this.id);
        if (successful)
            this._stopped = true;
    }
}