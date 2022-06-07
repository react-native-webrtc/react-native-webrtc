import {NativeModules} from 'react-native';
import MediaStreamTrack from './MediaStreamTrack';
import RTCRtpCapabilities, { getCapabilities } from './RTCRtpCapabilities';

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
        return new Promise<void>((resolve, reject) => {
            WebRTCModule.senderReplaceTrack(this._peerConnectionId, this._id, track ? track.id : null, (successful, data) => {
                if (successful) {
                    this._track = track;
                    resolve();
                } else {
                    reject(new Error(data));
                }
            });
        });
    }
    
    static getCapabilities(kind: "audio" | "video"): RTCRtpCapabilities {
        if (kind === "audio") {
            throw new Error("Unimplemented capabilities for audio");
        }
        const capabilities = getCapabilities('sender');
        if (!capabilities)
            throw new Error("Capabilities is not yet initialized");
        return capabilities;
    }

    get track() {
        return this._track;
    }

    get id() {
        return this._id;
    }
}
