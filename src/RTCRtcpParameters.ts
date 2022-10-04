
export default class RTCRtcpParameters {
    readonly cname: string;
    readonly reducedSize: boolean;

    constructor(init: {
    cname: string,
    reducedSize: boolean
  }) {
        this.cname = init.cname;
        this.reducedSize = init.reducedSize;
        Object.freeze(this);
    }
}