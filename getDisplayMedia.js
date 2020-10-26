'use strict';

import { Platform, NativeModules } from 'react-native';

import MediaStream from './MediaStream';
import MediaStreamError from './MediaStreamError';

const { WebRTCModule } = NativeModules;

export default function getDisplayMedia(constraints) {
    if (Platform.OS !== 'android') {
        return Promise.reject(new Error('Unsupported platform'));
    }

    if (!constraints || !constraints.video) {
        return Promise.reject(new TypeError());
    }

    return new Promise((resolve, reject) => {
        WebRTCModule.getDisplayMedia()
            .then(data => {
                const { streamId, track } = data;

                const info = {
                    streamId: streamId,
                    streamReactTag: streamId,
                    tracks: [track]
                };

                const stream = new MediaStream(info);

                resolve(stream);
            }, error => {
                reject(new MediaStreamError(error));
            });
    });
}
