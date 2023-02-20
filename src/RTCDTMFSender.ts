import { NativeModules, Platform } from "react-native";

const { WebRTCModule } = NativeModules;
const DEFAULT_DURATION = 300;
const DEFAULT_INTER_TONE_GAP = 70;

export default class RTCDTMFSender {
    _peerConnectionId: number;
    _senderId: string;
    constructor(peerConnectionId: number, senderId: string) {
        this._peerConnectionId = peerConnectionId;
        this._senderId = senderId;
    }

    get canInsertDTMF(): boolean {
        // Blocking!
        return WebRTCModule.peerConnectionCanInsertDTMF(
            this._peerConnectionId,
            this._senderId
        );
    }

    insertDTMF(
        tones: string,
        duration = DEFAULT_DURATION,
        interToneGap = DEFAULT_INTER_TONE_GAP
    ) {
        WebRTCModule.peerConnectionSendDTMF(
            tones,
            duration,
            interToneGap < 30 ? 30 : interToneGap,
            this._peerConnectionId,
            this._senderId
        );
    }
}
