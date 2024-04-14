import { NativeModules } from "react-native";

const { WebRTCModule } = NativeModules;
const DEFAULT_DURATION = 300;
const DEFAULT_INTER_TONE_GAP = 70;
const MIN_DURATION = 70;
const MAX_DURATION = 6000;
const MIN_INTER_TONE_GAP = 50;
export default class RTCDTMFSender {
    _peerConnectionId: number;
    _senderId: string;
    constructor(peerConnectionId: number, senderId: string) {
        this._peerConnectionId = peerConnectionId;
        this._senderId = senderId;
    }

    get canInsertDTMF(): boolean {
        return WebRTCModule.senderCanInsertDTMF(
            this._peerConnectionId,
            this._senderId
        );
    }

    get toneBuffer(): string {
        return WebRTCModule.getDTMFToneBuffer(
            this._peerConnectionId,
            this._senderId
        );
    }

    insertDTMF(
        tones: string,
        duration = DEFAULT_DURATION,
        interToneGap = DEFAULT_INTER_TONE_GAP
    ) {
        WebRTCModule.senderInsertDTMF(
            this._peerConnectionId,
            this._senderId,
            tones,
            duration < MIN_DURATION
                ? MIN_DURATION
                : duration <= MAX_DURATION
                ? duration
                : MAX_DURATION,
            interToneGap < MIN_INTER_TONE_GAP
                ? MIN_INTER_TONE_GAP
                : interToneGap
        );
    }
}
