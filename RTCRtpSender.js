import {NativeModules} from 'react-native';
import RTCRtpTransceiver from './RTCRtpTransceiver';

const {WebRTCModule} = NativeModules;

export default class RTCRtpSender {
    _transceiver: RTCRtpTransceiver;
    _mergeState: Function;

    constructor(_transceiver: RTCRtpTransceiver, mergeState: Function) {
        this._transceiver = _transceiver;
        this._mergeState = mergeState;
    }

    replaceTrack = (track: MediaStreamTrack | null) => {
        return new Promise((resolve, reject) => {
            WebRTCModule.peerConnectionTransceiverReplaceTrack(this._transceiver._peerConnectionId, this._transceiver.id, track ? track.id : null, (successful, data) => {
                if (successful) {
                    this._transceiver._mergeState(data.state);
                    resolve();
                } else {
                    reject(new Error(data));
                }
            });
        });
    }
}