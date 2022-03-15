
import { defineCustomEventTarget } from 'event-target-shim';
import { NativeModules } from 'react-native';

import MediaStream from './MediaStream';
import MediaStreamEvent from './MediaStreamEvent';
import MediaStreamTrackEvent from './MediaStreamTrackEvent';
import RTCDataChannel from './RTCDataChannel';
import RTCDataChannelEvent from './RTCDataChannelEvent';
import RTCSessionDescription from './RTCSessionDescription';
import RTCIceCandidate from './RTCIceCandidate';
import RTCIceCandidateEvent from './RTCIceCandidateEvent';
import RTCEvent from './RTCEvent';
import * as RTCUtil from './RTCUtil';
import EventEmitter from './EventEmitter';

const { WebRTCModule } = NativeModules;

type RTCSignalingState =
    | 'stable'
    | 'have-local-offer'
    | 'have-remote-offer'
    | 'have-local-pranswer'
    | 'have-remote-pranswer'
    | 'closed';

type RTCIceGatheringState = 'new' | 'gathering' | 'complete';

type RTCPeerConnectionState = 'new' | 'connecting' | 'connected' | 'disconnected' | 'failed' | 'closed';

type RTCIceConnectionState = 'new' | 'checking' | 'connected' | 'completed' | 'failed' | 'disconnected' | 'closed';

type RTCDataChannelInit = {
    ordered?: boolean,
    maxPacketLifeTime?: number,
    maxRetransmits?: number,
    protocol?: string,
    negotiated?: boolean,
    id?: number
};

const PEER_CONNECTION_EVENTS = [
    'connectionstatechange',
    'icecandidate',
    'icecandidateerror',
    'iceconnectionstatechange',
    'icegatheringstatechange',
    'negotiationneeded',
    'signalingstatechange',
    'datachannel',
    'addstream',
    'removestream'
];

let nextPeerConnectionId = 0;

export default class RTCPeerConnection extends defineCustomEventTarget(...PEER_CONNECTION_EVENTS) {
    localDescription: RTCSessionDescription | null = null;
    remoteDescription: RTCSessionDescription | null = null;

    signalingState: RTCSignalingState = 'stable';
    iceGatheringState: RTCIceGatheringState = 'new';
    connectionState: RTCPeerConnectionState = 'new';
    iceConnectionState: RTCIceConnectionState = 'new';

    _peerConnectionId: number;
    _localStreams: Array<MediaStream> = [];
    _remoteStreams: Array<MediaStream> = [];
    _subscriptions: Array<any> = [];

    constructor(configuration) {
        super();
        this._peerConnectionId = nextPeerConnectionId++;
        WebRTCModule.peerConnectionInit(configuration, this._peerConnectionId);
        this._registerEvents();
    }

    addStream(stream: MediaStream) {
        const index = this._localStreams.indexOf(stream);
        if (index !== -1) {
            return;
        }
        WebRTCModule.peerConnectionAddStream(stream._reactTag, this._peerConnectionId);
        this._localStreams.push(stream);
    }

    removeStream(stream: MediaStream) {
        const index = this._localStreams.indexOf(stream);
        if (index === -1) {
            return;
        }
        this._localStreams.splice(index, 1);
        WebRTCModule.peerConnectionRemoveStream(stream._reactTag, this._peerConnectionId);
    }

    createOffer(options) {
        return new Promise((resolve, reject) => {
            WebRTCModule.peerConnectionCreateOffer(
                this._peerConnectionId,
                RTCUtil.normalizeOfferAnswerOptions(options),
                (successful, data) => {
                    if (successful) {
                        resolve(data);
                    } else {
                        reject(data); // TODO: convert to NavigatorUserMediaError
                    }
                }
            );
        });
    }

    createAnswer(options = {}) {
        return new Promise((resolve, reject) => {
            WebRTCModule.peerConnectionCreateAnswer(
                this._peerConnectionId,
                RTCUtil.normalizeOfferAnswerOptions(options),
                (successful, data) => {
                    if (successful) {
                        resolve(data);
                    } else {
                        reject(data);
                    }
                }
            );
        });
    }

    setConfiguration(configuration) {
        WebRTCModule.peerConnectionSetConfiguration(configuration, this._peerConnectionId);
    }

    async setLocalDescription(sessionDescription?: RTCSessionDescription): Promise<void> {
        const desc = sessionDescription
            ? sessionDescription.toJSON
                ? sessionDescription.toJSON()
                : sessionDescription
            : null;
        const newSdp = await WebRTCModule.peerConnectionSetLocalDescription(this._peerConnectionId, desc);

        this.localDescription = new RTCSessionDescription(newSdp);
    }

    setRemoteDescription(sessionDescription: RTCSessionDescription): Promise<void> {
        return new Promise((resolve, reject) => {
            WebRTCModule.peerConnectionSetRemoteDescription(
                sessionDescription.toJSON ? sessionDescription.toJSON() : sessionDescription,
                this._peerConnectionId,
                (successful, data) => {
                    if (successful) {
                        this.remoteDescription = new RTCSessionDescription(data);
                        resolve();
                    } else {
                        reject(data);
                    }
                }
            );
        });
    }

    async addIceCandidate(candidate): Promise<void> {
        if (!candidate || !candidate.candidate) {
            // XXX end-of cantidates is not implemented: https://bugs.chromium.org/p/webrtc/issues/detail?id=9218
            return;
        }

        const newSdp = await WebRTCModule.peerConnectionAddICECandidate(
            this._peerConnectionId,
            candidate.toJSON ? candidate.toJSON() : candidate
        );

        this.remoteDescription = new RTCSessionDescription(newSdp);
    }

    getStats() {
        return WebRTCModule.peerConnectionGetStats(this._peerConnectionId).then(data => {
            /* On both Android and iOS it is faster to construct a single
            JSON string representing the Map of StatsReports and have it
            pass through the React Native bridge rather than the Map of
            StatsReports. While the implementations do try to be faster in
            general, the stress is on being faster to pass through the React
            Native bridge which is a bottleneck that tends to be visible in
            the UI when there is congestion involving UI-related passing.

            TODO Implement the logic for filtering the stats based on 
            the sender/receiver
            */
            return new Map(JSON.parse(data));
        });
    }

    getLocalStreams() {
        return this._localStreams.slice();
    }

    getRemoteStreams() {
        return this._remoteStreams.slice();
    }

    close() {
        WebRTCModule.peerConnectionClose(this._peerConnectionId);
    }

    restartIce() {
        WebRTCModule.peerConnectionRestartIce(this._peerConnectionId);
    }

    _unregisterEvents(): void {
        this._subscriptions.forEach(e => e.remove());
        this._subscriptions = [];
    }

    _registerEvents(): void {
        this._subscriptions = [
            EventEmitter.addListener('peerConnectionOnRenegotiationNeeded', ev => {
                if (ev.id !== this._peerConnectionId) {
                    return;
                }
                // @ts-ignore
                this.dispatchEvent(new RTCEvent('negotiationneeded'));
            }),
            EventEmitter.addListener('peerConnectionIceConnectionChanged', ev => {
                if (ev.id !== this._peerConnectionId) {
                    return;
                }
                this.iceConnectionState = ev.iceConnectionState;
                // @ts-ignore
                this.dispatchEvent(new RTCEvent('iceconnectionstatechange'));
                if (ev.iceConnectionState === 'closed') {
                    // This PeerConnection is done, clean up event handlers.
                    this._unregisterEvents();
                }
            }),
            EventEmitter.addListener('peerConnectionStateChanged', ev => {
                if (ev.id !== this._peerConnectionId) {
                    return;
                }
                this.connectionState = ev.connectionState;
                // @ts-ignore
                this.dispatchEvent(new RTCEvent('connectionstatechange'));
                if (ev.connectionState === 'closed') {
                    // This PeerConnection is done, clean up event handlers.
                    this._unregisterEvents();
                }
            }),
            EventEmitter.addListener('peerConnectionSignalingStateChanged', ev => {
                if (ev.id !== this._peerConnectionId) {
                    return;
                }
                this.signalingState = ev.signalingState;
                // @ts-ignore
                this.dispatchEvent(new RTCEvent('signalingstatechange'));
            }),
            EventEmitter.addListener('peerConnectionAddedStream', ev => {
                if (ev.id !== this._peerConnectionId) {
                    return;
                }
                const stream = new MediaStream(ev);
                this._remoteStreams.push(stream);
                this.remoteDescription = new RTCSessionDescription(ev.sdp);
                // @ts-ignore
                this.dispatchEvent(new MediaStreamEvent('addstream', { stream }));
            }),
            EventEmitter.addListener('peerConnectionRemovedStream', ev => {
                if (ev.id !== this._peerConnectionId) {
                    return;
                }
                const stream = this._remoteStreams.find(s => s._reactTag === ev.streamId);
                if (stream) {
                    const index = this._remoteStreams.indexOf(stream);
                    if (index !== -1) {
                        this._remoteStreams.splice(index, 1);
                    }
                }
                this.remoteDescription = new RTCSessionDescription(ev.sdp);
                // @ts-ignore
                this.dispatchEvent(new MediaStreamEvent('removestream', { stream }));
            }),
            EventEmitter.addListener('mediaStreamTrackMuteChanged', ev => {
                if (ev.peerConnectionId !== this._peerConnectionId) {
                    return;
                }
                let track;
                for (const stream of this._remoteStreams) {
                    const t = stream._tracks.find(track => track.id === ev.trackId);
                    if (t) {
                        track = t;
                        break;
                    }
                }
                if (track) {
                    track._muted = ev.muted;
                    const eventName = ev.muted ? 'mute' : 'unmute';
                    track.dispatchEvent(new MediaStreamTrackEvent(eventName, { track }));
                }
            }),
            EventEmitter.addListener('peerConnectionGotICECandidate', ev => {
                if (ev.id !== this._peerConnectionId) {
                    return;
                }
                this.localDescription = new RTCSessionDescription(ev.sdp);
                const candidate = new RTCIceCandidate(ev.candidate);
                // @ts-ignore
                this.dispatchEvent(new RTCIceCandidateEvent('icecandidate', { candidate }));
            }),
            EventEmitter.addListener('peerConnectionIceGatheringChanged', ev => {
                if (ev.id !== this._peerConnectionId) {
                    return;
                }
                this.iceGatheringState = ev.iceGatheringState;

                if (this.iceGatheringState === 'complete') {
                    this.localDescription = new RTCSessionDescription(ev.sdp);
                    // @ts-ignore
                    this.dispatchEvent(new RTCIceCandidateEvent('icecandidate', { candidate: null }));
                }

                // @ts-ignore
                this.dispatchEvent(new RTCEvent('icegatheringstatechange'));
            }),
            EventEmitter.addListener('peerConnectionDidOpenDataChannel', ev => {
                if (ev.id !== this._peerConnectionId) {
                    return;
                }
                const channel = new RTCDataChannel(ev.dataChannel);
                // @ts-ignore
                this.dispatchEvent(new RTCDataChannelEvent('datachannel', { channel }));
            })
        ];
    }

    /**
     * Creates a new RTCDataChannel object with the given label. The
     * RTCDataChannelInit dictionary can be used to configure properties of the
     * underlying channel such as data reliability.
     *
     * @param {string} label - the value with which the label attribute of the new
     * instance is to be initialized
     * @param {RTCDataChannelInit} dataChannelDict - an optional dictionary of
     * values with which to initialize corresponding attributes of the new
     * instance such as id
     */
    createDataChannel(label: string, dataChannelDict?: RTCDataChannelInit) {
        if (dataChannelDict && 'id' in dataChannelDict) {
            const id = dataChannelDict.id;
            if (typeof id !== 'number') {
                throw new TypeError('DataChannel id must be a number: ' + id);
            }
        }

        const channelInfo = WebRTCModule.createDataChannel(this._peerConnectionId, label, dataChannelDict);

        if (channelInfo === null) {
            throw new TypeError('Failed to create new DataChannel');
        }

        return new RTCDataChannel(channelInfo);
    }
}
