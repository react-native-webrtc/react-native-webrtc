export type RTCCertificateFingerprint = {
    algorithm: string;
    value: string;
};

export default class RTCCertificate {
    _privateKey: string;
    _certificate: string;
    _expires: number;
    _fingerprints: RTCCertificateFingerprint[];

    constructor(info: {
        privateKey: string;
        certificate: string;
        expires: number;
        fingerprints: RTCCertificateFingerprint[];
    }) {
        this._privateKey = info.privateKey;
        this._certificate = info.certificate;
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
