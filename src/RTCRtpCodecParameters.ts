export interface RTCRtpCodecParametersInit {
    payloadType: number;
    clockRate: number;
    mimeType: string;
    channels?: number;
    sdpFmtpLine?: string;
}

export default class RTCRtpCodecParameters {
    readonly payloadType: number;
    readonly clockRate: number;
    readonly mimeType: string;
    readonly channels: number | null;
    readonly sdpFmtpLine: string | null;

    constructor(init: RTCRtpCodecParametersInit) {
        this.payloadType = init.payloadType;
        this.clockRate = init.clockRate;
        this.mimeType = init.mimeType;

        this.channels = init.channels ? init.channels : null;
        this.sdpFmtpLine = init.sdpFmtpLine ? init.sdpFmtpLine : null;

        Object.freeze(this);
    }

    toJSON(): RTCRtpCodecParametersInit {
        const obj = {
            payloadType: this.payloadType,
            clockRate: this.clockRate,
            mimeType: this.mimeType
        };

        if (this.channels !== null) {
            obj['channels'] = this.channels;
        }

        if (this.sdpFmtpLine !== null) {
            obj['sdpFmtpLine'] = this.sdpFmtpLine;
        }

        return obj;
    }
}
