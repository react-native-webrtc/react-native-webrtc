
export default class RTCSessionDescription {
    _sdp: string;
    _type: string | null;

    constructor(info = { type: null, sdp: '' }) {
        this._sdp = info.sdp;
        this._type = info.type;
    }

    get sdp(): string {
        return this._sdp;
    }

    get type(): string | null {
        return this._type;
    }

    toJSON(): { sdp: string, type: string | null } {
        return {
            sdp: this._sdp,
            type: this._type
        };
    }
}
