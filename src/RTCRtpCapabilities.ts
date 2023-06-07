import { NativeModules } from 'react-native';

import RTCRtpCodecCapability from './RTCRtpCodecCapability';
const { WebRTCModule } = NativeModules;

/**
 * @brief represents codec capabilities for senders and receivers.
 */
export default class RTCRtpCapabilities {
    _codecs: RTCRtpCodecCapability[] = [];
    constructor(codecs: RTCRtpCodecCapability[]) {
        this._codecs = codecs;
        Object.freeze(this);
    }

    get codecs() {
        return this._codecs;
    }
}
