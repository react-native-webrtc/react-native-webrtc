export default class RTCIceCandidate {
    candidate: string;
    sdpMLineIndex: number;
    sdpMid: string;

    constructor(info) {
        if (typeof info?.candidate !== 'string') {
            throw new TypeError('`candidate` must be string');
        }
        if (typeof info?.sdpMLineIndex !== 'number') {
            throw new TypeError('`sdpMLineIndex` must be number');
        }
        if (typeof info?.sdpMid !== 'string') {
            throw new TypeError('`sdpMid` must be string');
        }

        this.candidate = info.candidate;
        this.sdpMLineIndex = info.sdpMLineIndex;
        this.sdpMid = info.sdpMid;
    }

    toJSON() {
        return {
            candidate: this.candidate,
            sdpMLineIndex: this.sdpMLineIndex,
            sdpMid: this.sdpMid
        };
    }
}
