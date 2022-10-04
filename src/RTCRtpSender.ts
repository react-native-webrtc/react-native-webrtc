import { NativeModules } from 'react-native';

import MediaStreamTrack from './MediaStreamTrack';
import RTCRtpCapabilities, { senderCapabilities, DEFAULT_AUDIO_CAPABILITIES } from './RTCRtpCapabilities';
import RTCRtpSendParameters from './RTCRtpSendParameters';

const { WebRTCModule } = NativeModules;


export default class RTCRtpSender {
    _id: string;
    _track: MediaStreamTrack | null = null;
    _peerConnectionId: number;
    _rtpParameters: RTCRtpSendParameters;

    constructor(info: {
        peerConnectionId: number,
        id: string,
        track?: MediaStreamTrack,
        rtpParameters: RTCRtpSendParameters
    }) {
        this._peerConnectionId = info.peerConnectionId;
        this._id = info.id;
        this._rtpParameters = info.rtpParameters;

        if (info.track) {
            this._track = info.track;
        }
    }

    async replaceTrack(track: MediaStreamTrack | null): Promise<void> {
        try {
            await WebRTCModule.senderReplaceTrack(this._peerConnectionId, this._id, track ? track.id : null);
        } catch (e) {
            return;
        }

        this._track = track;
    }

    static getCapabilities(kind: 'audio' | 'video'): RTCRtpCapabilities {
        if (kind === 'audio') {
            return DEFAULT_AUDIO_CAPABILITIES;
        }

        if (!senderCapabilities) {
            throw new Error('sender Capabilities are null');
        }

        return senderCapabilities;
    }

    getParameters(): RTCRtpSendParameters {
        return this._rtpParameters;
    }

    setParameters(parameters: RTCRtpSendParameters): Promise<void> {
        return WebRTCModule.senderSetParameters(this._peerConnectionId,
            this._id,
            JSON.parse(JSON.stringify(parameters)))// This allows us to get rid of private "underscore properties"
            .then(newParameters => {
                this._rtpParameters = new RTCRtpSendParameters(newParameters);
            });
    }


    get track() {
        return this._track;
    }

    get id() {
        return this._id;
    }
}
