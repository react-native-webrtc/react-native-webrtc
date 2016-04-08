'use strict';

var EventTarget = require('event-target-shim');
var React = require('react-native');
var {
  DeviceEventEmitter,
  NativeModules,
} = React;
var WebRTCModule = NativeModules.WebRTCModule;

var MediaStream = require('./MediaStream');
var MediaStreamEvent = require('./MediaStreamEvent');
var MediaStreamTrack = require('./MediaStreamTrack');
var RTCSessionDescription = require('./RTCSessionDescription');
var RTCIceCandidate = require('./RTCIceCandidate');
var RTCIceCandidateEvent = require('./RTCIceCandidateEvent');
var RTCEvent = require('./RTCEvent');

const PEER_CONNECTION_EVENTS = [
  'connectionstatechange',
  'icecandidate',
  'icecandidateerror',
  'iceconnectionstatechange',
  'icegatheringstatechange',
  'negotiationneeded',
  'signalingstatechange',
  // old:
  'addstream',
  'removestream',
];

let nextPeerConnectionId = 0;

class RTCPeerConnection extends EventTarget(PEER_CONNECTION_EVENTS) {
  localDescription: RTCSessionDescription;
  remoteDescription: RTCSessionDescription;

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

  constructor(configuration) {
    super();
    this._peerConnectionId = nextPeerConnectionId++;
    WebRTCModule.peerConnectionInit(configuration, this._peerConnectionId);
    this._registerEvents(this._peerConnectionId);
  }

  addStream(stream) {
    WebRTCModule.peerConnectionAddStream(stream._streamId, this._peerConnectionId);
    this._localStreams.push(stream);
  }

  removeStream(stream) {
    var index = this._localStreams.indexOf(stream);
    if (index > -1) {
      this._localStreams.splice(index, 1);
    }
    WebRTCModule.peerConnectionRemoveStream(stream._streamId, this._peerConnectionId);
  }

  createOffer(success: ?Function, failure: ?Function, constraints) {
    WebRTCModule.peerConnectionCreateOffer(this._peerConnectionId, (successful, data) => {
      if (successful) {
        var sessionDescription = new RTCSessionDescription(data);
        success(sessionDescription);
      } else {
        failure(data); // TODO: convert to NavigatorUserMediaError
      }
    });
  }

  createAnswer(success: ?Function, failure: ?Function, constraints) {
    WebRTCModule.peerConnectionCreateAnswer(this._peerConnectionId, (successful, data) => {
      if (successful) {
        var sessionDescription = new RTCSessionDescription(data);
        success(sessionDescription);
      } else {
        failure(data);
      }
    });
  }

  setLocalDescription(sessionDescription: RTCSessionDescription, success: ?Function, failure: ?Function, constraints) {
    WebRTCModule.peerConnectionSetLocalDescription(sessionDescription.toJSON(), this._peerConnectionId, (successful, data) => {
      if (successful) {
        this.localDescription = sessionDescription;
        success();
      } else {
        failure(data);
      }
    });
  }

  setRemoteDescription(sessionDescription: RTCSessionDescription, success: ?Function, failure: ?Function) {
    WebRTCModule.peerConnectionSetRemoteDescription(sessionDescription.toJSON(), this._peerConnectionId, (successful, data) => {
      if (successful) {
        this.remoteDescription = sessionDescription;
        success();
      } else {
        failure(data);
      }
    });
  }

  addIceCandidate(candidate, success, failure) { // TODO: success, failure
    WebRTCModule.peerConnectionAddICECandidate(candidate.toJSON(), this._peerConnectionId, (successful) => {
      if (successful) {
        success && success();
      } else {
        failure && failure();
      }
    });
  }

  getStats(track, success, failure) {
    if (WebRTCModule.peerConnectionGetStats) {
      WebRTCModule.peerConnectionGetStats(track ? track.id : -1, this._peerConnectionId, stats => {
        success && success(stats);
      });
    } else {
      console.warn('RTCPeerConnection getStats doesn\'t support');
    }
  }

  close() {
    WebRTCModule.peerConnectionClose(this._peerConnectionId);
  }

  _registerEvents(id: number): void {
    this._subscriptions = [
      DeviceEventEmitter.addListener('peerConnectionOnRenegotiationNeeded', ev => {
        if (ev.id !== id) {
          return;
        }
        this.dispatchEvent(new RTCEvent('negotiationneeded'));
      }),
      DeviceEventEmitter.addListener('peerConnectionIceConnectionChanged', ev => {
        if (ev.id !== id) {
          return;
        }
        this.iceConnectionState = ev.iceConnectionState;
        this.dispatchEvent(new RTCEvent('iceconnectionstatechange'));
      }),
      DeviceEventEmitter.addListener('peerConnectionSignalingStateChanged', ev => {
        if (ev.id !== id) {
          return;
        }
        this.signalingState = ev.signalingState;
        this.dispatchEvent(new RTCEvent('signalingstatechange'));
      }),
      DeviceEventEmitter.addListener('peerConnectionAddedStream', ev => {
        if (ev.id !== id) {
          return;
        }
        var stream = new MediaStream(ev.streamId);
        var tracks = ev.tracks;
        for (var i = 0; i < tracks.length; i++) {
          stream.addTrack(new MediaStreamTrack(tracks[i]));
        }
        this._remoteStreams.push(stream);
        this.dispatchEvent(new MediaStreamEvent('addstream', {stream}));
      }),
      DeviceEventEmitter.addListener('peerConnectionRemovedStream', ev => {
        if (ev.id !== id) {
          return;
        }
        var stream = this._remoteStreams.find(s => s._streamId === ev.streamId);
        if (stream) {
          var index = this._remoteStreams.indexOf(stream);
          if (index > -1) {
            this._remoteStreams.splice(index, 1);
          }
        }
        this.dispatchEvent(new MediaStreamEvent('removestream', {stream}));
      }),
      DeviceEventEmitter.addListener('peerConnectionGotICECandidate', ev => {
        if (ev.id !== id) {
          return;
        }
        var candidate = new RTCIceCandidate(ev.candidate);
        var event = new RTCIceCandidateEvent('icecandidate', {candidate});
        this.dispatchEvent(event);
      }),
      DeviceEventEmitter.addListener('peerConnectionIceGatheringChanged', ev => {
        if (ev.id !== id) {
          return;
        }
        this.iceGatheringState = ev.iceGatheringState;
        this.dispatchEvent(new RTCEvent('icegatheringstatechange'));
      })
    ];
  }
}

module.exports = RTCPeerConnection;
