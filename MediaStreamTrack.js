'use strict';

import {
  NativeModules,
} from 'react-native';
const WebRTCModule = NativeModules.WebRTCModule;

import EventTarget from 'event-target-shim';
import MediaStreamErrorEvent from './MediaStreamErrorEvent';

import type MediaStreamError from './MediaStreamError';

const MEDIA_STREAM_TRACK_EVENTS = [
  'ended',
  'mute',
  'unmute',
  'overconstrained', // --- see: https://www.w3.org/TR/mediacapture-streams/#constrainable-interface
];

type MediaStreamTrackState = "live" | "ended";

type SourceInfo = {
  id: string;
  label: string;
  facing: string;
  kind: string;
};

class MediaStreamTrack {
  static getSources(success: (sources: Array<SourceInfo>) => void) {
    WebRTCModule.mediaStreamTrackGetSources(success);
  }

  _enabled: boolean;
  id: number; // NOTE: spec wants string here
  kind: string;
  label: string;
  muted: boolean;
  readonly: boolean; // how to decide?
  readyState: MediaStreamTrackState; // readyState in java: INITIALIZING, LIVE, ENDED, FAILED
  remote: boolean;

  onended: ?Function;
  onmute: ?Function;
  onunmute: ?Function;
  overconstrained: ?Function;

  constructor(info) {
    let _readyState = info.readyState.toLowerCase();
    this._enabled = info.enabled;
    this.id = info.id;
    this.kind = info.kind;
    this.label = info.label;
    this.muted = false;
    this.readonly = true; // how to decide?
    this.remote = info.remote;
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
    if (this.remote) {
      return;
    }
    WebRTCModule.mediaStreamTrackStop(this.id);
    this._enabled = false;
    this.readyState = 'ended';
    this.muted = !this._enabled;
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
    throw new Error('Not implemented.');
  }

  getSettings() {
    throw new Error('Not implemented.');
  }
}

module.exports = MediaStreamTrack;
