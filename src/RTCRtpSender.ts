import {NativeModules} from 'react-native';
import MediaStreamTrack from './MediaStreamTrack';
import RTCRtpCapabilities, { senderCapabilities } from './RTCRtpCapabilities';

const {WebRTCModule} = NativeModules;


export default class RTCRtpSender {
    _id: string;
    _track: MediaStreamTrack | null = null;
    _peerConnectionId: number;

    constructor(info: { peerConnectionId: number, id: string, track?: MediaStreamTrack }) {
        this._peerConnectionId = info.peerConnectionId;
        this._id = info.id;
        if (info.track)
            this._track = info.track;
    }

    replaceTrack(track: MediaStreamTrack | null): Promise<void> {
        return WebRTCModule.senderReplaceTrack(this._peerConnectionId, this._id, track ? track.id : null)
            .then(() => this._track = track);
    }
    
    static getCapabilities(kind: "audio" | "video"): RTCRtpCapabilities {
        if (kind === "audio") {
            throw new Error("Unimplemented capabilities for audio");
        }
        if (!senderCapabilities) {
            throw new Error("sender Capabilities are null");
        }
        return senderCapabilities;
    }

    get track() {
        return this._track;
    }

    get id() {
        return this._id;
    }
}
