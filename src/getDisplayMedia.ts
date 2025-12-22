
import { NativeModules } from 'react-native';

import MediaStream from './MediaStream';
import MediaStreamError from './MediaStreamError';

const { WebRTCModule } = NativeModules;

export interface Constraints {
    createConfigForDefaultDisplay?: boolean;
    resolutionScale?: number;
}

export default function getDisplayMedia(constraints: Constraints = {}): Promise<MediaStream> {
    return new Promise((resolve, reject) => {
        WebRTCModule.getDisplayMedia(constraints).then(
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
