'use strict';

import {NativeModules} from 'react-native';
import EventTarget from 'event-target-shim';
import MediaStreamTrackEvent from './MediaStreamTrackEvent';

import type MediaStreamTrack from './MediaStreamTrack';

const {WebRTCModule} = NativeModules;

const MEDIA_STREAM_EVENTS = [
  'active',
  'inactive',
  'addtrack',
  'removetrack',
];

export default class MediaStream extends EventTarget(MEDIA_STREAM_EVENTS) {
  id: number; // NOTE: spec wants string here
  active: boolean = true;

  onactive: ?Function;
  oninactive: ?Function;
  onaddtrack: ?Function;
  onremovetrack: ?Function;

  _tracks: Array<MediaStreamTrack> = [];

  constructor(streamId) {
    super();
    this.id = streamId;
  }

  addTrack(track: MediaStreamTrack) {
    this._tracks.push(track);
    this.dispatchEvent(new MediaStreamTrackEvent('addtrack', {track}));
  }

  removeTrack(track: MediaStreamTrack) {
    let index = this._tracks.indexOf(track);
    if (index === -1) {
      return;
    }
    WebRTCModule.mediaStreamTrackRelease(this.id, track.id);
    this._tracks.splice(index, 1);
    this.dispatchEvent(new MediaStreamTrackEvent('removetrack', {track}));
  }

  getTracks(): Array<MediaStreamTrack> {
    return this._tracks.slice();
  }

  getTrackById(trackId): ?MediaStreamTrack {
    return this._tracks.find(track => track.id === trackId);
  }

  getAudioTracks(): Array<MediaStreamTrack> {
    return this._tracks.filter(track => track.kind === 'audio');
  }

  getVideoTracks(): Array<MediaStreamTrack> {
    return this._tracks.filter(track => track.kind === 'video');
  }

  clone() {
    throw new Error('Not implemented.');
  }

  toURL() {
    return this.id;
  }

  release() {
    WebRTCModule.mediaStreamRelease(this.id);
  }
}
