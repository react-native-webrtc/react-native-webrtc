import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';
import type { Int32, Double } from 'react-native/Libraries/Types/CodegenTypes';

// EventEmitter type for codegen - generates emit* methods on native side
// The codegen parser identifies this type by the readonly property pattern
type EventEmitter<T> = (handler: (value: T) => void | Promise<void>) => { remove(): void };

export interface Spec extends TurboModule {
    // --- Events (using EventEmitter for new architecture support) ---
    readonly peerConnectionSignalingStateChanged: EventEmitter<Object>;
    readonly peerConnectionStateChanged: EventEmitter<Object>;
    readonly peerConnectionOnRenegotiationNeeded: EventEmitter<Object>;
    readonly peerConnectionIceConnectionChanged: EventEmitter<Object>;
    readonly peerConnectionIceGatheringChanged: EventEmitter<Object>;
    readonly peerConnectionGotICECandidate: EventEmitter<Object>;
    readonly peerConnectionDidOpenDataChannel: EventEmitter<Object>;
    readonly peerConnectionOnRemoveTrack: EventEmitter<Object>;
    readonly peerConnectionOnTrack: EventEmitter<Object>;
    readonly dataChannelStateChanged: EventEmitter<Object>;
    readonly dataChannelReceiveMessage: EventEmitter<Object>;
    readonly dataChannelDidChangeBufferedAmount: EventEmitter<Object>;
    readonly mediaStreamTrackMuteChanged: EventEmitter<Object>;
    readonly mediaStreamTrackEnded: EventEmitter<Object>;

    // --- Synchronous methods (no Promise) ---
    peerConnectionInit(config: Object, id: Int32): boolean;
    peerConnectionAddTransceiver(id: Int32, options: Object): Object;
    peerConnectionAddTrack(id: Int32, trackId: string, options?: Object): Object;
    peerConnectionRemoveTrack(id: Int32, senderId: string): boolean;
    createDataChannel(peerConnectionId: Int32, label: string, config: Object): Object;
    senderGetCapabilities(kind: string): Object;
    receiverGetCapabilities(kind: string): Object;
    transceiverSetCodecPreferences(id: Int32, senderId: string, codecPreferences: Object[]): boolean;
    audioSessionDidActivate(): void;
    audioSessionDidDeactivate(): void;

    // --- Async methods (Promise) ---
    peerConnectionCreateOffer(id: Int32, options: Object): Promise<Object>;
    peerConnectionCreateAnswer(id: Int32, options: Object): Promise<Object>;
    peerConnectionSetLocalDescription(id: Int32, desc: Object | null): Promise<Object>;
    peerConnectionSetRemoteDescription(id: Int32, desc: Object): Promise<Object>;
    peerConnectionAddICECandidate(id: Int32, candidate: Object): Promise<Object>;
    peerConnectionGetStats(id: Int32): Promise<Object>;
    receiverGetStats(pcId: Int32, receiverId: string): Promise<Object>;
    senderGetStats(pcId: Int32, senderId: string): Promise<Object>;
    senderSetParameters(id: Int32, senderId: string, options: Object): Promise<Object>;
    senderReplaceTrack(id: Int32, senderId: string, trackId: string): Promise<boolean>;
    transceiverStop(id: Int32, senderId: string): Promise<boolean>;
    transceiverSetDirection(id: Int32, senderId: string, direction: string): Promise<boolean>;
    mediaStreamTrackApplyConstraints(id: string, constraints: Object): Promise<void>;
    getDisplayMedia(): Promise<Object>;
    checkPermission(mediaType: string): Promise<string>;
    requestPermission(mediaType: string): Promise<string>;

    // --- Fire-and-forget (void) ---
    peerConnectionSetConfiguration(config: Object, id: Int32): void;
    peerConnectionClose(id: Int32): void;
    peerConnectionDispose(id: Int32): void;
    peerConnectionRestartIce(id: Int32): void;
    getUserMedia(
        constraints: Object,
        success: (id: string, tracks: Object[]) => void,
        failure: (type: string, msg: string) => void,
    ): void;
    enumerateDevices(callback: (devices: Object[]) => void): void;
    mediaStreamCreate(id: string): void;
    mediaStreamAddTrack(streamId: string, pcId: Int32, trackId: string): void;
    mediaStreamRemoveTrack(streamId: string, pcId: Int32, trackId: string): void;
    mediaStreamRelease(id: string): void;
    mediaStreamTrackRelease(id: string): void;
    mediaStreamTrackSetEnabled(pcId: Int32, id: string, enabled: boolean): void;
    mediaStreamTrackSetVolume(pcId: Int32, id: string, volume: Double): void;
    mediaStreamTrackSetVideoEffects(id: string, names: Object[]): void;
    dataChannelClose(peerConnectionId: Int32, reactTag: string): void;
    dataChannelDispose(peerConnectionId: Int32, reactTag: string): void;
    dataChannelSend(peerConnectionId: Int32, reactTag: string, data: string, type: string): void;
    addListener(eventName: string): void;
    removeListeners(count: Int32): void;
}

const WebRTCModule = TurboModuleRegistry.getEnforcing<Spec>('WebRTCModule');

export default WebRTCModule;
