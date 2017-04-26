## iOS Installation

**If you used this module before, please remove `RCTWebRTC.xcodeproject`/`libjingle_peerconnection` and follow   instructions below.**

`npm install react-native-webrtc --save`  

## 1. Add Files Into Project

1.) in Xcode: Right click `Libraries` ➜ `Add Files to [project]`  
2.) choose `node_modules/react-native-webrtc/ios/RCTWebRTC.xcodeproj` then `Add`  
3.) also add `node_modules/react-native-webrtc/ios/WebRTC.framework` to project root or anywhere you want:  

![Picture 4](https://github.com/oney/react-native-webrtc/blob/master/Documentation/doc_install_xcode_add_xcodeproject.png)

4.) you will ended up with structure like:  

![Picture 4](https://github.com/oney/react-native-webrtc/blob/master/Documentation/doc_install_xcode_file_structure.png)

## iOS Podfile

You can use the included podspec in your podfile to take care of all dependencies instead of manually adding files to the project (instead of steps 2 through 5, but you might still have to do step 6 above).

Include in a Podfile in your react-native ios directory:

```
pod 'react-native-webrtc', :path => '../node_modules/react-native-webrtc'
```

## 2. Add Library Search Path

1.) select `Build Settings`, find `Search Paths`  
2.) edit BOTH `Framework Search Paths` and `Library Search Paths`  
3.) add path on BOTH sections with: `$(SRCROOT)/../node_modules/react-native-webrtc` with `recursive`  

![Picture 4](https://github.com/oney/react-native-webrtc/blob/master/Documentation/doc_install_xcode_search_path.png)

## 3. Change General Setting and Embed Framework

1.) go to `General` tab  
2.) change `Deployment Target` to `8.0`  
3.) add `Embedded Binaries` like below:  

![Picture 4](https://github.com/oney/react-native-webrtc/blob/master/Documentation/doc_install_xcode_embed_framework.png)


## 4. Link/Include Necessary Libraries


1.) click `Build Phases` tab, open `Link Binary With Libraries`  
2.) add `libRCTWebRTC.a`  
3.) make sure WebRTC.framework linked  
4.) add the following libraries:  

```
AVFoundation.framework
AudioToolbox.framework
CoreGraphics.framework
GLKit.framework
CoreAudio.framework
CoreVideo.framework
VideoToolbox.framework
libc.tbd
libsqlite3.tbd
libstdc++.tbd
```

5.) Under `Build setting` set `Dead Code Stripping` to `No` also under `Build Options` set `Enable Bitcode` to `No` as well  

![Picture 4](https://github.com/oney/react-native-webrtc/blob/master/Documentation/doc_install_xcode_link_libraries.png)


## CLEAN PROCESS

if you encounter any build time errors, like "linking library not found",  
try the cleaning steps below, and do it again carefully with every steps.

1. remove npm module: `rm -rf $YourProject/node_modules/react-native-webrtc`
2. clean npm cache: `npm cache clean`
3. clear temporary build files ( depends on your env )
    * ANDROID: clear intermediate files in `gradle buildDir`
    * iOS: in xocde project, click `Product` -> `clean`
4. `npm install react-native-webrtc`

## App Store Submission

according to [#141](https://github.com/oney/react-native-webrtc/issues/141)
you should strip i386/x86_64 arch from framework before submit to app store.

the script below is provided by [@besarthoxhaj](https://github.com/besarthoxhaj)  
all credit goes to [@besarthoxhaj](https://github.com/besarthoxhaj), thanks!

see [#141](https://github.com/oney/react-native-webrtc/issues/141) for more details

```javascript
'use strict';

const fs = require('fs');
const exec = require('child_process').execSync;

const WEBRTC_BIN_PATH = `${__dirname}/node_modules/react-native-webrtc/ios/WebRTC.framework`;
const ARCH_TYPES = ['i386','x86_64','armv7','arm64'];

if(process.argv[2] === '--extract' || process.argv[2] === '-e'){
  console.log(`Extracting...`);
  ARCH_TYPES.forEach(elm => {
    exec(`lipo -extract ${elm} WebRTC -o WebRTC-${elm}`,{cwd:WEBRTC_BIN_PATH});
  });
  exec('cp WebRTC WebRTC-all',{cwd:WEBRTC_BIN_PATH});
  console.log(exec('ls -ahl | grep WebRTC-',{cwd:WEBRTC_BIN_PATH}).toString().trim());
  console.log('Done!');
}

if(process.argv[2] === '--simulator' || process.argv[2] === '-s'){
  console.log(`Compiling simulator...`);
  exec(`lipo -o WebRTC -create WebRTC-x86_64 WebRTC-i386`,{cwd:WEBRTC_BIN_PATH});
  console.log(exec('ls -ahl | grep WebRTC',{cwd:WEBRTC_BIN_PATH}).toString().trim());
  console.log('Done!');
}

if(process.argv[2] === '--device' || process.argv[2] === '-d'){
  console.log(`Compiling device...`);
  exec(`lipo -o WebRTC -create WebRTC-armv7 WebRTC-arm64`,{cwd:WEBRTC_BIN_PATH});
  console.log(exec('ls -ahl | grep WebRTC',{cwd:WEBRTC_BIN_PATH}).toString().trim());
  console.log('Done!');
}
```
