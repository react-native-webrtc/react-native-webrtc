
import { NativeModules, Platform } from 'react-native';
// @ts-ignore
import resolveAssetSource from 'react-native/Libraries/Image/resolveAssetSource';

import MediaStream from './MediaStream';
import MediaStreamError from './MediaStreamError';


const { WebRTCModule } = NativeModules;

export type YuvAsset = {
    type: 'yuv';
    src: number;
    width: number;
    height: number;
    cache?: boolean;
}

export type ImageAsset = {
    type: 'img';
    src: string | number;
}

function getFileMediaIos(asset: ImageAsset): Promise<MediaStream> {
    if (
        typeof asset !== 'object' ||
        typeof asset.src !== 'number'
    ) {
        return Promise.reject(new TypeError('invalid asset'));
    }

    const src = resolveAssetSource(asset.src)?.uri as string;

    return new Promise((resolve, reject) => {
        WebRTCModule.getFileMedia(src).then(
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

function getFileMediaAndroid(asset: YuvAsset): Promise<MediaStream> {
    if (
        typeof asset !== 'object' ||
        typeof asset.src !== 'number' ||
        typeof asset.height !== 'number' ||
        typeof asset.width !== 'number'
    ) {
        return Promise.reject(new TypeError('invalid asset'));
    }

    const src = resolveAssetSource(asset.src)?.uri as string;
    const { width, height, cache } = asset;

    return new Promise((resolve, reject) => {
        WebRTCModule.getFileMedia(src, width, height, cache === true).then(
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

export default function getFileMedia(asset: YuvAsset | ImageAsset): Promise<MediaStream> {
    const platform = Platform.OS;

    if (platform === 'ios') {
        if (asset.type === 'img') {
            return getFileMediaIos(asset);
        } else {
            return Promise.reject(new TypeError(`${platform} only supports image assets`));
        }
    }

    if (platform === 'android') {
        if (asset.type === 'yuv') {
            return getFileMediaAndroid(asset);
        } else {
            return Promise.reject(new TypeError(`${platform} only supports yuv assets`));
        }
    }

    return Promise.reject(new TypeError(`${Platform.OS} not supported`));
}
