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
