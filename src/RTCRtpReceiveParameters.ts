import RTCRtpParameters, { RTCRtpParametersInit } from './RTCRtpParameters';

export default class RTCRtpReceiveParameters extends RTCRtpParameters {
    constructor(init: RTCRtpParametersInit) {
        super(init);
    }
}
