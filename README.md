# react-native-webrtc

A WebRTC module for React Native.

## Installation

1. Run `react-native init RCTWebRTCDemo` to create your react native project. (RCTWebRTCDemo can be any name you like)
2. Add `react-native-webrtc` to your `package.json`'s `dependencies`.
```
"dependencies": {
  "react-native": "^0.11.2",
  "react-native-webrtc": "https://github.com/oney/react-native-webrtc"
}
```
3. Run `npm install`
4. Open `RCTWebRTCDemo/node_modules/react-native-webrtc`
5. Drag `RCTWebRTC` directory to you project. Uncheck `Copy items if needed` and select `Added folders: Create groups`
![Picture 1](http://i.imgur.com/NRHANSq.jpg)
![Picture 2](http://i.imgur.com/8fX2fDM.jpg)
![Picture 3](http://i.imgur.com/vVDTIXD.jpg)
6. Select your target, select `Build Phases`, open `Link Binary With Libraries`, add those libraries
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
7. select `Build Settings`, find `Library Search Paths`, add `$(SRCROOT)/../node_modules/react-native-webrtc` with `recursive`
![Picture 5](http://i.imgur.com/L3QkvzG.jpg)
8. Maybe you have to set `Dead Code Stripping` to `No` and `Enable Bitcode` to `No`.

## Usage
Now, you can use WebRTC like in brower.
In your index.ios.js, you can require WebRTC to import RTCPeerConnection, RTCSessionDescription, etc.
```
var WebRTC = require('react-native-webrtc');
var {
  RTCPeerConnection,
  RTCMediaStream,
  RTCIceCandidate,
  RTCSessionDescription,
  RTCView
} = WebRTC;
```
Anything about using RTCPeerConnection, RTCSessionDescription and RTCIceCandidate is like brower. However, render video stream should be used by react way.

Rendering RTCView.
```
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
```
container.setState({videoSrc: stream.objectId});
```

