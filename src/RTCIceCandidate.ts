interface RTCIceCandidateInfo {
    candidate?: string;
    sdpMLineIndex?: number | null;
    sdpMid?: string | null;
}

export default class RTCIceCandidate {
    candidate: string;
    sdpMLineIndex: number;
    sdpMid: string;

    constructor({ candidate = '', sdpMLineIndex = null, sdpMid = null }: RTCIceCandidateInfo) {
        if (sdpMLineIndex === null || sdpMid === null) {
            throw new TypeError('`sdpMLineIndex` and `sdpMid` must not null');
        }

        this.candidate = candidate;
        this.sdpMLineIndex = sdpMLineIndex;
        this.sdpMid = sdpMid;
    }

    toJSON() {
        return {
            candidate: this.candidate,
            sdpMLineIndex: this.sdpMLineIndex,
            sdpMid: this.sdpMid
        };
    }
}
