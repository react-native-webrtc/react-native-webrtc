import { NativeModules } from 'react-native';

import MediaStreamTrack from './MediaStreamTrack';
import RTCRtpCapabilities, { senderCapabilities, DEFAULT_AUDIO_CAPABILITIES } from './RTCRtpCapabilities';
import RTCRtpSendParameters, { RTCRtpSendParametersInit } from './RTCRtpSendParameters';

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
        rtpParameters: RTCRtpSendParametersInit
    }) {
        this._peerConnectionId = info.peerConnectionId;
        this._id = info.id;
        this._rtpParameters = new RTCRtpSendParameters(info.rtpParameters);

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

    async setParameters(parameters: RTCRtpSendParameters): Promise<void> {
        // This allows us to get rid of private "underscore properties"
        const _params = JSON.parse(JSON.stringify(parameters));
        const newParameters = await WebRTCModule.senderSetParameters(this._peerConnectionId, this._id, _params);

        this._rtpParameters = new RTCRtpSendParameters(newParameters);
    }

    getStats() {
        return WebRTCModule.senderGetStats(this._peerConnectionId, this._id).then(data =>
            /* On both Android and iOS it is faster to construct a single
            JSON string representing the Map of StatsReports and have it
            pass through the React Native bridge rather than the Map of
            StatsReports. While the implementations do try to be faster in
            general, the stress is on being faster to pass through the React
            Native bridge which is a bottleneck that tends to be visible in
            the UI when there is congestion involving UI-related passing.
            */
            new Map(JSON.parse(data))
        );
    }

    get track() {
        return this._track;
    }

    get id() {
        return this._id;
    }
}
