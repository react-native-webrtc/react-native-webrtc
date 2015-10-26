# react-native-webrtc

A WebRTC module for React Native.

## Support
Currently support for iOS only.  
Support video and audio communication. Not support DataChannel now.  
You can use it to build an app that can communicate with web browser.  
The iOS Library is based on libWebRTC.a(It's build by [webrtc-build-scripts](https://github.com/pristineio/webrtc-build-scripts) and you can download it [here](https://cocoapods.org/pods/libjingle_peerconnection))

## Installation

### [iOS](https://github.com/oney/react-native-webrtc/blob/master/Documentation/iOSInstallation.md)
### [Android](https://github.com/oney/react-native-webrtc/blob/master/Documentation/AndroidInstallation.md)

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
