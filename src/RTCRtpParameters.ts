import RTCRtcpParameters from './RTCRtcpParameters';
import RTCRtpCodecParameters from './RTCRtpCodecParameters';
import RTCRtpHeaderExtension from './RTCRtpHeaderExtension';


export type RTCRtpParametersInit = {
    codecs: RTCRtpCodecParameters[],
    headerExtensions: RTCRtpHeaderExtension[],
    rtcp: RTCRtcpParameters
}

export default class RTCRtpParameters {
    readonly codecs: RTCRtpCodecParameters[] = [];
    readonly headerExtensions: RTCRtpHeaderExtension[] = [];
    readonly rtcp: RTCRtcpParameters;
    constructor(init: RTCRtpParametersInit) {
        this.codecs = init.codecs;
        this.headerExtensions = init.headerExtensions;
        this.rtcp = init.rtcp;
    }
}