export interface RTCRtpHeaderExtensionInit {
    id: number;
    uri: string;
    encrypted: boolean;
}

export default class RTCRtpHeaderExtension {
    readonly id: number;
    readonly uri: string;
    readonly encrypted: boolean;

    constructor(init: RTCRtpHeaderExtensionInit) {
        this.id = init.id;
        this.uri = init.uri;
        this.encrypted = init.encrypted;

        Object.freeze(this);
    }

    toJSON(): RTCRtpHeaderExtensionInit {
        return {
            id: this.id,
            uri: this.uri,
            encrypted: this.encrypted
        };
    }
}
