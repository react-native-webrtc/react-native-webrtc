import { NativeModules } from 'react-native';

import MediaStreamTrack from './MediaStreamTrack';
import RTCRtpCapabilities, { DEFAULT_AUDIO_CAPABILITIES, receiverCapabilities } from './RTCRtpCapabilities';
import { RTCRtpParametersInit } from './RTCRtpParameters';
import RTCRtpReceiveParameters from './RTCRtpReceiveParameters';

const { WebRTCModule } = NativeModules;

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

    getStats() {
        return WebRTCModule.receiverGetStats(this._peerConnectionId, this._id).then(data =>
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
