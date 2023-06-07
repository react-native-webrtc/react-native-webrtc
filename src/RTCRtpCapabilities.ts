import { NativeModules } from 'react-native';

import RTCRtpCodecCapability from './RTCRtpCodecCapability';
const { WebRTCModule } = NativeModules;

/**
 * @brief represents codec capabilities for senders and receivers. Currently
 * this only supports codec names and does not have other
 * fields like clockRate and numChannels and such.
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


function getCapabilities(endpoint: 'sender' | 'receiver'): RTCRtpCapabilities | null {
    switch (endpoint) {
        case 'sender': {
            const capabilities = WebRTCModule.senderGetCapabilities();

            if (!capabilities) {
                return null;
            }

            return new RTCRtpCapabilities(capabilities.codecs);
        }

        case 'receiver': {
            const capabilities = WebRTCModule.receiverGetCapabilities();

            if (!capabilities) {
                return null;
            }

            return new RTCRtpCapabilities(capabilities.codecs);
        }

        default:
            throw new TypeError('Invalid endpoint: ' + endpoint);
    }
}
