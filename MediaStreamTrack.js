'use strict';

import {NativeModules} from 'react-native';
import EventTarget from 'event-target-shim';
import MediaStreamErrorEvent from './MediaStreamErrorEvent';
import type MediaStreamError from './MediaStreamError';
import { deepClone } from './RTCUtil';
import uuid from 'uuid';

const {WebRTCModule} = NativeModules;

const MEDIA_STREAM_TRACK_EVENTS = [
  'ended',
  'mute',
  'unmute',
  // see: https://www.w3.org/TR/mediacapture-streams/#constrainable-interface
  'overconstrained',
];

type MediaStreamTrackState = "live" | "ended";

class MediaStreamTrack extends EventTarget(MEDIA_STREAM_TRACK_EVENTS) {
  _constraints: Object;
  _enabled: boolean;
  id: string;
  kind: string;
  label: string;
  muted: boolean;
  // readyState in java: INITIALIZING, LIVE, ENDED, FAILED
  readyState: MediaStreamTrackState;
  remote: boolean;

  onended: ?Function;
  onmute: ?Function;
  onunmute: ?Function;
  overconstrained: ?Function;

  constructor(info) {
    super();

    this._constraints = info.constraints || {};
    this._enabled = info.enabled;
    this.id = info.id;
    this.kind = info.kind;
    this.label = info.label;
    this.muted = false;
    this.remote = info.remote;

    const _readyState = info.readyState.toLowerCase();
    this.readyState = (_readyState === "initializing"
                    || _readyState === "live") ? "live" : "ended";
  }

  get enabled(): boolean {
    return this._enabled;
  }

  set enabled(enabled: boolean): void {
    if (enabled === this._enabled) {
      return;
    }
    WebRTCModule.mediaStreamTrackSetEnabled(this.id, !this._enabled);
    this._enabled = !this._enabled;
    this.muted = !this._enabled;
  }

  stop() {
    WebRTCModule.mediaStreamTrackSetEnabled(this.id, false);
    this.readyState = 'ended';
    // TODO: save some stopped flag?
  }

  /**
   * Private / custom API for switching the cameras on the fly, without the
   * need for adding / removing tracks or doing any SDP renegotiation.
   *
   * This is how the reference application (AppRTCMobile) implements camera
   * switching.
   */
  _switchCamera() {
    if (this.remote) {
      throw new Error('Not implemented for remote tracks');
    }
    if (this.kind !== 'video') {
      throw new Error('Only implemented for video tracks');
    }
    WebRTCModule.mediaStreamTrackSwitchCamera(this.id);
  }

  applyConstraints() {
    throw new Error('Not implemented.');
  }

  clone() {
    const tClone = {};
    //copy properties
    Object.assign(tClone, track);
    //according to the spec, need a new id
    tClone.id = uuid.v4();
    tClone.stop = track.stop.bind(tClone);
    tClone.applyConstraints = track.applyConstraints.bind(tClone);
    tClone.getCapabilities = track.getCapabilities.bind(tClone);
    tClone.getConstraints = track.getConstraints.bind(tClone);
    tClone.getSettings = track.getSettings.bind(tClone);

    return tClone;
  }

  getCapabilities() {
    throw new Error('Not implemented.');
  }

  getConstraints() {
    return deepClone(this._constraints);
  }

  getSettings() {
    throw new Error('Not implemented.');
  }

  release() {
    WebRTCModule.mediaStreamTrackRelease(this.id);
  }
}

export default MediaStreamTrack;
