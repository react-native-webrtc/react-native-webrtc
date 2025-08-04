import { EventTarget, Event, defineEventAttribute } from 'event-target-shim/index';
import { NativeModules } from 'react-native';

import { addListener } from './EventEmitter';
import getDisplayMedia from './getDisplayMedia';
import getUserMedia, { Constraints } from './getUserMedia';

const { WebRTCModule } = NativeModules;

export type VideoTrackDimension = {
    width: number;
    height: number;
};

export const videoTrackDimensionChangedEventQueue = new Map<string, VideoTrackDimension>();

let listenersReady = false;

function ensureListeners() {
    if (listenersReady) {
        return;
    }

    addListener('MediaDevices', 'videoTrackDimensionChanged', (ev: any) => {
        // We only want to queue events for local tracks.
        if (ev.pcId !== -1) {
            return;
        }

        const { trackId, width, height } = ev;

        videoTrackDimensionChangedEventQueue.set(trackId, { width, height });
    });

    listenersReady = true;
}

type MediaDevicesEventMap = {
    devicechange: Event<'devicechange'>
}

class MediaDevices extends EventTarget<MediaDevicesEventMap> {
    /**
     * W3C "Media Capture and Streams" compatible {@code enumerateDevices}
     * implementation.
     */
    enumerateDevices() {
        return new Promise(resolve => WebRTCModule.enumerateDevices(resolve));
    }

    /**
     * W3C "Screen Capture" compatible {@code getDisplayMedia} implementation.
     * See: https://w3c.github.io/mediacapture-screen-share/
     *
     * @returns {Promise}
     */
    getDisplayMedia() {
        ensureListeners();

        return getDisplayMedia();
    }

    /**
     * W3C "Media Capture and Streams" compatible {@code getUserMedia}
     * implementation.
     * See: https://www.w3.org/TR/mediacapture-streams/#dom-mediadevices-enumeratedevices
     *
     * @param {*} constraints
     * @returns {Promise}
     */
    getUserMedia(constraints: Constraints) {
        ensureListeners();

        return getUserMedia(constraints);
    }
}

/**
 * Define the `onxxx` event handlers.
 */
const proto = MediaDevices.prototype;

defineEventAttribute(proto, 'devicechange');


export default new MediaDevices();
