export interface RTCRtcpParametersInit {
    cname: string;
    reducedSize: boolean;
}

export default class RTCRtcpParameters {
    readonly cname: string;
    readonly reducedSize: boolean;

    constructor(init: RTCRtcpParametersInit) {
        this.cname = init.cname;
        this.reducedSize = init.reducedSize;

        Object.freeze(this);
    }

    toJSON(): RTCRtcpParametersInit {
        return {
            cname: this.cname,
            reducedSize: this.reducedSize
        };
    }
}
