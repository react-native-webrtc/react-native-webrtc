import { NativeModules } from 'react-native';
import MediaStreamTrack from './MediaStreamTrack';
import RTCRtpCapabilities, { getCapabilities } from './RTCRtpCapabilities';

const { WebRTCModule } = NativeModules;

export default class RTCRtpReceiver {
    _id: string;
    _track: MediaStreamTrack;
    constructor(info: { id: string, track: MediaStreamTrack }) {
        this._id = info.id;
        this._track = info.track;
        Object.freeze(this);
    }

    static getCapabilities(kind: "audio" | "video"): RTCRtpCapabilities {

        if (kind == "audio") {
            throw new Error("Unimplemented capabilities for audio");
        }
        const capabilities = getCapabilities('receiver');
        if (!capabilities)
            throw new Error('capabilities is not yet initialized');
        return capabilities;
    }

    get id() {
        return this._id;
    }

    get track() {
        return this._track;
    }
}
