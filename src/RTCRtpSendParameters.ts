import RTCRtpEncodingParameters, { RTCRtpEncodingParametersInit } from './RTCRtpEncodingParameters';
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

export interface RTCRtpSendParametersInit extends RTCRtpParametersInit {
    transactionId: string;
    encodings: RTCRtpEncodingParametersInit[];
    degradationPreference?: string;
}

export default class RTCRtpSendParameters extends RTCRtpParameters {
    readonly transactionId: string;
    readonly encodings: RTCRtpEncodingParameters[];
    degradationPreference: DegradationPreferenceType | null;

    constructor(init: RTCRtpSendParametersInit) {
        super(init);

        this.transactionId = init.transactionId;
        this.encodings = [];
        this.degradationPreference = init.degradationPreference ?
            DegradationPreference.fromNative(init.degradationPreference) : null;

        for (const enc of init.encodings) {
            this.encodings.push(new RTCRtpEncodingParameters(enc));
        }
    }

    toJSON(): RTCRtpSendParametersInit {
        const obj = super.toJSON();

        obj['transactionId'] = this.transactionId;

        obj['encodings'] = this.encodings.map(e => e.toJSON());

        if (this.degradationPreference !== null) {
            obj['degradationPreference'] = DegradationPreference.toNative(this.degradationPreference);
        }

        return obj as RTCRtpSendParametersInit;
    }
}
