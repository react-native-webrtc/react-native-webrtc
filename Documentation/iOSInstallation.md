## iOS Installation

**If you used this module before, please remove `RCTWebRTC.xcodeproject`/`libjingle_peerconnection` and follow   instructions below.**

`npm install react-native-webrtc --save`  

## Step 1. Add Files Into Project

1-1.) in Xcode: Right click `Libraries` ➜ `Add Files to [project]`  
1-2.) choose `node_modules/react-native-webrtc/ios/RCTWebRTC.xcodeproj` then `Add`  
1-3.) also add `node_modules/react-native-webrtc/ios/WebRTC.framework` to project root or anywhere you want:  

![Picture 4](https://github.com/react-native-webrtc/react-native-webrtc/blob/master/Documentation/doc_install_xcode_add_xcodeproject.png)

1-4.) you will ended up with structure like:  

![Picture 4](https://github.com/react-native-webrtc/react-native-webrtc/blob/master/Documentation/doc_install_xcode_file_structure.png)

## iOS Podfile

You can use the included podspec in your podfile to take care of all dependencies instead of manually adding files to the project (instead of steps 2 through 4, but you might still have to do Appendix A/B ).

Include in a Podfile in your react-native ios directory:

```
pod 'react-native-webrtc', :path => '../node_modules/react-native-webrtc'
```

You may have to change the `platform` field in your Podfile, as `react-native-webrtc` doesn't support iOS 9 - set it to '10.0' or above (otherwise you get an error when doing `pod install`):

```
platform :ios, '10.0'
```

## Step 2. Add Library Search Path

2-1.) select `Build Settings`, find `Search Paths`  
2-2.) edit BOTH `Framework Search Paths` and `Library Search Paths`  
2-3.) add path on BOTH sections with: `$(SRCROOT)/../node_modules/react-native-webrtc/ios` with `recursive`  

![Picture 4](https://github.com/react-native-webrtc/react-native-webrtc/blob/master/Documentation/doc_install_xcode_search_path.png)

## Step 3. Change General Setting and Embed Framework

3-1.) go to `General` tab  
3-2.) change `Deployment Target` to `8.0`  
3-3.) add `Embedded Binaries` like below:  

![Picture 4](https://github.com/react-native-webrtc/react-native-webrtc/blob/master/Documentation/doc_install_xcode_embed_framework.png)


## Step 4. Link/Include Necessary Libraries


4-1.) click `Build Phases` tab, open `Link Binary With Libraries`  
4-2.) add `libRCTWebRTC.a`  
4-3.) make sure WebRTC.framework linked  
4-4.) add the following libraries:  

```
AVFoundation.framework
AudioToolbox.framework
CoreGraphics.framework
GLKit.framework
CoreAudio.framework
CoreVideo.framework
VideoToolbox.framework
libc.tbd
libc++.tbd
libsqlite3.tbd
```
![Picture 4](https://github.com/react-native-webrtc/react-native-webrtc/blob/master/Documentation/doc_install_xcode_link_libraries.png)
4-5.) Under `Build setting` set `Dead Code Stripping` to `No` also under `Build Options` set `Enable Bitcode` to `No` as well  



## Step 5. Add Permissions

5-1.) navigate to `<ProjectFolder>/ios/<ProjectName>/`  
5-2.) edit `Info.plist` and add the following lines

```
<key>NSCameraUsageDescription</key>
<string>Camera Permission</string>
<key>NSMicrophoneUsageDescription</key>
<string>Microphone Permission</string>
```

## Appendix A - CLEAN PROCESS

if you encounter any build time errors, like "linking library not found",  
try the cleaning steps below, and do it again carefully with every steps.

1. remove npm module: `rm -rf $YourProject/node_modules/react-native-webrtc`  
2. clean npm cache: `npm cache clean`  
3. clear temporary build files ( depends on your env )    
    * ANDROID: clear intermediate files in `gradle buildDir`    
    * iOS: in xocde project, click `Product` -> `clean`    
4. `npm install react-native-webrtc`  
  
## Appendix B - Apple Store Submission

(ios only)

You should strip simulator (i386/x86_64) archs from WebRTC binary before submit to Apple Store.  
We provide a handy script to do it easily. see below sections.

credit: The script is originally provided by [@besarthoxhaj](https://github.com/besarthoxhaj) in [#141](https://github.com/react-native-webrtc/react-native-webrtc/issues/141), thanks!

#### Strip Simulator Archs Usage

The script and example are here: https://github.com/react-native-webrtc/react-native-webrtc/blob/master/tools/ios_arch.js

1. go to `react-native-webrtc/tools` folder
2. extract all archs first: `node ios_arch.js --extract`
3. re-package device related archs only: `node ios_arch.js --device`
4. delete files generated from `step 2` under `node_modules/react-native-webrtc/ios WebRTC.framework/` (e.g. with a command `rm node_modules/react-native-webrtc/ios/WebRTC.framework/WebRTC-*` from application root)
5. you can check current arch use this command: `file node_modules/react-native-webrtc/ios/WebRTC.framework/WebRTC`
