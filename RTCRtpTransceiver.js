import {NativeModules} from 'react-native';
import RTCRtpSender from './RTCRtpSender';

const {WebRTCModule} = NativeModules;

export default class RTCRtpTransceiver {
    _state: any;
    _peerConnectionId: number;
    sender: RTCRtpSender
    id: string;
    
    constructor(pcId, state) {
        this._peerConnectionId = pcId;
        this._state = state;
        this.id = state.id;
        this.sender = new RTCRtpSender(this);
    }

    get mid() {
        this.state.mid ? this.state.mid : null;
    }

    get isStopped() {
        return this.state.isStopped;
    }

    get direction() {
        return this.state.direction;
    }

    get currentDirection() {
        return this.state.currentDirection ? this.state.currentDirection : null;
    }

    stop() {
        return new Promise((resolve, reject) => {
            WebRTCModule.peerConnectionTransceiverStop(this._peerConnectionId, this._state.id, (successful, data) => {
                if (successful) {
                    this._state = data;
                    resolve();
                } else {
                    reject(new Error(data));
                }
            });
        });
    }

    _updateState(state) {
        this._state = state;
    }
}