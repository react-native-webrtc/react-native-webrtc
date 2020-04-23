import {NativeModules} from 'react-native';

const {WebRTCModule} = NativeModules;

export default class RTCRtpTransceiver {
    _state: any;
    _peerConnectionId: number;
    
    constructor(pcId, state) {
        this._peerConnectionId = pcId;
        this._state = state;
    }

    get id() {
        return this.state.id;
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

    async stop() {
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
}