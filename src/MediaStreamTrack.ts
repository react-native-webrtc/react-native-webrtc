import { EventTarget, Event, defineEventAttribute } from 'event-target-shim/index';
import { NativeModules } from 'react-native';

import { MediaTrackConstraints } from './Constraints';
import { addListener, removeListener } from './EventEmitter';
import Logger from './Logger';
import { videoTrackDimensionChangedEventQueue } from './MediaDevices';
import { deepClone, normalizeConstraints } from './RTCUtil';

const log = new Logger('pc');
const { WebRTCModule } = NativeModules;


type MediaStreamTrackState = 'live' | 'ended';

export type MediaStreamTrackInfo = {
    id: string;
    kind: string;
    remote: boolean;
    constraints: object;
    enabled: boolean;
    settings: object;
    peerConnectionId: number;
    readyState: MediaStreamTrackState;
}

export type MediaTrackSettings = {
    width?: number;
    height?: number;
    frameRate?: number;
    facingMode?: string;
    deviceId?: string;
    groupId?: string;
}

type MediaStreamTrackEventMap = {
    ended: Event<'ended'>;
    mute: Event<'mute'>;
    unmute: Event<'unmute'>;
}

export default class MediaStreamTrack extends EventTarget<MediaStreamTrackEventMap> {
    _constraints: MediaTrackConstraints;
    _enabled: boolean;
    _settings: MediaTrackSettings;
    _muted: boolean;
    _peerConnectionId: number;
    _readyState: MediaStreamTrackState;

    readonly id: string;
    readonly kind: string;
    readonly label: string = '';
    readonly remote: boolean;

    constructor(info: MediaStreamTrackInfo) {
        super();

        this.id = info.id;
        this.kind = info.kind;
        this.remote = info.remote;
        this._constraints = info.constraints || {};
        this._enabled = info.enabled;
        this._settings = info.settings || {};
        this._muted = false;
        this._peerConnectionId = info.peerConnectionId;
        this._readyState = info.readyState;

        if (!this.remote) {
            this._registerEvents();

            if (this.kind === 'video') {
                this._processVideoTrackDimensionChangedQueue();
            }
        }
    }

    get enabled(): boolean {
        return this._enabled;
    }

    set enabled(enabled: boolean) {
        if (enabled === this._enabled) {
            return;
        }

        this._enabled = Boolean(enabled);

        if (this._readyState === 'ended') {
            return;
        }

        WebRTCModule.mediaStreamTrackSetEnabled(this.remote ? this._peerConnectionId : -1, this.id, this._enabled);
    }

    get muted(): boolean {
        return this._muted;
    }

    get readyState(): string {
        return this._readyState;
    }

    stop(): void {
        this.enabled = false;
        this._readyState = 'ended';
    }

    /**
     * Private / custom API for switching the cameras on the fly, without the
     * need for adding / removing tracks or doing any SDP renegotiation.
     *
     * This is how the reference application (AppRTCMobile) implements camera
     * switching.
     *
     * @deprecated Use applyConstraints instead.
     */
    _switchCamera(): void {
        if (this.remote) {
            throw new Error('Not implemented for remote tracks');
        }

        if (this.kind !== 'video') {
            throw new Error('Only implemented for video tracks');
        }

        const constraints = deepClone(this._settings);

        delete constraints.deviceId;
        constraints.facingMode = this._settings.facingMode === 'user' ? 'environment' : 'user';

        this.applyConstraints(constraints);
    }

    _setVideoEffects(names: string[]) {
        if (this.remote) {
            throw new Error('Not implemented for remote tracks');
        }

        if (this.kind !== 'video') {
            throw new Error('Only implemented for video tracks');
        }

        WebRTCModule.mediaStreamTrackSetVideoEffects(this.id, names);
    }

    _setVideoEffect(name: string | null | undefined) {
        if (name === null || name === undefined) {
            this._setVideoEffects([]);

            return;
        }

        this._setVideoEffects([ name ]);
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

    /**
     * Internal function which is used to set the video dimensions on video tracks.
     *
     * @param width The new width of the video track.
     * @param height The new height of the video track.
     */
    _setVideoTrackDimensions(width: number, height: number) {
        if (this.kind !== 'video') {
            throw new Error('Only implemented for video tracks');
        }

        this._settings = {
            ...this._settings,
            width,
            height
        };
    }

    /**
     * Custom API for setting the volume on an individual audio track.
     *
     * @param volume a gain value in the range of 0-10. defaults to 1.0
     */
    _setVolume(volume: number) {
        if (this.kind !== 'audio') {
            throw new Error('Only implemented for audio tracks');
        }

        WebRTCModule.mediaStreamTrackSetVolume(this.remote ? this._peerConnectionId : -1, this.id, volume);
    }

    /**
     * Applies a new set of constraints to the track.
     *
     * @param constraints An object listing the constraints
     * to apply to the track's constrainable properties; any existing
     * constraints are replaced with the new values specified, and any
     * constrainable properties not included are restored to their default
     * constraints. If this parameter is omitted, all currently set custom
     * constraints are cleared.
     */
    async applyConstraints(constraints?: MediaTrackConstraints): Promise<void> {
        if (this.kind !== 'video') {
            throw new Error('Only implemented for video tracks');
        }

        const normalized = normalizeConstraints({ video: constraints ?? true });

        this._settings = await WebRTCModule.mediaStreamTrackApplyConstraints(this.id, normalized.video);
        this._constraints = constraints ?? {};
    }

    clone(): MediaStreamTrack {
        if (this.remote) {
            throw new Error('clone is not implemented for remote tracks');
        }

        const id = WebRTCModule.mediaStreamTrackClone(this.id);

        return new MediaStreamTrack({
            id,
            kind: this.kind,
            remote: this.remote,
            constraints: deepClone(this._constraints),
            enabled: this._enabled,
            settings: deepClone(this._settings),
            peerConnectionId: this._peerConnectionId,
            readyState: this._readyState,
        });
    }

    getCapabilities(): never {
        throw new Error('Not implemented.');
    }

    getConstraints() {
        return deepClone(this._constraints);
    }

    getSettings(): MediaTrackSettings {
        return deepClone(this._settings);
    }

    _registerEvents(): void {
        addListener(this, 'mediaStreamTrackEnded', (ev: any) => {
            if (ev.trackId !== this.id || this._readyState === 'ended') {
                return;
            }

            log.debug(`${this.id} mediaStreamTrackEnded`);
            this._readyState = 'ended';

            this.dispatchEvent(new Event('ended'));
        });

        // Add dimension change listener for local video tracks
        if (this.kind === 'video') {
            addListener(this, 'videoTrackDimensionChanged', (ev: any) => {
                // Only handle local tracks (pcId === -1) and only for this track
                if (ev.pcId !== -1 || ev.trackId !== this.id) {
                    return;
                }

                this._setVideoTrackDimensions(ev.width, ev.height);
            });
        }
    }

    /**
     * Processes any queued `videoTrackDimensionChanged` events for this track.
     */
    _processVideoTrackDimensionChangedQueue(): void {
        const eventData = videoTrackDimensionChangedEventQueue.get(this.id);

        if (!eventData) {
            return;
        }

        this._setVideoTrackDimensions(eventData.width, eventData.height);

        videoTrackDimensionChangedEventQueue.delete(this.id);
    }

    release(): void {
        if (this.remote) {
            return;
        }

        removeListener(this);
        WebRTCModule.mediaStreamTrackRelease(this.id);

        if (this.kind === 'video') {
            videoTrackDimensionChangedEventQueue.delete(this.id);
        }
    }
}

/**
 * Define the `onxxx` event handlers.
 */
const proto = MediaStreamTrack.prototype;

defineEventAttribute(proto, 'ended');
defineEventAttribute(proto, 'mute');
defineEventAttribute(proto, 'unmute');
