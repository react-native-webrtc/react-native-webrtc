import { defineCustomEventTarget } from 'event-target-shim';
import { NativeModules } from 'react-native';

import { addListener, removeListener } from './EventEmitter';
import RTCErrorEvent from './RTCErrorEvent';
import RTCRtpReceiver from './RTCRtpReceiver';
import RTCRtpSender from './RTCRtpSender';

const { WebRTCModule } = NativeModules;

const TRANSCEIVER_EVENTS = [
    'error'
];

export default class RTCRtpTransceiver extends defineCustomEventTarget(...TRANSCEIVER_EVENTS) {
    _peerConnectionId: number;
    _sender: RTCRtpSender;
    _receiver: RTCRtpReceiver;

    _id: string;
    _mid: string | null = null;
    _direction: string;
    _currentDirection: string;
    _stopped: boolean;

    constructor(args: {
        peerConnectionId: number,
        id: string,
        isStopped: boolean,
        direction: string,
        currentDirection: string,
        mid?: string,
        sender: RTCRtpSender,
        receiver: RTCRtpReceiver,
    }) {
        super();
        this._peerConnectionId = args.peerConnectionId;
        this._id = args.id;
        this._mid = args.mid ? args.mid : null;
        this._direction = args.direction;
        this._currentDirection = args.currentDirection;
        this._stopped = args.isStopped;
        this._sender = args.sender;
        this._receiver = args.receiver;
        this._registerEvents();
    }

    get id() {
        return this._id;
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
            throw Error('Transceiver Stopped');
        }

        if (this._direction === val) {
            return;
        }

        WebRTCModule.transceiverSetDirection(this._peerConnectionId, this.id, val);
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

        WebRTCModule.transceiverStop(this._peerConnectionId, this.id);
    }

    _registerEvents(): void {
        addListener(this, 'transceiverStopSuccessful', (ev: any) => {
            if (ev.peerConnectionId !== this._peerConnectionId || ev.transceiverId !== this._id) {
                return;
            }

            this._stopped = true;
            this._direction = 'stopped';
            this._currentDirection = 'stopped';
            this._mid = null;
            removeListener(this);
        });

        addListener(this, 'transceiverOnError', (ev: any) => {
            if (ev.info.peerConnectionId !== this._peerConnectionId || ev.info.transceiverId !== this._id) {
                return;
            }

            if (ev.func === 'stopTransceiver') {
                this._stopped = false;
            } else if (ev.func === 'setDirection') {
                this._direction = ev.info.oldDirection;
            }

            // @ts-ignore
            this.dispatchEvent(new RTCErrorEvent('error', ev.func, ev.message));
        });
    }
}
