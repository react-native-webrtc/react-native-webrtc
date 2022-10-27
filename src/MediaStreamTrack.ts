
import { defineCustomEventTarget, Event } from 'event-target-shim';
import { NativeModules } from 'react-native';

import { deepClone } from './RTCUtil';

const { WebRTCModule } = NativeModules;

const MEDIA_STREAM_TRACK_EVENTS = [ 'ended', 'mute', 'unmute' ];

type MediaStreamTrackState = 'live' | 'ended';

class MediaStreamTrack extends defineCustomEventTarget(...MEDIA_STREAM_TRACK_EVENTS) {
    _constraints: object;
    _enabled: boolean;
    _settings: object;
    _muted: boolean;
    _peerConnectionId: number;

    readonly id: string;
    readonly kind: string;
    readonly label: string = '';
    readyState: MediaStreamTrackState;
    readonly remote: boolean;

    constructor(info) {
        super();

        this._constraints = info.constraints || {};
        this._enabled = info.enabled;
        this._settings = info.settings || {};
        this._muted = false;
        this._peerConnectionId = info.peerConnectionId;

        this.id = info.id;
        this.kind = info.kind;
        this.remote = info.remote;

        const _readyState = info.readyState.toLowerCase();

        this.readyState = _readyState === 'initializing' || _readyState === 'live' ? 'live' : 'ended';
    }

    get enabled(): boolean {
        return this._enabled;
    }

    set enabled(enabled: boolean) {
        if (enabled === this._enabled) {
            return;
        }

        WebRTCModule.mediaStreamTrackSetEnabled(this.id, !this._enabled);
        this._enabled = !this._enabled;
    }

    get muted(): boolean {
        return this._muted;
    }

    stop(): void {
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
    _switchCamera(): void {
        if (this.remote) {
            throw new Error('Not implemented for remote tracks');
        }

        if (this.kind !== 'video') {
            throw new Error('Only implemented for video tracks');
        }

        WebRTCModule.mediaStreamTrackSwitchCamera(this.id);
    }

    _setVideoEffect(name:string) {
        if (this.remote) {
            throw new Error('Not implemented for remote tracks');
        }

        if (this.kind !== 'video') {
            throw new Error('Only implemented for video tracks');
        }

        WebRTCModule.mediaStreamTrackSetVideoEffect(this.id, name);
    }

    /**
     * Internal function which is used to set the muted state on remote tracks and
     * emit the mute / unmute event.
     *
     * @param muted Whether the track should be marked as muted / unmuted.
     */
    _setMutedInternal(muted: boolean) {
        if (!this.remote) {
            throw new Error('Track is not remote!');
        }

        this._muted = muted;
        this.dispatchEvent(new Event(muted ? 'mute' : 'unmute'));
    }

    applyConstraints(): never {
        throw new Error('Not implemented.');
    }

    clone(): never {
        throw new Error('Not implemented.');
    }

    getCapabilities(): never {
        throw new Error('Not implemented.');
    }

    getConstraints() {
        return deepClone(this._constraints);
    }

    getSettings() {
        return deepClone(this._settings);
    }

    release(): void {
        WebRTCModule.mediaStreamTrackRelease(this.id);
    }
}

export default MediaStreamTrack;
