
import { NativeModules } from 'react-native';
// @ts-ignore
import resolveAssetSource from 'react-native/Libraries/Image/resolveAssetSource';

import MediaStream from './MediaStream';
import MediaStreamError from './MediaStreamError';


const { WebRTCModule } = NativeModules;

export default function getFileMedia(source: number | string): Promise<MediaStream> {
    let asset = source;

    if (typeof source === 'number') {
        asset = resolveAssetSource(source)?.uri;
    }

    return new Promise((resolve, reject) => {
        WebRTCModule.getFileMedia(asset).then(
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
