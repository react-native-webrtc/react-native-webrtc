
import { defineCustomEventTarget } from 'event-target-shim';
import { NativeModules } from 'react-native';

import MediaStream from './MediaStream';
import MediaStreamEvent from './MediaStreamEvent';
import MediaStreamTrackEvent from './MediaStreamTrackEvent';
import MediaStreamTrack from './MediaStreamTrack';
import RTCRtpTransceiver from './RTCRtpTransceiver';
import RTCDataChannel from './RTCDataChannel';
import RTCDataChannelEvent from './RTCDataChannelEvent';
import RTCTrackEvent from './RTCTrackEvent';
import RTCSessionDescription from './RTCSessionDescription';
import RTCIceCandidate from './RTCIceCandidate';
import RTCIceCandidateEvent from './RTCIceCandidateEvent';
import RTCErrorEvent from './RTCErrorEvent';
import RTCEvent from './RTCEvent';
import * as RTCUtil from './RTCUtil';
import { addListener, removeListener } from './EventEmitter';
import RTCRtpReceiver from './RTCRtpReceiver';
import RTCRtpSender from './RTCRtpSender';

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
    _subscriptions: any[] = [];
    _transceivers: { timestamp: number, transceiver: RTCRtpTransceiver }[] = [];
    _remoteStreams: { [id: string]: MediaStream } = {};

    constructor(configuration) {
        super();
        this._peerConnectionId = nextPeerConnectionId++;
        WebRTCModule.peerConnectionInit(configuration, this._peerConnectionId);
        this._registerEvents();
    }

    createOffer(options) {
        return new Promise((resolve, reject) => {
            WebRTCModule.peerConnectionCreateOffer(
                this._peerConnectionId,
                RTCUtil.normalizeOfferOptions(options),
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

    createAnswer() {
        return new Promise((resolve, reject) => {
            WebRTCModule.peerConnectionCreateAnswer(
                this._peerConnectionId,
                {},
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

    setConfiguration(configuration): void {
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


    addTransceiver(source: 'audio' | 'video' | MediaStreamTrack, init): RTCRtpTransceiver {
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
            init.streamIds = init.streams.map((stream) => {
                return stream.id;
            });
        }

        const result = WebRTCModule.peerConnectionAddTransceiver(this._peerConnectionId, { ...src, init: { ...init } });
        if (result == null) {
            console.log("Error adding transceiver");
            throw new Error("Transceiver could not be added");
        }
        const transceiver = new RTCRtpTransceiver(result.transceiver);
        this._transceivers.push({ timestamp: parseInt(result.timestamp), transceiver: transceiver });
        this._transceivers.sort((a, b) => {
            return a.timestamp - b.timestamp;
        })
        return transceiver;
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

    getTransceivers(): RTCRtpTransceiver[] {
        // Return a cached version of transceivers
        if (this._transceivers == null || this._transceivers.length === 0) {
            return [];
        } 
        return this._transceivers.map((element) => {
            return element.transceiver;
        });
    }

    getSenders(): RTCRtpSender[] {
        const transceivers = this.getTransceivers();
        if (transceivers.length === 0) return [];

        let senders: RTCRtpSender[] = [];
        for (var transceiver of transceivers) {
            if (transceiver.sender)
                senders.push(transceiver.sender);
        }
        return senders;
    }

    getReceivers(): RTCRtpReceiver[] {
        const transceivers = this.getTransceivers();
        if (transceivers.length === 0) return [];

        let receivers: RTCRtpReceiver[] = [];
        for (var transceiver of transceivers) {
            if (transceiver.receiver)
                receivers.push(transceiver.receiver);
        }
        return receivers;
    }

    close(): void {
        WebRTCModule.peerConnectionClose(this._peerConnectionId);
        // According to the W3C spec: https://w3c.github.io/webrtc-pc/#rtcpeerconnection-interface
        // transceivers have to be stopped
        this._transceivers.forEach(({ timestamp, transceiver })=> {
            transceiver.stop();
        })
    }

    restartIce(): void {
        WebRTCModule.peerConnectionRestartIce(this._peerConnectionId);
    }

    _registerEvents(): void {
        addListener(this, 'peerConnectionOnRenegotiationNeeded', ev => {
            if (ev.id !== this._peerConnectionId) {
                return;
            }
            // @ts-ignore
            this.dispatchEvent(new RTCEvent('negotiationneeded'));
        });
        addListener(this, 'peerConnectionIceConnectionChanged', ev => {
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
        addListener(this, 'peerConnectionStateChanged', ev => {
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
        addListener(this, 'peerConnectionSignalingStateChanged', ev => {
            if (ev.id !== this._peerConnectionId) {
                return;
            }
            this.signalingState = ev.signalingState;
            // @ts-ignore
            this.dispatchEvent(new RTCEvent('signalingstatechange'));
        })
        addListener(this, 'peerConnectionOnTrack', ev => {
            if (ev.id !== this._peerConnectionId) {
                return;
            }
            const transceiver = (() => {
                // Creating a track and receiver objects
                const track = new MediaStreamTrack(ev.info.track);
                const receiver = new RTCRtpReceiver({ ...ev.info.receiver, track });
                // Make sure transceivers are stored in timestamp order. Also, we have to make
                // sure we do not add a transceiver if it exists. 
                let [{ timestamp, transceiver }] = this._transceivers.filter(({ timestamp, transceiver }) => {
                    return transceiver.id === ev.info.transceiver.id;
                })
                if (!transceiver) {
                    // Creating objects out of the event data
                    const newTransceiver = new RTCRtpTransceiver({ ...ev.info.transceiver, receiver: receiver });
                    this._transceivers.push({ timestamp: parseInt(ev.info.timestamp), transceiver: newTransceiver });
                    this._transceivers.sort((a, b) => {
                        return a.timestamp - b.timestamp;
                    })
                    return newTransceiver;
                } else {
                    transceiver._receiver._track = track;
                    transceiver._mid = ev.info.transceiver.mid;
                    transceiver._currentDirection = ev.info.transceiver.currentDirection;
                    transceiver._direction = ev.info.transceiver.direction;
                    return transceiver;
                }
            })();

            // Get the stream object from the event. Create if necessary.
            const streams = ev.info.streams.map(stream => {
                // Here we are making sure that we don't create stream objects that already exist
                // So that event listeners do get the same object if it has been created before.
                if (!(stream.streamId in this._remoteStreams)) {
                    this._remoteStreams[stream.streamId] = new MediaStream(stream);
                }
                return this._remoteStreams[stream.streamId];
            });

            console.log("transceiver is", transceiver);
            // @ts-ignore
            this.dispatchEvent(new RTCTrackEvent('track', { streams, transceiver }));
        });
        addListener(this, 'peerConnectionGotICECandidate', ev => {
            if (ev.id !== this._peerConnectionId) {
                return;
            }
            this.localDescription = new RTCSessionDescription(ev.sdp);
            const candidate = new RTCIceCandidate(ev.candidate);
            // @ts-ignore
            this.dispatchEvent(new RTCIceCandidateEvent('icecandidate', { candidate }));
        });
        addListener(this, 'peerConnectionIceGatheringChanged', ev => {
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
        addListener(this, 'peerConnectionDidOpenDataChannel', ev => {
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
        addListener(this, 'peerConnectionOnError', ev => {
            if (ev.info.peerConnectionId !== this._peerConnectionId) {
                return;
            }
            console.log("Received peer connection error event: ", ev);
            // @ts-ignore
            this.dispatchEvent(new RTCErrorEvent('error', ev.func, ev.message));
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
}
