
import { NativeModules } from 'react-native';

import MediaStream from './MediaStream';
import MediaStreamError from './MediaStreamError';

const { WebRTCModule } = NativeModules;

export default function getDisplayMedia(): Promise<MediaStream> {
    return new Promise((resolve, reject) => {
        WebRTCModule.getDisplayMedia().then(
            data => {
                const { streamId, track } = data;

                const info = {
                    streamId: streamId,
                    streamReactTag: streamId,
                    tracks: [ track ]
                };

                const stream = new MediaStream(info);

                resolve(stream);
            },
            error => {
                reject(new MediaStreamError(error));
            }
        );
    });
}
