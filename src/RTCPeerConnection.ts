
import { defineCustomEventTarget } from 'event-target-shim';
import { NativeModules } from 'react-native';

import { addListener, removeListener } from './EventEmitter';
import Logger from './Logger';
import MediaStream from './MediaStream';
import MediaStreamTrack from './MediaStreamTrack';
import RTCDataChannel from './RTCDataChannel';
import RTCDataChannelEvent from './RTCDataChannelEvent';
import RTCEvent from './RTCEvent';
import RTCIceCandidate from './RTCIceCandidate';
import RTCIceCandidateEvent from './RTCIceCandidateEvent';
import RTCRtpReceiveParameters from './RTCRtpReceiveParameters';
import RTCRtpReceiver from './RTCRtpReceiver';
import RTCRtpSendParameters from './RTCRtpSendParameters';
import RTCRtpSender from './RTCRtpSender';
import RTCRtpTransceiver from './RTCRtpTransceiver';
import RTCSessionDescription, { RTCSessionDescriptionInit } from './RTCSessionDescription';
import RTCTrackEvent from './RTCTrackEvent';
import * as RTCUtil from './RTCUtil';

const log = new Logger('pc');
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
    'track',
    'error'
];

let nextPeerConnectionId = 0;

export default class RTCPeerConnection extends defineCustomEventTarget(...PEER_CONNECTION_EVENTS) {
    localDescription: RTCSessionDescription | null = null;
    remoteDescription: RTCSessionDescription | null = null;

    signalingState: RTCSignalingState = 'stable';
    iceGatheringState: RTCIceGatheringState = 'new';
    connectionState: RTCPeerConnectionState = 'new';
    iceConnectionState: RTCIceConnectionState = 'new';

    _pcId: number;
    _transceivers: { order: number, transceiver: RTCRtpTransceiver }[] = [];
    _remoteStreams: Map<string, MediaStream> = new Map<string, MediaStream>();

    constructor(configuration) {
        super();
        this._pcId = nextPeerConnectionId++;
        WebRTCModule.peerConnectionInit(configuration, this._pcId);
        this._registerEvents();

        log.debug(`${this._pcId} ctor`);
    }

    createOffer(options) {
        log.debug(`${this._pcId} createOffer`);

        return new Promise((resolve, reject) => {
            WebRTCModule.peerConnectionCreateOffer(
                this._pcId,
                RTCUtil.normalizeOfferOptions(options),
                (successful, data) => {
                    if (successful) {
                        log.debug(`${this._pcId} createOffer OK`);

                        this._updateTransceivers(data.transceiversInfo);
                        resolve(data.sdpInfo);
                    } else {
                        log.debug(`${this._pcId} createOffer ERROR`);

                        reject(data);
                    }
                }
            );
        });
    }

    createAnswer() {
        log.debug(`${this._pcId} createAnswer`);

        return new Promise((resolve, reject) => {
            WebRTCModule.peerConnectionCreateAnswer(
                this._pcId,
                {},
                (successful, data) => {
                    if (successful) {
                        log.debug(`${this._pcId} createAnswer OK`);

                        this._updateTransceivers(data.transceiversInfo);
                        resolve(data.sdpInfo);
                    } else {
                        log.debug(`${this._pcId} createAnswer ERROR`);

                        reject(data);
                    }
                }
            );
        });
    }

    setConfiguration(configuration): void {
        WebRTCModule.peerConnectionSetConfiguration(configuration, this._pcId);
    }

    async setLocalDescription(sessionDescription?: RTCSessionDescription | RTCSessionDescriptionInit): Promise<void> {
        log.debug(`${this._pcId} setLocalDescription`);

        let desc;

        if (sessionDescription) {
            desc = {
                type: sessionDescription.type,
                sdp: sessionDescription.sdp ?? ''
            };

            if (!RTCUtil.isSdpTypeValid(desc.type)) {
                throw new Error(`Invalid session description: invalid type: ${desc.type}`);
            }
        } else {
            desc = null;
        }

        const {
            sdpInfo,
            transceiversInfo
        } = await WebRTCModule.peerConnectionSetLocalDescription(this._pcId, desc);

        this.localDescription = new RTCSessionDescription(sdpInfo);
        this._updateTransceivers(transceiversInfo);

        log.debug(`${this._pcId} setLocalDescription OK`);
    }

    setRemoteDescription(sessionDescription: RTCSessionDescription | RTCSessionDescriptionInit): Promise<void> {
        log.debug(`${this._pcId} setRemoteDescription`);

        if (!sessionDescription) {
            return Promise.reject(new Error('No session description provided'));
        }

        const desc = {
            type: sessionDescription.type,
            sdp: sessionDescription.sdp ?? ''
        };

        if (!RTCUtil.isSdpTypeValid(desc.type ?? '')) {
            throw new Error(`Invalid session description: invalid type: ${desc.type}`);
        }

        return new Promise((resolve, reject) => {
            WebRTCModule.peerConnectionSetRemoteDescription(
                desc,
                this._pcId,
                (successful, data) => {
                    if (successful) {
                        log.debug(`${this._pcId} setRemoteDescription OK`);

                        this.remoteDescription = new RTCSessionDescription(data.sdpInfo);

                        data.newTransceivers?.forEach( t => {
                            const { transceiverOrder, transceiver } = t;
                            const newSender = new RTCRtpSender(transceiver.sender);
                            const newReceiver = new RTCRtpReceiver(transceiver.receiver);
                            const newTransceiver = new RTCRtpTransceiver({
                                ...transceiver,
                                sender: newSender,
                                receiver: newReceiver,
                            });

                            this._insertTransceiverSorted(transceiverOrder, newTransceiver);
                        });

                        this._updateTransceivers(data.transceiversInfo);

                        resolve();
                    } else {
                        reject(data);
                    }
                }
            );
        });
    }

    async addIceCandidate(candidate): Promise<void> {
        log.debug(`${this._pcId} addIceCandidate`);

        if (!candidate || !candidate.candidate) {
            // XXX end-of cantidates is not implemented: https://bugs.chromium.org/p/webrtc/issues/detail?id=9218
            return;
        }

        const newSdp = await WebRTCModule.peerConnectionAddICECandidate(
            this._pcId,
            candidate.toJSON ? candidate.toJSON() : candidate
        );

        this.remoteDescription = new RTCSessionDescription(newSdp);
    }

    /**
     * @brief Adds a new track to the {@link RTCPeerConnection},
     * and indicates that it is contained in the specified {@link MediaStream}s.
     * This method has to be synchronous as the W3C API expects a track to be returned
     * @param {MediaStreamTrack} track The track to be added
     * @param {...MediaStream} streams One or more {@link MediaStream}s the track needs to be added to
     * https://w3c.github.io/webrtc-pc/#dom-rtcpeerconnection-addtrack
     */
    addTrack(track: MediaStreamTrack, ...streams: MediaStream[]): RTCRtpSender {
        log.debug(`${this._pcId} addTrack`);

        if (this.connectionState === 'closed') {
            throw new Error('Peer Connection is closed');
        }

        if (this._trackExists(track)) {
            throw new Error('Track already exists in a sender');
        }

        const streamIds = streams.map(s => s.id);
        const result = WebRTCModule.peerConnectionAddTrack(this._pcId, track.id, { streamIds });

        if (result === null) {
            throw new Error('Could not add sender');
        }

        const { transceiverOrder, transceiver, sender } = result;

        // According to the W3C docs, the sender could have been reused, and
        // so we check if that is the case, and update accordingly.
        const [ existingSender ] = this
            .getSenders()
            .filter(s => s.id === sender.id);

        if (existingSender) {
            // Update sender
            existingSender._track = track;

            // Update the corresponding transceiver as well
            const [ existingTransceiver ] = this
                .getTransceivers()
                .filter(t => t.sender.id === existingSender.id);

            existingTransceiver._direction = transceiver.direction;
            existingTransceiver._currentDirection = transceiver.currentDirection;

            return existingSender;
        }

        // This is a new transceiver, should create a transceiver for it and add it
        const newSender = new RTCRtpSender({ ...transceiver.sender, track });
        const remoteTrack = transceiver.receiver.track ? new MediaStreamTrack(transceiver.receiver.track) : null;
        const newReceiver = new RTCRtpReceiver({ ...transceiver.receiver, track: remoteTrack });
        const newTransceiver = new RTCRtpTransceiver({
            ...transceiver,
            sender: newSender,
            receiver: newReceiver,
        });

        this._insertTransceiverSorted(transceiverOrder, newTransceiver);

        return newSender;
    }

    addTransceiver(source: 'audio' | 'video' | MediaStreamTrack, init): RTCRtpTransceiver {
        log.debug(`${this._pcId} addTransceiver`);

        let src = {};

        if (source === 'audio') {
            src = { type: 'audio' };
        } else if (source === 'video') {
            src = { type: 'video' };
        } else {
            src = { trackId: source.id };
        }

        // Extract the stream ids
        if (init && init.streams) {
            init.streamIds = init.streams.map(stream => stream.id);
        }

        const result = WebRTCModule.peerConnectionAddTransceiver(this._pcId, { ...src, init: { ...init } });

        if (result === null) {
            throw new Error('Transceiver could not be added');
        }

        const t = result.transceiver;
        let track: MediaStreamTrack | null = null;

        if (typeof source === 'string') {
            if (t.sender.track) {
                track = new MediaStreamTrack(t.sender.track);
            }
        } else {
            // 'source' is a MediaStreamTrack
            track = source;
        }

        const sender = new RTCRtpSender({ ...t.sender, track });
        const remoteTrack = t.receiver.track ? new MediaStreamTrack(t.receiver.track) : null;
        const receiver = new RTCRtpReceiver({ ...t.receiver, track: remoteTrack });
        const transceiver = new RTCRtpTransceiver({
            ...result.transceiver,
            sender,
            receiver
        });

        this._insertTransceiverSorted(result.transceiverOrder, transceiver);

        return transceiver;
    }

    removeTrack(sender: RTCRtpSender) {
        log.debug(`${this._pcId} removeTrack`);

        if (this._pcId !== sender._peerConnectionId) {
            throw new Error('Sender does not belong to this peer connection');
        }

        if (this.connectionState === 'closed') {
            throw new Error('Peer Connection is closed');
        }

        const existingSender = this
            .getSenders()
            .find(s => s === sender);

        if (!existingSender) {
            throw new Error('Sender does not exist');
        }

        if (existingSender.track === null) {
            return;
        }

        // Blocking!
        WebRTCModule.peerConnectionRemoveTrack(this._pcId, sender.id);

        existingSender._track = null;

        const [ existingTransceiver ] = this
            .getTransceivers()
            .filter(t => t.sender.id === existingSender.id);

        existingTransceiver._direction = existingTransceiver.direction === 'sendrecv' ? 'recvonly' : 'inactive';
    }

    getStats() {
        log.debug(`${this._pcId} getStats`);

        return WebRTCModule.peerConnectionGetStats(this._pcId).then(data =>
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
            new Map(JSON.parse(data))
        );
    }

    getTransceivers(): RTCRtpTransceiver[] {
        return this._transceivers.map(e => e.transceiver);
    }

    getSenders(): RTCRtpSender[] {
        return this._transceivers.map(e => e.transceiver.sender);
    }

    getReceivers(): RTCRtpReceiver[] {
        return this._transceivers.map(e => e.transceiver.receiver);
    }

    close(): void {
        log.debug(`${this._pcId} close`);

        WebRTCModule.peerConnectionClose(this._pcId);

        // Mark transceivers as stopped.
        this._transceivers.forEach(({ transceiver })=> {
            transceiver._setStopped();
        });
    }

    restartIce(): void {
        WebRTCModule.peerConnectionRestartIce(this._pcId);
    }

    _registerEvents(): void {
        addListener(this, 'peerConnectionOnRenegotiationNeeded', (ev: any) => {
            if (ev.pcId !== this._pcId) {
                return;
            }

            // @ts-ignore
            this.dispatchEvent(new RTCEvent('negotiationneeded'));
        });

        addListener(this, 'peerConnectionIceConnectionChanged', (ev: any) => {
            if (ev.pcId !== this._pcId) {
                return;
            }

            this.iceConnectionState = ev.iceConnectionState;

            if (ev.iceConnectionState === 'closed') {
                // This PeerConnection is done, clean up event handlers.
                removeListener(this);
            }

            // @ts-ignore
            this.dispatchEvent(new RTCEvent('iceconnectionstatechange'));
        });

        addListener(this, 'peerConnectionStateChanged', (ev: any) => {
            if (ev.pcId !== this._pcId) {
                return;
            }

            this.connectionState = ev.connectionState;

            if (ev.connectionState === 'closed') {
                // This PeerConnection is done, clean up event handlers.
                removeListener(this);
            }

            // @ts-ignore
            this.dispatchEvent(new RTCEvent('connectionstatechange'));
        });

        addListener(this, 'peerConnectionSignalingStateChanged', (ev: any) => {
            if (ev.pcId !== this._pcId) {
                return;
            }

            this.signalingState = ev.signalingState;
            // @ts-ignore
            this.dispatchEvent(new RTCEvent('signalingstatechange'));
        });

        // Consider moving away from this event: https://github.com/WebKit/WebKit/pull/3953
        addListener(this, 'peerConnectionOnTrack', (ev: any) => {
            if (ev.pcId !== this._pcId) {
                return;
            }

            log.debug(`${this._pcId} ontrack`);

            let track;
            let transceiver;

            // Make sure transceivers are stored in timestamp order. Also, we have to make
            // sure we do not add a transceiver if it exists.
            const [ { transceiver: oldTransceiver } = { transceiver: null } ]
                    = this._transceivers.filter(({ transceiver }) => transceiver.id === ev.transceiver.id);

            // We need to fire this event for an existing track sometimes, like
            // when the transceiver direction (on the sending side) switches from
            // sendrecv to recvonly and then back.

            if (oldTransceiver) {
                transceiver = oldTransceiver;
                track = transceiver._receiver._track;
                transceiver._mid = ev.transceiver.mid;
                transceiver._currentDirection = ev.transceiver.currentDirection;
                transceiver._direction = ev.transceiver.direction;
            } else {
                track = new MediaStreamTrack(ev.receiver.track);
                const sender = new RTCRtpSender({ ...ev.transceiver.sender });
                const receiver = new RTCRtpReceiver({ ...ev.receiver, track });

                transceiver = new RTCRtpTransceiver({ ...ev.transceiver, receiver, sender });
                this._insertTransceiverSorted(ev.transceiverOrder, transceiver);
            }

            // Get the stream object from the event. Create if necessary.
            const streams = ev.streams.map(streamInfo => {
                // Here we are making sure that we don't create stream objects that already exist
                // So that event listeners do get the same object if it has been created before.
                if (!this._remoteStreams.has(streamInfo.streamId)) {
                    const stream = new MediaStream({
                        streamId: streamInfo.streamId,
                        streamReactTag: streamInfo.streamReactTag,
                        tracks: []
                    });

                    this._remoteStreams.set(streamInfo.streamId, stream);
                }

                const stream = this._remoteStreams.get(streamInfo.streamId);

                if (!stream?._tracks.includes(track)) {
                    stream?._tracks.push(track);
                }

                return stream;
            });

            const eventData = {
                streams,
                transceiver,
                track,
                receiver: transceiver.receiver
            };

            // @ts-ignore
            this.dispatchEvent(new RTCTrackEvent('track', eventData));

            // Dispatch an unmute event for the track.
            track._setMutedInternal(false);
        });

        addListener(this, 'peerConnectionOnRemoveTrack', (ev: any) => {
            if (ev.pcId !== this._pcId) {
                return;
            }

            log.debug(`${this._pcId} onremovetrack`);

            // As per the spec:
            // - Remove the track from any media streams that were previously passed to the `track` event.
            // https://w3c.github.io/webrtc-pc/#dom-rtcpeerconnection-removetrack,
            // - Mark the track as muted:
            // https://w3c.github.io/webrtc-pc/#process-remote-track-removal
            for (const stream of this._remoteStreams.values()) {
                const [ track ] = stream._tracks.filter(t => t.id === ev.trackId);

                if (track) {
                    const trackIdx = stream._tracks.indexOf(track);

                    stream._tracks.splice(trackIdx, 1);

                    // Dispatch a mute event for the track.
                    track._setMutedInternal(true);
                }
            }
        });

        addListener(this, 'peerConnectionGotICECandidate', (ev: any) => {
            if (ev.pcId !== this._pcId) {
                return;
            }

            this.localDescription = new RTCSessionDescription(ev.sdp);
            const candidate = new RTCIceCandidate(ev.candidate);

            // @ts-ignore
            this.dispatchEvent(new RTCIceCandidateEvent('icecandidate', { candidate }));
        });

        addListener(this, 'peerConnectionIceGatheringChanged', (ev: any) => {
            if (ev.pcId !== this._pcId) {
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
        });

        addListener(this, 'peerConnectionDidOpenDataChannel', (ev: any) => {
            if (ev.pcId !== this._pcId) {
                return;
            }

            const channel = new RTCDataChannel(ev.dataChannel);

            // @ts-ignore
            this.dispatchEvent(new RTCDataChannelEvent('datachannel', { channel }));
        });

        addListener(this, 'mediaStreamTrackMuteChanged', (ev: any) => {
            if (ev.pcId !== this._pcId) {
                return;
            }

            const [
                track
            ] = this.getReceivers().map(r => r.track).filter(t => t?.id === ev.trackId);

            if (track) {
                track._setMutedInternal(ev.muted);
            }
        });
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
    createDataChannel(label: string, dataChannelDict?: RTCDataChannelInit): RTCDataChannel {
        if (dataChannelDict && 'id' in dataChannelDict) {
            const id = dataChannelDict.id;

            if (typeof id !== 'number') {
                throw new TypeError('DataChannel id must be a number: ' + id);
            }
        }

        const channelInfo = WebRTCModule.createDataChannel(this._pcId, label, dataChannelDict);

        if (channelInfo === null) {
            throw new TypeError('Failed to create new DataChannel');
        }

        return new RTCDataChannel(channelInfo);
    }

    /**
     * Check whether a media stream track exists already in a sender.
     * See https://w3c.github.io/webrtc-pc/#dom-rtcpeerconnection-addtrack for more information
     */
    _trackExists(track: MediaStreamTrack): boolean {
        const [ sender ] = this
            .getSenders()
            .filter(
                sender => sender.track?.id === track.id
            );

        return sender? true : false;
    }

    /**
     * Updates transceivers after offer/answer updates if necessary.
     */
    _updateTransceivers(transceiverUpdates) {
        for (const update of transceiverUpdates) {
            const [ transceiver ] = this
                .getTransceivers()
                .filter(t => t.id === update.transceiverId);

            if (!transceiver) {
                continue;
            }

            transceiver._currentDirection = update.currentDirection;
            transceiver._mid = update.mid;
            transceiver._sender._rtpParameters = new RTCRtpSendParameters(update.senderRtpParameters);
            transceiver._receiver._rtpParameters = new RTCRtpReceiveParameters(update.receiverRtpParameters);
        }
    }

    /**
     * Inserts transceiver into the transceiver array in the order they are created (timestamp).
     * @param order an index that refers to when it it was created relatively.
     * @param transceiver the transceiver object to be inserted.
     */
    _insertTransceiverSorted(order: number, transceiver: RTCRtpTransceiver) {
        this._transceivers.push({ order, transceiver });
        this._transceivers.sort((a, b) => a.order - b.order);
    }
}
