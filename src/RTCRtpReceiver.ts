import { NativeModules } from 'react-native';
import MediaStreamTrack from './MediaStreamTrack';
import RTCRtpCapabilities from './RTCRtpCapabilities';

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

        // Need to call the underlying module to get capabilities 
        const result = WebRTCModule.receiverGetCapabilities();
        return new RTCRtpCapabilities(result.codecs);
    }

    get id() {
        return this._id;
    }

    get track() {
        return this._track;
    }
}
