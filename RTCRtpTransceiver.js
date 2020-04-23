import {NativeModules} from 'react-native';
import RTCRtpSender from './RTCRtpSender';

const {WebRTCModule} = NativeModules;

export default class RTCRtpTransceiver {
    _peerConnectionId: number;
    _sender: RTCRtpSender;
    
    _id: string;
    _mid: string | null;
    _direction: string;
    _currentDirection: string;
    _stopped: boolean;
    
    constructor(pcId, state) {
        this._peerConnectionId = pcId;
        this._id = state.id;
        this._direction = state.direction;
        this._currentDirection = state.currentDirection;
        this._stopped = state.isStopped;
        this.sender = new RTCRtpSender(this);
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
        this._direction = val;

        WebRTCModule.peerConnectionTransceiverSetDirection(this._peerConnectionId, this.id, val, (successful, data) => {
            if (successful) {
                this._updateState(data);
            } else {
                console.warn("Unable to set direction: " + data);
            }
        });
    }

    get currentDirection() {
        return this._currentDirection;
    }

    stop() {
        if (this._stopped) {
            return;
        }
        this._stopped = true;
        return new Promise((resolve, reject) => {
            WebRTCModule.peerConnectionTransceiverStop(this._peerConnectionId, this.id, (successful, data) => {
                if (successful) {
                    this._updateState(data);
                    resolve();
                } else {
                    reject(new Error(data));
                }
            });
        });
    }

    _updateState(state) {
        this._mid = state.mid;
        this._direction = state.direction;
        this._currentDirection = state.currentDirection;
        if (state.isStopped) {
            this._stopped = true;
        }
    }
}