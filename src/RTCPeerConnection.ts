
import { defineCustomEventTarget } from 'event-target-shim';
import { NativeModules } from 'react-native';

import { addListener, removeListener } from './EventEmitter';
import Logger from './Logger';
import MediaStream from './MediaStream';
import MediaStreamTrack from './MediaStreamTrack';
import MediaStreamTrackEvent from './MediaStreamTrackEvent';
import RTCDataChannel from './RTCDataChannel';
import RTCDataChannelEvent from './RTCDataChannelEvent';
import RTCErrorEvent from './RTCErrorEvent';
import RTCEvent from './RTCEvent';
import RTCIceCandidate from './RTCIceCandidate';
import RTCIceCandidateEvent from './RTCIceCandidateEvent';
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

    _peerConnectionId: number;
    _transceivers: { order: number, transceiver: RTCRtpTransceiver }[] = [];
    _remoteStreams: Map<string, MediaStream> = new Map<string, MediaStream>();

    constructor(configuration) {
        super();
        this._peerConnectionId = nextPeerConnectionId++;
        WebRTCModule.peerConnectionInit(configuration, this._peerConnectionId);
        this._registerEvents();

        log.debug(`${this._peerConnectionId} ctor`);
    }

    createOffer(options) {
        log.debug(`${this._peerConnectionId} createOffer`);

        return new Promise((resolve, reject) => {
            WebRTCModule.peerConnectionCreateOffer(
                this._peerConnectionId,
                RTCUtil.normalizeOfferOptions(options),
                (successful, data) => {
                    if (successful) {
                        log.debug(`${this._peerConnectionId} createOffer OK`);

                        this._updateTransceivers(data.transceiversInfo);
                        resolve(data.sdpInfo);
                    } else {
                        log.debug(`${this._peerConnectionId} createOffer ERROR`);

                        reject(data);
                    }
                }
            );
        });
    }

    createAnswer() {
        log.debug(`${this._peerConnectionId} createAnswer`);

        return new Promise((resolve, reject) => {
            WebRTCModule.peerConnectionCreateAnswer(
                this._peerConnectionId,
                {},
                (successful, data) => {
                    if (successful) {
                        log.debug(`${this._peerConnectionId} createAnswer OK`);

                        this._updateTransceivers(data.transceiversInfo);
                        resolve(data.sdpInfo);
                    } else {
                        log.debug(`${this._peerConnectionId} createAnswer ERROR`);

                        reject(data);
                    }
                }
            );
        });
    }

    setConfiguration(configuration): void {
        WebRTCModule.peerConnectionSetConfiguration(configuration, this._peerConnectionId);
    }

    async setLocalDescription(sessionDescription?: RTCSessionDescription | RTCSessionDescriptionInit): Promise<void> {
        log.debug(`${this._peerConnectionId} setLocalDescription`);

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
        } = await WebRTCModule.peerConnectionSetLocalDescription(this._peerConnectionId, desc);

        this.localDescription = new RTCSessionDescription(sdpInfo);
        this._updateTransceivers(transceiversInfo);

        log.debug(`${this._peerConnectionId} setLocalDescription OK`);
    }

    setRemoteDescription(sessionDescription: RTCSessionDescription | RTCSessionDescriptionInit): Promise<void> {
        log.debug(`${this._peerConnectionId} setRemoteDescription`);

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
                this._peerConnectionId,
                (successful, data) => {
                    if (successful) {
                        log.debug(`${this._peerConnectionId} setRemoteDescription OK`);

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
        log.debug(`${this._peerConnectionId} addIceCandidate`);

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

    /**
     * @brief Adds a new track to the {@link RTCPeerConnection},
     * and indicates that it is contained in the specified {@link MediaStream} array.
     * This method has to be synchronous as the W3C API expects a track to be return
     * @param {MediaStreamTrack} track The track to be added
     * @param {MediaStream[]} streams An array of streams the track needs to be added to
     * https://w3c.github.io/webrtc-pc/#dom-rtcpeerconnection-addtrack
     */
    addTrack(track: MediaStreamTrack, streams: MediaStream[] = []): RTCRtpSender {
        log.debug(`${this._peerConnectionId} addTrack`);

        if (this.connectionState === 'closed') {
            throw new Error('Peer Connection is closed');
        }

        if (this._trackExists(track)) {
            throw new Error('Track already exists in a sender');
        }

        const streamIds = streams.map(s => s.id);
        const result = WebRTCModule.peerConnectionAddTrack(this._peerConnectionId, track.id, { streamIds });

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
        log.debug(`${this._peerConnectionId} addTransceiver`);

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

        const result = WebRTCModule.peerConnectionAddTransceiver(this._peerConnectionId, { ...src, init: { ...init } });

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
        log.debug(`${this._peerConnectionId} removeTrack`);

        if (this._peerConnectionId !== sender._peerConnectionId) {
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

        WebRTCModule.peerConnectionRemoveTrack(this._peerConnectionId, sender.id);
    }

    getStats() {
        log.debug(`${this._peerConnectionId} getStats`);

        return WebRTCModule.peerConnectionGetStats(this._peerConnectionId).then(data =>
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
        log.debug(`${this._peerConnectionId} close`);

        // According to the W3C spec: https://w3c.github.io/webrtc-pc/#rtcpeerconnection-interface
        // transceivers have to be stopped
        this._transceivers.forEach(({ transceiver })=> {
            transceiver.stop();
        });
        WebRTCModule.peerConnectionClose(this._peerConnectionId);
    }

    restartIce(): void {
        WebRTCModule.peerConnectionRestartIce(this._peerConnectionId);
    }

    _registerEvents(): void {
        addListener(this, 'peerConnectionOnRenegotiationNeeded', (ev: any) => {
            if (ev.id !== this._peerConnectionId) {
                return;
            }

            // @ts-ignore
            this.dispatchEvent(new RTCEvent('negotiationneeded'));
        });

        addListener(this, 'peerConnectionIceConnectionChanged', (ev: any) => {
            if (ev.id !== this._peerConnectionId) {
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
            if (ev.id !== this._peerConnectionId) {
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
            if (ev.id !== this._peerConnectionId) {
                return;
            }

            this.signalingState = ev.signalingState;
            // @ts-ignore
            this.dispatchEvent(new RTCEvent('signalingstatechange'));
        });

        // Consider moving away from this event: https://github.com/WebKit/WebKit/pull/3953
        addListener(this, 'peerConnectionOnTrack', (ev: any) => {
            if (ev.id !== this._peerConnectionId) {
                return;
            }

            log.debug(`${this._peerConnectionId} ontrack`);

            const track = new MediaStreamTrack(ev.receiver.track);
            let transceiver;

            // Make sure transceivers are stored in timestamp order. Also, we have to make
            // sure we do not add a transceiver if it exists.
            const [ { transceiver: oldTransceiver } = { transceiver: null } ]
                    = this._transceivers.filter(({ transceiver }) => transceiver.id === ev.transceiver.id);

            if (!oldTransceiver) {
                // Creating objects out of the event data.
                const sender = new RTCRtpSender({ ...ev.transceiver.sender });
                const receiver = new RTCRtpReceiver({ ...ev.receiver, track });

                transceiver = new RTCRtpTransceiver({ ...ev.transceiver, receiver, sender });
                this._insertTransceiverSorted(ev.transceiverOrder, transceiver);
            } else {
                transceiver = oldTransceiver;
                transceiver._receiver._track = track;
                transceiver._mid = ev.transceiver.mid;
                transceiver._currentDirection = ev.transceiver.currentDirection;
                transceiver._direction = ev.transceiver.direction;
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

                stream?._tracks.push(track);

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
        });

        addListener(this, 'peerConnectionOnRemoveTrack', (ev: any) => {
            if (ev.peerConnectionId !== this._peerConnectionId) {
                return;
            }

            log.debug(`${this._peerConnectionId} onremovetrack`);

            // Based on the W3C specs https://w3c.github.io/webrtc-pc/#dom-rtcpeerconnection-removetrack,
            // we need to remove the track from any media streams
            // that were previously passed to the `track` event.
            for (const stream of this._remoteStreams.values()) {
                const trackIdx = stream._tracks.findIndex(t => t.id === ev.trackId);

                if (trackIdx !== -1) {
                    stream._tracks.splice(trackIdx, 1);
                }
            }
        });

        addListener(this, 'peerConnectionOnRemoveTrackSuccessful', (ev: any) => {
            if (ev.peerConnectionId !== this._peerConnectionId) {
                return;
            }

            const [ existingSender ] = this
                .getSenders()
                .filter(s => s.id === ev.senderId);

            const oldTrack = existingSender._track;

            if (oldTrack) {
                oldTrack._muted = true;
            }

            existingSender._track = null;

            const [ existingTransceiver ] = this
                .getTransceivers()
                .filter(t => t.sender.id === existingSender.id);

            existingTransceiver._direction = existingTransceiver.direction === 'sendrecv' ? 'recvonly' : 'inactive';
        });

        addListener(this, 'peerConnectionGotICECandidate', (ev: any) => {
            if (ev.id !== this._peerConnectionId) {
                return;
            }

            this.localDescription = new RTCSessionDescription(ev.sdp);
            const candidate = new RTCIceCandidate(ev.candidate);

            // @ts-ignore
            this.dispatchEvent(new RTCIceCandidateEvent('icecandidate', { candidate }));
        });

        addListener(this, 'peerConnectionIceGatheringChanged', (ev: any) => {
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
        });

        addListener(this, 'peerConnectionDidOpenDataChannel', (ev: any) => {
            if (ev.id !== this._peerConnectionId) {
                return;
            }

            const channel = new RTCDataChannel(ev.dataChannel);

            // @ts-ignore
            this.dispatchEvent(new RTCDataChannelEvent('datachannel', { channel }));
        });

        // Since the current underlying architecture performs certain actions
        // Asynchronously when the outer web API expects synchronous behaviour
        // This is the only way to report error on operations for those who wish
        // to handle them.
        addListener(this, 'peerConnectionOnError', (ev: any) => {
            if (ev.info.peerConnectionId !== this._peerConnectionId) {
                return;
            }

            log.error(`onerror: ${JSON.stringify(ev)}`);

            // @ts-ignore
            this.dispatchEvent(new RTCErrorEvent('error', ev.func, ev.message));
        });

        addListener(this, 'mediaStreamTrackMuteChanged', (ev: any) => {
            if (ev.peerConnectionId !== this._peerConnectionId) {
                return;
            }

            const [
                track
            ] = this.getReceivers().map(r => r.track).filter(t => t?.id === ev.trackId);

            if (track) {
                track._muted = ev.muted;
                const eventName = ev.muted ? 'mute' : 'unmute';

                // @ts-ignore
                track.dispatchEvent(new MediaStreamTrackEvent(eventName, { track }));
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

        const channelInfo = WebRTCModule.createDataChannel(this._peerConnectionId, label, dataChannelDict);

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
     * updates transceivers after offer/answer updates if necessary
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
