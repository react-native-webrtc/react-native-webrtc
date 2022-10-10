import MediaStreamTrack from './MediaStreamTrack';
import RTCRtpCapabilities, { DEFAULT_AUDIO_CAPABILITIES, receiverCapabilities } from './RTCRtpCapabilities';
import { RTCRtpParametersInit } from './RTCRtpParameters';
import RTCRtpReceiveParameters from './RTCRtpReceiveParameters';


export default class RTCRtpReceiver {
    _id: string;
    _peerConnectionId: number;
    _track: MediaStreamTrack | null = null;
    _rtpParameters: RTCRtpReceiveParameters;

    constructor(info: {
        peerConnectionId: number,
        id: string,
        track?: MediaStreamTrack,
        rtpParameters: RTCRtpParametersInit
    }) {
        this._id = info.id;
        this._peerConnectionId = info.peerConnectionId;
        this._rtpParameters = new RTCRtpReceiveParameters(info.rtpParameters);

        if (info.track) {
            this._track = info.track;
        }
    }

    static getCapabilities(kind: 'audio' | 'video'): RTCRtpCapabilities {
        if (kind === 'audio') {
            return DEFAULT_AUDIO_CAPABILITIES;
        }

        if (!receiverCapabilities) {
            throw new Error('Receiver Capabilities is null');
        }

        return receiverCapabilities;
    }

    getParameters(): RTCRtpReceiveParameters {
        return this._rtpParameters;
    }

    get id() {
        return this._id;
    }

    get track() {
        return this._track;
    }
}
