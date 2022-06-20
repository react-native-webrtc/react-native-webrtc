import RTCRtpParameters, { RTCRtpParametersInit } from "./RTCRtpParameters";
import RTCRtpEncodingParameters from "./RTCRtpEncodingParameters";

type DegradationPreference = 'maintain-framerate' 
    | 'maintain-resolution'
    | 'balanced'

export default class RTCRtpSendParameters extends RTCRtpParameters {

    readonly transactionId: string;
    readonly encodings: RTCRtpEncodingParameters[];
    degradationPreference: DegradationPreference | null;
    constructor(init: RTCRtpParametersInit & {
        transactionId: string,
        encodings: RTCRtpEncodingParameters[],
        degradationPreference?: DegradationPreference
    }) {
        super(init);
        this.transactionId = init.transactionId;
        this.encodings = init.encodings;
        this.degradationPreference = init.degradationPreference ?? null;
    }

    toString() {
        let obj = {
            encodings: this.encodings,
        };
        if (this.degradationPreference !== null) {
            obj['degradationPreference'] = this.degradationPreference.toUpperCase();
        }
        return JSON.stringify(obj);
    }
}