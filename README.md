# react-native-webrtc

A WebRTC module for React Native.

## Support
Currently support for iOS only.  
Support video and audio communication. Not support DataChannel now.  
You can use it to build an app that can communicate with web browser.  
The iOS Library is based on libWebRTC.a(It's build by [webrtc-build-scripts](https://github.com/pristineio/webrtc-build-scripts) and you can download it [here](https://cocoapods.org/pods/libjingle_peerconnection))

## Installation

1.) Run `react-native init RCTWebRTCDemo` to create your react native project. (RCTWebRTCDemo can be any name you like)   
2.) `npm install react-native-webrtc --save`  
3.) Open `RCTWebRTCDemo/node_modules/react-native-webrtc`  
4.) Drag `RCTWebRTC` directory to you project. Uncheck `Copy items if needed` and select `Added folders: Create groups`  
![Picture 1](http://i.imgur.com/NRHANSq.jpg)
![Picture 2](http://i.imgur.com/8fX2fDM.jpg)
![Picture 3](http://i.imgur.com/vVDTIXD.jpg)  
5.) Select your target, select `Build Phases`, open `Link Binary With Libraries`, add these libraries  
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
![Picture 4](http://i.imgur.com/hHNfKkZ.jpg)  
6.) select `Build Settings`, find `Library Search Paths`, add `$(SRCROOT)/../node_modules/react-native-webrtc` with `recursive` 
![Picture 5](http://i.imgur.com/L3QkvzG.jpg)  
7.) Maybe you have to set `Dead Code Stripping` to `No` and `Enable Bitcode` to `No`.

## Usage
Now, you can use WebRTC like in browser.
In your index.ios.js, you can require WebRTC to import RTCPeerConnection, RTCSessionDescription, etc.
```javascript
var WebRTC = require('react-native-webrtc');
var {
  RTCPeerConnection,
  RTCMediaStream,
  RTCIceCandidate,
  RTCSessionDescription,
  RTCView
} = WebRTC;
```
Anything about using RTCPeerConnection, RTCSessionDescription and RTCIceCandidate is like browser. However, render video stream should be used by React way.

Rendering RTCView.
```javascript
var container;
var RCTWebRTCDemo = React.createClass({
  getInitialState: function() {
    return {videoSrc: null};
  },
  componentDidMount: function() {
    container = this;
  },
  render: function() {
    return (
      <View>
        <RTCView src={this.state.videoSrc}/>
      </View>
    );
  }
});
```
And set stream to RTCView
```javascript
container.setState({videoSrc: stream.objectId});
```
## Demo
The demo project is https://github.com/oney/RCTWebRTCDemo   
And you will need a signaling server. I have written a signaling server http://react-native-webrtc.herokuapp.com/ (the repository is https://github.com/oney/react-native-webrtc-server).   
You can open this website in browser, and then set it as signaling server in the app, and run the app. After you enter the same room ID, the video stream will be connected.
