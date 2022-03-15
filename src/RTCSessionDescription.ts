
export default class RTCSessionDescription {
    _sdp: string;
    _type: string | null;

    constructor(info = { type: null, sdp: '' }) {
        this._sdp = info.sdp;
        this._type = info.type;
    }

    get sdp() {
        return this._sdp;
    }

    get type() {
        return this._type;
    }

    toJSON() {
        return {
            sdp: this._sdp,
            type: this._type
        };
    }
}
