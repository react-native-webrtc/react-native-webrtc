import { NativeModules } from 'react-native';

import RTCRtpReceiver from './RTCRtpReceiver';
import RTCRtpSender from './RTCRtpSender';

const { WebRTCModule } = NativeModules;

export default class RTCRtpTransceiver {
    _peerConnectionId: number;
    _sender: RTCRtpSender;
    _receiver: RTCRtpReceiver;

    _mid: string | null = null;
    _direction: string;
    _currentDirection: string;
    _stopped: boolean;

    constructor(args: {
        peerConnectionId: number,
        isStopped: boolean,
        direction: string,
        currentDirection: string,
        mid?: string,
        sender: RTCRtpSender,
        receiver: RTCRtpReceiver,
    }) {
        this._peerConnectionId = args.peerConnectionId;
        this._mid = args.mid ?? null;
        this._direction = args.direction;
        this._currentDirection = args.currentDirection ?? null;
        this._stopped = Boolean(args.isStopped);
        this._sender = args.sender;
        this._receiver = args.receiver;
    }

    get mid() {
        return this._mid;
    }

    get stopped() {
        return this._stopped;
    }

    get direction() {
        return this._direction;
    }

    set direction(val) {
        if (![ 'sendonly', 'recvonly', 'sendrecv', 'inactive' ].includes(val)) {
            throw new TypeError('Invalid direction provided');
        }

        if (this._stopped) {
            throw new Error('Transceiver Stopped');
        }

        if (this._direction === val) {
            return;
        }

        const oldDirection = this._direction;

        WebRTCModule.transceiverSetDirection(this._peerConnectionId, this.sender.id, val)
            .catch(() => {
                this._direction = oldDirection;
            });

        this._direction = val;
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

        WebRTCModule.transceiverStop(this._peerConnectionId, this.sender.id)
            .then(() => this._setStopped());
    }

    _setStopped() {
        this._stopped = true;
        this._direction = 'stopped';
        this._currentDirection = 'stopped';
        this._mid = null;
    }
}
