
const fs = require('fs');
const https = require('https');
const path = require('path');
const process = require('process');
const tar = require('tar');

const pkg = require('../package.json');
const builds = pkg['webrtc-builds'];


async function download(url, filePath) {
    return new Promise((resolve, reject) => {
        const request = https.get(url, response => {
            if (response.statusCode === 301 || response.statusCode === 302) {
                resolve(download(response.headers.location, filePath));
                return;
            }
            
            if (response.statusCode !== 200) {
                reject(new Error(`Failed to get '${url}' (${response.statusCode})`));
                return;
            }
            
            const file = fs.createWriteStream(filePath);
            
            file.on('finish', () => resolve());

            file.on('error', err => {
                fs.unlink(filePath, () => reject(err));
            });

            response.pipe(file);
        });
        
        request.on('error', err => {
            fs.unlink(filePath, () => reject(err));
        });
        
        request.end();
    });
}

(async () => {
    if (process.env.RN_WEBRTC_SKIP_DOWNLOAD) {
        console.log('Skipping WebRTC build downloads')
        return process.exit(0);
    }

    const items = [];

    // iOS
    //

    if (process.platform === 'darwin') {
        const bitcode = process.env.RN_WEBRTC_BITCODE;
        const url = bitcode ? builds['ios-bitcode'] : builds['ios'];

        items.push({
            url,
            dstFileName: 'WebRTC.xcframework.tgz',
            dstDir: `${__dirname}/../apple/`
        });
    }

    // Android
    //

    items.push({
        url: builds['android'],
        dstFileName: 'android-webrtc.tgz',
        dstDir: `${__dirname}/../android/libs/`
    });

    // Download them all!
    //

    for (const item of items) {
        const { url, dstFileName, dstDir } = item;
        const dstPath = path.join(dstDir, dstFileName);

        if (fs.existsSync(dstPath) && process.env.RN_WEBRTC_FORCE_DOWNLOAD) {
            console.log('Removing previously downloaded file')
            fs.rmSync(dstPath);
        }

        console.log(`Downloading ${url}...`);

        await download(url, dstPath);

        tar.extract({
            file: dstPath,
            cwd: dstDir,
            sync: true,
            strict: true
        });

        console.log('Done!');
    }
})();
