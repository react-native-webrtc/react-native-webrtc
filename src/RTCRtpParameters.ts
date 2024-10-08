import RTCRtcpParameters, { RTCRtcpParametersInit } from './RTCRtcpParameters';
import RTCRtpCodecParameters, { RTCRtpCodecParametersInit } from './RTCRtpCodecParameters';
import RTCRtpHeaderExtension, { RTCRtpHeaderExtensionInit } from './RTCRtpHeaderExtension';
import { deepClone } from './RTCUtil';


export interface RTCRtpParametersInit {
    codecs: RTCRtpCodecParametersInit[],
    headerExtensions: RTCRtpHeaderExtensionInit[],
    rtcp: RTCRtcpParametersInit
}

export default class RTCRtpParameters {
    codecs: (RTCRtpCodecParameters | RTCRtpCodecParametersInit)[] = [];
    readonly headerExtensions: RTCRtpHeaderExtension[] = [];
    readonly rtcp: RTCRtcpParameters;

    constructor(init: RTCRtpParametersInit) {
        for (const codec of init.codecs) {
            this.codecs.push(new RTCRtpCodecParameters(codec));
        }

        for (const ext of init.headerExtensions) {
            this.headerExtensions.push(new RTCRtpHeaderExtension(ext));
        }

        this.rtcp = new RTCRtcpParameters(init.rtcp);
    }

    toJSON() {
        return {
            codecs: this.codecs.map(c => deepClone(c)),
            headerExtensions: this.headerExtensions.map(he => deepClone(he)),
            rtcp: deepClone(this.rtcp)
        };
    }
}
