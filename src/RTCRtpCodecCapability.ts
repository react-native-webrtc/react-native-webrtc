
export default class RTCRtpCodecCapability {
    _mimeType: string;

    constructor(init: { mimeType: string }) {
        this._mimeType = init.mimeType;
        Object.freeze(this);
    }

    get mimeType() {
        return this._mimeType;
    }
}