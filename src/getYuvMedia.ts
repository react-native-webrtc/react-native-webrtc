
import { NativeModules } from 'react-native';
// @ts-ignore
import resolveAssetSource from 'react-native/Libraries/Image/resolveAssetSource';

import MediaStream from './MediaStream';
import MediaStreamError from './MediaStreamError';


const { WebRTCModule } = NativeModules;

export type YuvSource = {
    asset: number;
    width: number;
    height: number;
}

export default function getYuvMedia(source: YuvSource): Promise<MediaStream> {
    if (
        typeof source !== 'object' ||
        typeof source.asset !== 'number' ||
        typeof source.height !== 'number' ||
        typeof source.width !== 'number'
    ) {
        return Promise.reject(new TypeError('invalid source'));
    }

    const asset = resolveAssetSource(source.asset)?.uri as string;
    const { width, height } = source;

    return new Promise((resolve, reject) => {
        WebRTCModule.getYuvMedia(asset).then(
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
