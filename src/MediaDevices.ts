import { EventTarget, Event, defineEventAttribute } from 'event-target-shim/index';
import { NativeModules } from 'react-native';

import getDisplayMedia from './getDisplayMedia';
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
     * @param {*} constraints
     * @default { video: { deviceId: 'screen-capture-manual' } }
     *
     * @returns {Promise}
     *
     * @example
     * ```ts
     * // screen-capture-auto uses Broadcast Extension + RPSystemBroadcastPickerView
     * // screen-capture-manual uses Broadcast Extension
     * // app-capture uses RPScreenRecorder
     * mediaDevices.getDisplayMedia({ video: { deviceId: 'app-capture' }})
     * ```
     */
    getDisplayMedia(constraints?: Constraints) {
        return getDisplayMedia(constraints);
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
}

/**
 * Define the `onxxx` event handlers.
 */
const proto = MediaDevices.prototype;

defineEventAttribute(proto, 'devicechange');


export default new MediaDevices();
