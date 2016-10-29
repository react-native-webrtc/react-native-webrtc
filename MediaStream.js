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
  id: string;
  active: boolean = true;

  onactive: ?Function;
  oninactive: ?Function;
  onaddtrack: ?Function;
  onremovetrack: ?Function;

  _tracks: Array<MediaStreamTrack> = [];

  /**
   * The identifier of this MediaStream unique within the associated
   * WebRTCModule instance. As the id of a remote MediaStream instance is unique
   * only within the associated RTCPeerConnection, it is not sufficiently unique
   * to identify this MediaStream across multiple RTCPeerConnections and to
   * unambiguously differentiate it from a local MediaStream instance not added
   * to an RTCPeerConnection.
   */
  reactTag: string;

  constructor(id, reactTag) {
    super();
    this.id = id;
    // Local MediaStreams are created by WebRTCModule to have their id and
    // reactTag equal because WebRTCModule follows the respective standard's
    // recommendation for id generation i.e. uses UUID which is unique enough
    // for the purposes of reactTag.
    this.reactTag = (typeof reactTag === 'undefined') ? id : reactTag;
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
    WebRTCModule.mediaStreamTrackRelease(this.reactTag, track.id);
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
    return this.reactTag;
  }

  release() {
    WebRTCModule.mediaStreamRelease(this.reactTag);
  }
}
