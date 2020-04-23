import {NativeModules} from 'react-native';
import RTCRtpTransceiver from './RTCRtpTransceiver';

const {WebRTCModule} = NativeModules;

export default class RTCRtpSender {
    _transceiver: RTCRtpTransceiver;

    constructor(_transceiver: RTCRtpTransceiver) {
        this._transceiver = _transceiver;
    }

    replaceTrack = (track: MediaStreamTrack | null) => {
        return new Promise((resolve, reject) => {
            WebRTCModule.peerConnectionTransceiverReplaceTrack(this._transceiver._peerConnectionId, this._transceiver.id, track ? track.id : null, (successful, data) => {
                if (successful) {
                    this._transceiver._updateState(data);
                    resolve();
                } else {
                    reject(new Error(data));
                }
            });
        });
    }
}