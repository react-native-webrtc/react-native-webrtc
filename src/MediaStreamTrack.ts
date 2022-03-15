
import { NativeModules } from 'react-native';
import { defineCustomEventTarget } from 'event-target-shim';

import { deepClone } from './RTCUtil';

const { WebRTCModule } = NativeModules;

const MEDIA_STREAM_TRACK_EVENTS = ['ended', 'mute', 'unmute'];

type MediaStreamTrackState = 'live' | 'ended';

class MediaStreamTrack extends defineCustomEventTarget(...MEDIA_STREAM_TRACK_EVENTS) {
    _constraints: object;
    _enabled: boolean;
    _settings: object;
    _muted: boolean;

    id: string;
    kind: string;
    label: string;
    readyState: MediaStreamTrackState;
    remote: boolean;

    constructor(info) {
        super();

        this._constraints = info.constraints || {};
        this._enabled = info.enabled;
        this._settings = info.settings || {};
        this._muted = false;

        this.id = info.id;
        this.kind = info.kind;
        this.label = info.label;
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
    }
}

export default MediaStreamTrack;
