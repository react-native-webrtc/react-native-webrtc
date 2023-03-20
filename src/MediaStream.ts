
import { defineCustomEventTarget } from 'event-target-shim';
import { NativeModules } from 'react-native';

import MediaStreamTrack from './MediaStreamTrack';
import { uniqueID } from './RTCUtil';

const { WebRTCModule } = NativeModules;

const MEDIA_STREAM_EVENTS = [ 'active', 'inactive', 'addtrack', 'removetrack' ];

export default class MediaStream extends defineCustomEventTarget(...MEDIA_STREAM_EVENTS) {
    id: string;
    active = true;

    _tracks: MediaStreamTrack[] = [];

    /**
     * The identifier of this MediaStream unique within the associated
     * WebRTCModule instance. As the id of a remote MediaStream instance is unique
     * only within the associated RTCPeerConnection, it is not sufficiently unique
     * to identify this MediaStream across multiple RTCPeerConnections and to
     * unambiguously differentiate it from a local MediaStream instance not added
     * to an RTCPeerConnection.
     */
    _reactTag: string;

    /**
     * A MediaStream can be constructed in several ways, depending on the parameters
     * that are passed here.
     *
     * - undefined: just a new stream, with no tracks.
     * - MediaStream instance: a new stream, with a copy of the tracks of the passed stream.
     * - Array of MediaStreamTrack: a new stream with a copy of the tracks in the array.
     * - object: a new stream instance, represented by the passed info object, this is always
     *   done internally, when the stream is first created in native and the JS wrapper is
     *   built afterwards.
     */
    constructor(arg) {
        super();

        // Assigm a UUID to start with. It may get overridden for remote streams.
        this.id = uniqueID();
        // Local MediaStreams are created by WebRTCModule to have their id and
        // reactTag equal because WebRTCModule follows the respective standard's
        // recommendation for id generation i.e. uses UUID which is unique enough
        // for the purposes of reactTag.
        this._reactTag = this.id;

        if (typeof arg === 'undefined') {
            WebRTCModule.mediaStreamCreate(this.id);
        } else if (arg instanceof MediaStream) {
            WebRTCModule.mediaStreamCreate(this.id);

            for (const track of arg.getTracks()) {
                this.addTrack(track);
            }
        } else if (Array.isArray(arg)) {
            WebRTCModule.mediaStreamCreate(this.id);

            for (const track of arg) {
                this.addTrack(track);
            }
        } else if (typeof arg === 'object' && arg.streamId && arg.streamReactTag && arg.tracks) {
            this.id = arg.streamId;
            this._reactTag = arg.streamReactTag;

            for (const trackInfo of arg.tracks) {
                // We are not using addTrack here because the track is already part of the
                // stream, so there is no need to add it on the native side.
                this._tracks.push(new MediaStreamTrack(trackInfo));
            }
        } else {
            throw new TypeError(`invalid type: ${typeof arg}`);
        }
    }

    addTrack(track: MediaStreamTrack): void {
        const index = this._tracks.indexOf(track);

        if (index !== -1) {
            return;
        }

        this._tracks.push(track);
        WebRTCModule.mediaStreamAddTrack(this._reactTag, track.remote ? track._peerConnectionId : -1, track.id);
    }

    removeTrack(track: MediaStreamTrack): void {
        const index = this._tracks.indexOf(track);

        if (index === -1) {
            return;
        }

        this._tracks.splice(index, 1);
        WebRTCModule.mediaStreamRemoveTrack(this._reactTag, track.remote ? track._peerConnectionId : -1, track.id);
    }

    getTracks(): MediaStreamTrack[] {
        return this._tracks.slice();
    }

    getTrackById(trackId): MediaStreamTrack | undefined {
        return this._tracks.find(track => track.id === trackId);
    }

    getAudioTracks(): MediaStreamTrack[] {
        return this._tracks.filter(track => track.kind === 'audio');
    }

    getVideoTracks(): MediaStreamTrack[] {
        return this._tracks.filter(track => track.kind === 'video');
    }

    clone(): never {
        throw new Error('Not implemented.');
    }

    toURL(): string {
        return this._reactTag;
    }

    release(releaseTracks = true): void {
        const tracks = [ ...this._tracks ];

        for (const track of tracks) {
            this.removeTrack(track);

            if (releaseTracks) {
                track.release();
            }
        }

        WebRTCModule.mediaStreamRelease(this._reactTag);
    }
}
