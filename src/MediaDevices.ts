import { EventTarget, Event, defineEventAttribute } from 'event-target-shim/index';
import { NativeModules } from 'react-native';

import getDisplayMedia from './getDisplayMedia';
import getFileMedia from './getFileMedia';
import getUserMedia, { Constraints } from './getUserMedia';

const { WebRTCModule } = NativeModules;

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
        return getUserMedia(constraints);
    }

    /**
     * File based media stream
     *
     * NOTE: Only supports static images at the moment
     *
     * @param source A file path, url, or import/require result
     * @returns {Promise}
     */
    getFileMedia(source: number | string) {
        return getFileMedia(source);
    }
}

/**
 * Define the `onxxx` event handlers.
 */
const proto = MediaDevices.prototype;

defineEventAttribute(proto, 'devicechange');


export default new MediaDevices();
