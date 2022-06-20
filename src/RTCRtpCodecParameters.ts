
export default class RTCRtpCodecParameters {
    readonly payloadType: number;
    readonly clockRate: number;
    readonly channels: number;
    readonly mimeType: string;
    readonly sdpFmtpLine: Map<String, String>;

    constructor(init: {
        payloadType: number,
        clockRate: number,
        channels: number,
        mimeType: string,
        sdpFmtpLine: Map<String, String>
    }) {
        this.payloadType = init.payloadType;
        this.clockRate = init.clockRate;
        this.channels = init.channels;
        this.mimeType = init.mimeType;
        this.sdpFmtpLine = init.sdpFmtpLine;
        Object.freeze(this);
    }
}