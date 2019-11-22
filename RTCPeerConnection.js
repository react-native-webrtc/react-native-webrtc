'use strict';

import EventTarget from 'event-target-shim';
import {DeviceEventEmitter, NativeModules} from 'react-native';

import MediaStream from './MediaStream';
import MediaStreamEvent from './MediaStreamEvent';
import MediaStreamTrack from './MediaStreamTrack';
import MediaStreamTrackEvent from './MediaStreamTrackEvent';
import RTCDataChannel from './RTCDataChannel';
import RTCDataChannelEvent from './RTCDataChannelEvent';
import RTCSessionDescription from './RTCSessionDescription';
import RTCIceCandidate from './RTCIceCandidate';
import RTCIceCandidateEvent from './RTCIceCandidateEvent';
import RTCEvent from './RTCEvent';
import * as RTCUtil from './RTCUtil';

const {WebRTCModule} = NativeModules;

type RTCSignalingState =
  'stable' |
  'have-local-offer' |
  'have-remote-offer' |
  'have-local-pranswer' |
  'have-remote-pranswer' |
  'closed';

type RTCIceGatheringState =
  'new' |
  'gathering' |
  'complete';

type RTCIceConnectionState =
  'new' |
  'checking' |
  'connected' |
  'completed' |
  'failed' |
  'disconnected' |
  'closed';

const PEER_CONNECTION_EVENTS = [
  'connectionstatechange',
  'icecandidate',
  'icecandidateerror',
  'iceconnectionstatechange',
  'icegatheringstatechange',
  'negotiationneeded',
  'signalingstatechange',
  // Peer-to-peer Data API:
  'datachannel',
  // old:
  'addstream',
  'removestream',
];

let nextPeerConnectionId = 0;

export default class RTCPeerConnection extends EventTarget(PEER_CONNECTION_EVENTS) {
  localDescription: RTCSessionDescription;
  remoteDescription: RTCSessionDescription;

  signalingState: RTCSignalingState = 'stable';
  iceGatheringState: RTCIceGatheringState = 'new';
  iceConnectionState: RTCIceConnectionState = 'new';

  onconnectionstatechange: ?Function;
  onicecandidate: ?Function;
  onicecandidateerror: ?Function;
  oniceconnectionstatechange: ?Function;
  onicegatheringstatechange: ?Function;
  onnegotiationneeded: ?Function;
  onsignalingstatechange: ?Function;

  onaddstream: ?Function;
  onremovestream: ?Function;

  _peerConnectionId: number;
  _localStreams: Array<MediaStream> = [];
  _remoteStreams: Array<MediaStream> = [];
  _subscriptions: Array<any>;

  /**
   * The RTCDataChannel.id allocator of this RTCPeerConnection.
   */
  _dataChannelIds: Set = new Set();

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
            resolve(new RTCSessionDescription(data));
          } else {
            reject(data); // TODO: convert to NavigatorUserMediaError
          }
        });
    });
  }

  createAnswer(options = {}) {
    return new Promise((resolve, reject) => {
      WebRTCModule.peerConnectionCreateAnswer(
        this._peerConnectionId,
        RTCUtil.normalizeOfferAnswerOptions(options),
        (successful, data) => {
          if (successful) {
            resolve(new RTCSessionDescription(data));
          } else {
            reject(data);
          }
        });
    });
  }

  setConfiguration(configuration) {
    WebRTCModule.peerConnectionSetConfiguration(configuration, this._peerConnectionId);
  }

  setLocalDescription(sessionDescription: RTCSessionDescription) {
    return new Promise((resolve, reject) => {
      WebRTCModule.peerConnectionSetLocalDescription(
        sessionDescription.toJSON ? sessionDescription.toJSON() : sessionDescription,
        this._peerConnectionId,
        (successful, data) => {
          if (successful) {
            this.localDescription = sessionDescription;
            resolve();
          } else {
            reject(data);
          }
      });
    });
  }

  setRemoteDescription(sessionDescription: RTCSessionDescription) {
    return new Promise((resolve, reject) => {
      WebRTCModule.peerConnectionSetRemoteDescription(
        sessionDescription.toJSON ? sessionDescription.toJSON() : sessionDescription,
        this._peerConnectionId,
        (successful, data) => {
          if (successful) {
            this.remoteDescription = sessionDescription;
            resolve();
          } else {
            reject(data);
          }
      });
    });
  }

  addIceCandidate(candidate) {
    return new Promise((resolve, reject) => {
      WebRTCModule.peerConnectionAddICECandidate(
        candidate.toJSON ? candidate.toJSON() : candidate,
        this._peerConnectionId,
        (successful) => {
          if (successful) {
            resolve()
          } else {
            // XXX: This should be OperationError
            reject(new Error('Failed to add ICE candidate'));
          }
      });
    });
  }

  getStats(track) {
    // NOTE: This returns a Promise but the format of the results is still
    // the "legacy" one. The native side (in Oobj-C) doesn't yet support the
    // new format: https://bugs.chromium.org/p/webrtc/issues/detail?id=6872
    return new Promise((resolve, reject) => {
      WebRTCModule.peerConnectionGetStats(
        (track && track.id) || '',
        this._peerConnectionId,
        (success, data) => {
          if (success) {
            // On both Android and iOS it is faster to construct a single
            // JSON string representing the array of StatsReports and have it
            // pass through the React Native bridge rather than the array of
            // StatsReports. While the implementations do try to be faster in
            // general, the stress is on being faster to pass through the React
            // Native bridge which is a bottleneck that tends to be visible in
            // the UI when there is congestion involving UI-related passing.
            try {
              const stats = JSON.parse(data);
              resolve(stats);
            } catch (e) {
              reject(e);
            }
          } else {
            reject(new Error(data));
          }
        });
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

  _getTrack(streamReactTag, trackId): MediaStreamTrack {
    const stream
      = this._remoteStreams.find(
          stream => stream._reactTag === streamReactTag);

    return stream && stream._tracks.find(track => track.id === trackId);
  }

  _unregisterEvents(): void {
    this._subscriptions.forEach(e => e.remove());
    this._subscriptions = [];
  }

  _registerEvents(): void {
    this._subscriptions = [
      DeviceEventEmitter.addListener('peerConnectionOnRenegotiationNeeded', ev => {
        if (ev.id !== this._peerConnectionId) {
          return;
        }
        this.dispatchEvent(new RTCEvent('negotiationneeded'));
      }),
      DeviceEventEmitter.addListener('peerConnectionIceConnectionChanged', ev => {
        if (ev.id !== this._peerConnectionId) {
          return;
        }
        this.iceConnectionState = ev.iceConnectionState;
        this.dispatchEvent(new RTCEvent('iceconnectionstatechange'));
        if (ev.iceConnectionState === 'closed') {
          // This PeerConnection is done, clean up event handlers.
          this._unregisterEvents();
        }
      }),
      DeviceEventEmitter.addListener('peerConnectionSignalingStateChanged', ev => {
        if (ev.id !== this._peerConnectionId) {
          return;
        }
        this.signalingState = ev.signalingState;
        this.dispatchEvent(new RTCEvent('signalingstatechange'));
      }),
      DeviceEventEmitter.addListener('peerConnectionAddedStream', ev => {
        if (ev.id !== this._peerConnectionId) {
          return;
        }
        const stream = new MediaStream(ev);
        this._remoteStreams.push(stream);
        this.dispatchEvent(new MediaStreamEvent('addstream', {stream}));
      }),
      DeviceEventEmitter.addListener('peerConnectionRemovedStream', ev => {
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
        this.dispatchEvent(new MediaStreamEvent('removestream', {stream}));
      }),
      DeviceEventEmitter.addListener('mediaStreamTrackMuteChanged', ev => {
        if (ev.peerConnectionId !== this._peerConnectionId) {
          return;
        }
        const track = this._getTrack(ev.streamReactTag, ev.trackId);
        if (track) {
          track.muted = ev.muted;
          const eventName = ev.muted ? 'mute' : 'unmute';
          track.dispatchEvent(new MediaStreamTrackEvent(eventName, {track}));
        }
      }),
      DeviceEventEmitter.addListener('peerConnectionGotICECandidate', ev => {
        if (ev.id !== this._peerConnectionId) {
          return;
        }
        const candidate = new RTCIceCandidate(ev.candidate);
        const event = new RTCIceCandidateEvent('icecandidate', {candidate});
        this.dispatchEvent(event);
      }),
      DeviceEventEmitter.addListener('peerConnectionIceGatheringChanged', ev => {
        if (ev.id !== this._peerConnectionId) {
          return;
        }
        this.iceGatheringState = ev.iceGatheringState;

        if (this.iceGatheringState === 'complete') {
          this.dispatchEvent(new RTCIceCandidateEvent('icecandidate', null));
        }

        this.dispatchEvent(new RTCEvent('icegatheringstatechange'));
      }),
      DeviceEventEmitter.addListener('peerConnectionDidOpenDataChannel', ev => {
        if (ev.id !== this._peerConnectionId) {
          return;
        }
        const evDataChannel = ev.dataChannel;
        const id = evDataChannel.id;
        // XXX RTP data channels are not defined by the WebRTC standard, have
        // been deprecated in Chromium, and Google have decided (in 2015) to no
        // longer support them (in the face of multiple reported issues of
        // breakages).
        if (typeof id !== 'number' || id === -1) {
          return;
        }
        const channel
          = new RTCDataChannel(
              this._peerConnectionId,
              evDataChannel.label,
              evDataChannel);
        // XXX webrtc::PeerConnection checked that id was not in use in its own
        // SID allocator before it invoked us. Additionally, its own SID
        // allocator is the authority on ResourceInUse. Consequently, it is
        // (pretty) safe to update our RTCDataChannel.id allocator without
        // checking for ResourceInUse.
        this._dataChannelIds.add(id);
        this.dispatchEvent(new RTCDataChannelEvent('datachannel', {channel}));
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
  createDataChannel(label: string, dataChannelDict?: ?RTCDataChannelInit) {
    let id;
    const dataChannelIds = this._dataChannelIds;
    if (dataChannelDict && 'id' in dataChannelDict) {
      id = dataChannelDict.id;
      if (typeof id !== 'number') {
        throw new TypeError('DataChannel id must be a number: ' + id);
      }
      if (dataChannelIds.has(id)) {
        throw new ResourceInUse('DataChannel id already in use: ' + id);
      }
    } else {
      // Allocate a new id.
      // TODO Remembering the last used/allocated id and then incrementing it to
      // generate the next id to use will surely be faster. However, I want to
      // reuse ids (in the future) as the RTCDataChannel.id space is limited to
      // unsigned short by the standard:
      // https://www.w3.org/TR/webrtc/#dom-datachannel-id. Additionally, 65535
      // is reserved due to SCTP INIT and INIT-ACK chunks only allowing a
      // maximum of 65535 streams to be negotiated (as defined by the WebRTC
      // Data Channel Establishment Protocol).
      for (id = 1; id < 65535 && dataChannelIds.has(id); ++id);
      // TODO Throw an error if no unused id is available.
      dataChannelDict = Object.assign({id}, dataChannelDict);
    }
    WebRTCModule.createDataChannel(
        this._peerConnectionId,
        label,
        dataChannelDict);
    dataChannelIds.add(id);
    return new RTCDataChannel(this._peerConnectionId, label, dataChannelDict);
  }
}
