'use strict';

import {DeviceEventEmitter, NativeModules} from 'react-native';
import EventTarget from 'event-target-shim';
import MediaStreamErrorEvent from './MediaStreamErrorEvent';
import type MediaStreamError from './MediaStreamError';
import { deepClone } from './RTCUtil';

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
  _remote: boolean;
  _settings: Object;
  id: string;
  kind: string;
  label: string;
  muted: boolean;
  // readyState in java: INITIALIZING, LIVE, ENDED, FAILED
  readyState: MediaStreamTrackState;

  onended: ?Function;
  onmute: ?Function;
  onunmute: ?Function;
  overconstrained: ?Function;

  constructor(info) {
    super();

    this._constraints = info.constraints || {};
    this._enabled = info.enabled;
    this._remote = info.remote;
    this._settings = info.settings || {};

    this.id = info.id;
    this.kind = info.kind;
    this.label = info.label;
    this.muted = false;

    const _readyState = info.readyState.toLowerCase();
    this.readyState = (_readyState === "initializing"
                    || _readyState === "live") ? "live" : "ended";

    if (!info.remote) {
      this._registerEvents();
    }
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
    this._unregisterEvents();
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
    if (this._remote) {
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
    throw new Error('Not implemented.');
  }

  getCapabilities() {
    throw new Error('Not implemented.');
  }

  getConstraints() {
    return deepClone(this._constraints);
  }

  getSettings() {
    return deepClone(this._settings);
  }

  release() {
    WebRTCModule.mediaStreamTrackRelease(this.id);
    this._unregisterEvents();
  }

  // Track specific events
  //

  _unregisterEvents(): void {
    this._subscriptions.forEach(e => e.remove());
    this._subscriptions = [];
  }

  _registerEvents(): void {
    this._subscriptions = [
      DeviceEventEmitter.addListener('mediaStreamTrackUpdateSettings', ev => {
        if (ev.trackId !== this.id) {
          return;
        }
        this._settings = Object.assign({}, track._settings, ev.settings);
      })
    ];
  }
}

export default MediaStreamTrack;
