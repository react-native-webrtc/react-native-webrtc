## iOS Installation

1.) `npm install react-native-webrtc --save`  
2.) Xcode: Right click `Libraries` ➜ `Add Files to [project]`  
3.) Choose `node_modules/react-native-webrtc/ios/RCTWebRTC.xcodeproj` and `libjingle_peerconnection` directory  
![Picture 4](http://i.imgur.com/5K6JfWt.jpg)  
![Picture 4](http://i.imgur.com/KMAyYU9.jpg)  
4.) Select your target, select `Build Phases`, open `Link Binary With Libraries`, add `libRCTWebRTC.a` and these libraries  
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
![Picture 4](http://i.imgur.com/YyA9eQy.jpg)  
5.) select `Build Settings`, find `Library Search Paths`, add `$(SRCROOT)/../node_modules/react-native-webrtc` with `recursive`
![Picture 5](http://i.imgur.com/L3QkvzG.jpg)  
6.) Maybe you have to set `Dead Code Stripping` to `No` and `Enable Bitcode` to `No`.

## iOS Podfile

You can use the included podspec in your podfile to take care of all dependencies instead of manually adding files to the project (instead of steps 2 through 5, but you might still have to do step 6 above).

Include in a Podfile in your react-native ios directory:

```
pod 'RCTWebRTC', :path => '../node_modules/react-native-webrtc'
```

## Note
If you used this module before, please remove `RCTWebRTC` directory and follow above guide.
