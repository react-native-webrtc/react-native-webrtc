import {NativeModules} from 'react-native';
import RTCRtpSender from './RTCRtpSender';
import RTCRtpReceiver from './RTCRtpReceiver';
import MediaStreamTrack from './MediaStreamTrack';

const {WebRTCModule} = NativeModules;

export default class RTCRtpTransceiver {
    _peerConnectionId: number;
    _sender: RTCRtpSender;
    _receiver: RTCRtpReceiver
    
    _id: string;
    _mid: string | null;
    _direction: string;
    _currentDirection: string;
    _stopped: boolean;
    _mergeState: Function;
    
    constructor(pcId, state, mergeState) {
        this._peerConnectionId = pcId;
        this._id = state.id;
        this._mid = state.mid ? state.mid : null;
        this._direction = state.direction;
        this._currentDirection = state.currentDirection;
        this._stopped = state.isStopped;
        this._mergeState = mergeState;
        this._sender = new RTCRtpSender(this, mergeState);
        this._receiver = new RTCRtpReceiver(state.receiver.id, new MediaStreamTrack(state.receiver.track));
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
                this._mergeState(data.state);
            } else {
                console.warn("Unable to set direction: " + data);
            }
        });
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
        this._stopped = true;
        return new Promise((resolve, reject) => {
            WebRTCModule.peerConnectionTransceiverStop(this._peerConnectionId, this.id, (successful, data) => {
                if (successful) {
                    this._mergeState(data.state);
                    resolve();
                } else {
                    reject(new Error(data));
                }
            });
        });
    }

    _updateState(state) {
        this._mid = state.mid ? state.mid : null;
        this._direction = state.direction;
        this._currentDirection = state.currentDirection;
        if (state.isStopped) {
            this._stopped = true;
        }
    }
}