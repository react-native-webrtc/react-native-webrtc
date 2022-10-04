import RTCRtpEncodingParameters from './RTCRtpEncodingParameters';
import RTCRtpParameters, { RTCRtpParametersInit } from './RTCRtpParameters';

type DegradationPreferenceType = 'maintain-framerate'
    | 'maintain-resolution'
    | 'balanced'
    | 'disabled'


/**
 * Class to convert degradation preference format. Native has a format such as
 * MAINTAIN_FRAMERATE whereas the web APIs expect maintain-framerate
 */
class DegradationPreference {
    static fromNative(nativeFormat: string): DegradationPreferenceType {
        const stringFormat = nativeFormat.toLowerCase().replace('_', '-');

        return stringFormat as DegradationPreferenceType;
    }

    static toNative(format: DegradationPreferenceType): string {
        return format.toUpperCase().replace('-', '_');
    }
}

export default class RTCRtpSendParameters extends RTCRtpParameters {
    readonly transactionId: string;
    readonly encodings: RTCRtpEncodingParameters[];
    degradationPreference: DegradationPreferenceType | null;
    constructor(init: RTCRtpParametersInit & {
        transactionId: string,
        encodings: RTCRtpEncodingParameters[],
        degradationPreference?: string
    }) {
        super(init);
        this.transactionId = init.transactionId;
        this.encodings = init.encodings;
        this.degradationPreference = init.degradationPreference ?
            DegradationPreference.fromNative(init.degradationPreference) : null;
    }

    toJSON() {
        const obj = {
            encodings: this.encodings,
        };

        if (this.degradationPreference !== null) {
            obj['degradationPreference'] = DegradationPreference.toNative(this.degradationPreference);
        }

        return obj;
    }
}
