import RTCRtpCodecCapability from './RTCRtpCodecCapability';

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