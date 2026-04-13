export type RTCCertificateFingerprint = {
    algorithm: string;
    value: string;
};

export default class RTCCertificate {
    _expires: number;
    _fingerprints: RTCCertificateFingerprint[];
    _id: string;

    constructor(info: { certificateId: string, expires: number, fingerprints: RTCCertificateFingerprint[] }) {
        this._id = info.certificateId;
        this._expires = info.expires;
        this._fingerprints = info.fingerprints;
    }

    get expires(): number {
        return this._expires;
    }

    getFingerprints(): RTCCertificateFingerprint[] {
        return this._fingerprints;
    }
}
