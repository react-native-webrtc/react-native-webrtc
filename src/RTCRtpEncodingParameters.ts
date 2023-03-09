export interface RTCRtpEncodingParametersInit {
    active: boolean,
    rid?: string;
    maxFramerate?: number;
    maxBitrate?: number;
    scaleResolutionDownBy?: number;
}

export default class RTCRtpEncodingParameters {
    active: boolean;
    _rid: string | null;
    _maxFramerate: number | null;
    _maxBitrate: number | null;
    _scaleResolutionDownBy: number | null;

    constructor(init: RTCRtpEncodingParametersInit) {
        this.active = init.active;
        this._rid = init.rid ?? null;
        this._maxBitrate = init.maxBitrate ?? null;
        this._maxFramerate = init.maxFramerate ?? null;
        this._scaleResolutionDownBy = init.scaleResolutionDownBy ?? null;
    }

    get rid() {
        return this._rid;
    }

    get maxFramerate() {
        return this._maxFramerate;
    }

    set maxFramerate(framerate) {
        // eslint-disable-next-line eqeqeq
        if (framerate != null && framerate > 0) {
            this._maxFramerate = framerate;
        } else {
            this._maxFramerate = null;
        }
    }

    get maxBitrate() {
        return this._maxBitrate;
    }

    set maxBitrate(bitrate) {
        // eslint-disable-next-line eqeqeq
        if (bitrate != null && bitrate >= 0) {
            this._maxBitrate = bitrate;
        } else {
            this._maxBitrate = null;
        }
    }

    get scaleResolutionDownBy() {
        return this._scaleResolutionDownBy;
    }

    set scaleResolutionDownBy(resolutionScale) {
        // eslint-disable-next-line eqeqeq
        if (resolutionScale != null && resolutionScale >= 1) {
            this._scaleResolutionDownBy = resolutionScale;
        } else {
            this._scaleResolutionDownBy = null;
        }
    }

    toJSON(): RTCRtpEncodingParametersInit {
        const obj = {
            active: this.active,
        };

        if (this._rid !== null) {
            obj['rid'] = this._rid;
        }

        if (this._maxBitrate !== null) {
            obj['maxBitrate'] = this._maxBitrate;
        }

        if (this._maxFramerate !== null) {
            obj['maxFramerate'] = this._maxFramerate;
        }

        if (this._scaleResolutionDownBy !== null) {
            obj['scaleResolutionDownBy'] = this._scaleResolutionDownBy;
        }

        return obj;
    }
}
