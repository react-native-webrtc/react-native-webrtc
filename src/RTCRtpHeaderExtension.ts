
export default class RTCRtpHeaderExtension {
    readonly id: number;
    readonly uri: string;
    readonly encrypted: boolean;

    constructor(init: {
        id: number,
        uri: string,
        encrypted: boolean
    }) {
        this.id = init.id;
        this.uri = init.uri;
        this.encrypted = init.encrypted;
        Object.freeze(this);
    }
}

