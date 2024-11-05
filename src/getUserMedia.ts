
import { NativeModules } from 'react-native';


import MediaStream from './MediaStream';
import MediaStreamError from './MediaStreamError';
import permissions from './Permissions';
import * as RTCUtil from './RTCUtil';

const { WebRTCModule } = NativeModules;

interface Constraints {
    audio?: boolean | object;
    video?: boolean | object;
}

export default function getUserMedia(constraints: Constraints = {}): Promise<MediaStream> {
    // According to
    // https://www.w3.org/TR/mediacapture-streams/#dom-mediadevices-getusermedia,
    // the constraints argument is a dictionary of type MediaStreamConstraints.
    if (typeof constraints !== 'object') {
        return Promise.reject(new TypeError('constraints is not a dictionary'));
    }

    if (
        (typeof constraints.audio === 'undefined' || !constraints.audio) &&
        (typeof constraints.video === 'undefined' || !constraints.video)
    ) {
        return Promise.reject(new TypeError('audio and/or video is required'));
    }

    // Normalize constraints.
    constraints = RTCUtil.normalizeConstraints(constraints);

    // Request required permissions
    const reqPermissions: Promise<boolean>[] = [];

    if (constraints.audio) {
        reqPermissions.push(permissions.request({ name: 'microphone' }));
    } else {
        reqPermissions.push(Promise.resolve(false));
    }

    if (constraints.video) {
        reqPermissions.push(permissions.request({ name: 'camera' }));
    } else {
        reqPermissions.push(Promise.resolve(false));
    }

    return new Promise((resolve, reject) => {
        Promise.all(reqPermissions).then(results => {
            const [ audioPerm, videoPerm ] = results;

            // Check permission results and remove unneeded permissions.

            if (!audioPerm && !videoPerm) {
                // https://www.w3.org/TR/mediacapture-streams/#dom-mediadevices-getusermedia
                // step 4
                const error = {
                    message: 'Permission denied.',
                    name: 'SecurityError'
                };

                reject(new MediaStreamError(error));

                return;
            }

            audioPerm || delete constraints.audio;
            videoPerm || delete constraints.video;

            const success = (id, tracks) => {
                // Store initial constraints.
                for (const trackInfo of tracks) {
                    const c = constraints[trackInfo.kind];

                    if (typeof c === 'object') {
                        trackInfo.constraints = RTCUtil.deepClone(c);
                    }
                }

                const info = {
                    streamId: id,
                    streamReactTag: id,
                    tracks
                };

                resolve(new MediaStream(info));
            };

            const failure = (type, message) => {
                let error;

                switch (type) {
                    case 'TypeError':
                        error = new TypeError(message);
                        break;
                }

                if (!error) {
                    error = new MediaStreamError({ message, name: type });
                }

                reject(error);
            };

            WebRTCModule.getUserMedia(constraints, success, failure);
        });
    });
}
