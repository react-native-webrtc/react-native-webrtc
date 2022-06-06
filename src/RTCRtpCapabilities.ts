import {NativeModules} from 'react-native';
import RTCRtpCodecCapability from './RTCRtpCodecCapability';
import EventEmitter from './EventEmitter';

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

var _senderCapabilities: RTCRtpCapabilities | null = null;
var _receiverCapabilities: RTCRtpCapabilities | null = null;

export function getCapabilities(endpoint: 'sender' | 'receiver'): RTCRtpCapabilities | null {
    switch (endpoint) {
        case 'sender': {
            if (!_senderCapabilities) {
                WebRTCModule.senderGetCapabilities();
                return null;
            }
            return _senderCapabilities;
        }

        case 'receiver': {
            if (!_receiverCapabilities) {
                WebRTCModule.senderGetCapabilities();
                return null;
            }
            return _receiverCapabilities;
        }
        default:
            throw new TypeError('Invalid endpoint: ' + endpoint);
    }
}

// Registering EventEmitter to initialize capabilities
const _subscriptions: any[] = [
    EventEmitter.addListener('senderGetCapabilities', ev => {
        if (ev.codecs === 'undefined') {
            throw new Error('Invalid event object passed to senderGetCapabilities: codecs is undefined');
        }
        _senderCapabilities = new RTCRtpCapabilities(ev.codecs);
    }),

    EventEmitter.addListener('receiverGetCapabilities', ev => {
        if (ev.codecs === 'undefined') {
            throw new Error('Invalid event object passed to senderGetCapabilities: codecs is undefined');
        }
        _receiverCapabilities = new RTCRtpCapabilities(ev.codecs);
    }),
]

// Initialize capabilities on module import
if (!_senderCapabilities) {
    WebRTCModule.senderGetCapabilities();
}
if (!_receiverCapabilities) {
    WebRTCModule.receiverGetCapabilities();
}