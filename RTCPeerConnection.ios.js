'use strict';
var RCTDeviceEventEmitter = require('RCTDeviceEventEmitter');
var WebRTCManager = require('react-native').NativeModules.WebRTCManager;

var RTCSessionDescription = require('./RTCSessionDescription');
var RTCIceCandidate = require('./RTCIceCandidate');
var RTCMediaStream = require('./RTCMediaStream');
var RTCIceCandidateEvent = require('./RTCIceCandidateEvent');
var MediaStreamEvent = require('./MediaStreamEvent');
var RTCEvent = require('./RTCEvent');

var RTCPeerConnectionBase = require('./RTCPeerConnectionBase');

var RTCPeerConnectionId = 0;

class RTCPeerConnection extends RTCPeerConnectionBase {
  objectId: number;
  _subs: any;

  constructorImpl(configuration) {
    this.objectId = RTCPeerConnectionId++;
    WebRTCManager.RTCPeerConnectionInitWithConfiguration(configuration, this.objectId);
    this._registerEvents(this.objectId);
  }
  addStreamImpl(stream) {
    WebRTCManager.RTCPeerConnectionAddStream(stream.objectId, this.objectId);
  }
  createOfferImpl(success: ?Function, failure: ?Function, constraints) {
    WebRTCManager.RTCPeerConnectionCreateOfferWithObjectID(this.objectId, (errorJSON, sdpJSON) => {
      var error = errorJSON; // TODO: convert to NavigatorUserMediaError
      if (error) {
        failure(error);
      } else {
        var sessionDescription = new RTCSessionDescription(sdpJSON);
        success(sessionDescription);
      }
    });
  }
  createAnswerImpl(success: ?Function, failure: ?Function, constraints) {
    WebRTCManager.RTCPeerConnectionCreateAnswerWithObjectID(this.objectId, (errorJSON, sdpJSON) => {
      var error = errorJSON;
      if (error) {
        failure(error);
      } else {
        var sessionDescription = new RTCSessionDescription(sdpJSON);
        success(sessionDescription);
      }
    });
  }
  setLocalDescriptionImpl(sessionDescription: RTCSessionDescription, success: ?Function, failure: ?Function, constraints) {
    WebRTCManager.RTCPeerConnectionSetLocalDescriptionWithSessionDescription(sessionDescription.toJSON(), this.objectId, (errorResponse) => {
      var error = errorResponse;
      if (error) {
        failure(error);
      } else {
        this.localDescription = sessionDescription;
        success();
      }
    });
  }
  setRemoteDescriptionImpl(sessionDescription: RTCSessionDescription, success: ?Function, failure: ?Function) {
    WebRTCManager.RTCPeerConnectionSetRemoteDescriptionWithSessionDescription(sessionDescription.toJSON(), this.objectId, (errorResponse) => {
      var error = errorResponse;
      if (error) {
        failure(error);
      } else {
        this.remoteDescription = sessionDescription;
        success();
      }
    });
  }
  addIceCandidateImpl(candidate, success, failure) { // TODO: success, failure
    WebRTCManager.RTCPeerConnectionAddICECandidateWithICECandidate(candidate.toJSON(), this.objectId, (successful) => {
      if (successful) {
        success && success();
      } else {
        failure && failure();
      }
    });
  }
  addIceCandidateDirectImpl(candidate, success, failure) {
    WebRTCManager.RTCPeerConnectionAddICECandidateDirectWithICECandidate(candidate, this.objectId, (successful) => {
      if (successful) {
        success && success();
      } else {
        failure && failure();
      }
    });
  }
  closeImpl() {
    WebRTCManager.RTCPeerConnectionCloseWithObjectID(this.objectId);
  }
  _registerEvents(objectId): void {
    this._subs = [
      RCTDeviceEventEmitter.addListener('peerConnectionOnRenegotiationNeeded', function(event) {
        if (event.id !== objectId) {
          return;
        }
        this.onnegotiationneeded && this.onnegotiationneeded();
      }.bind(this)),
      RCTDeviceEventEmitter.addListener('peerConnectionIceConnectionChanged', function(event) {
        if (event.id !== objectId) {
          return;
        }
        this.iceConnectionState = event.iceConnectionState;
        var ev = new RTCEvent("iceconnectionstatechange", {"target": this});
        this.oniceconnectionstatechange && this.oniceconnectionstatechange(ev);
      }.bind(this)),
      RCTDeviceEventEmitter.addListener('peerConnectionSignalingStateChanged', function(event) {
        if (event.id !== objectId) {
          return;
        }
        this.signalingState = event.signalingState;
        var ev = new RTCEvent("signalingstatechange", {"target": this});
        this.onsignalingstatechange && this.onsignalingstatechange(ev);
      }.bind(this)),
      RCTDeviceEventEmitter.addListener('peerConnectionAddedStream', function(event) {
        if (event.id !== objectId) {
          return;
        }
        var stream = new RTCMediaStream(event.streamId);
        var ev = new MediaStreamEvent("addstream", {"target": this, "stream": stream});
        this.onaddstream && this.onaddstream(ev);
      }.bind(this)),
      RCTDeviceEventEmitter.addListener('peerConnectionGotICECandidate', function(event) {
        if (event.id !== objectId) {
          return;
        }
        var candidateJSON = event.candidateJSON;
        var candidate = new RTCIceCandidate(candidateJSON);
        var ev = new RTCIceCandidateEvent("icecandidate", {"target": this, "candidate": candidate});
        this.onicecandidate && this.onicecandidate(ev);
      }.bind(this))
    ];
  }
}

module.exports = RTCPeerConnection;
