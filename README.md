# react-native-webrtc

A WebRTC module for React Native.

## Support
- Currently support for iOS and Android.  
- Support video and audio communication. Not support DataChannel now.  
- You can use it to build an iOS/Android app that can communicate with web browser.  
- The WebRTC Library is based on [webrtc-build-scripts](https://github.com/pristineio/webrtc-build-scripts)

## Installation

- [iOS](https://github.com/oney/react-native-webrtc/blob/master/Documentation/iOSInstallation.md)
- [Android](https://github.com/oney/react-native-webrtc/blob/master/Documentation/AndroidInstallation.md)

## Usage
Now, you can use WebRTC like in browser.
In your `index.ios.js`/`index.android.js`, you can require WebRTC to import RTCPeerConnection, RTCSessionDescription, etc.
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
Anything about using RTCPeerConnection, RTCSessionDescription and RTCIceCandidate is like browser.  
Support most WebRTC APIs, please see the [Document](https://developer.mozilla.org/zh-TW/docs/Web/API/RTCPeerConnection).
```javascript
var configuration = {"iceServers": [{"url": "stun:stun.l.google.com:19302"}]};
var pc = new RTCPeerConnection(configuration);
navigator.getUserMedia({ "audio": true, "video": true }, function (stream) {
  pc.addStream(stream);
});
pc.createOffer(function(desc) {
  pc.setLocalDescription(desc, function () {
    // Send pc.localDescription to peer
  }, function(e) {});
}, function(e) {});
pc.onicecandidate = function (event) {
  // send event.candidate to peer
};
// also support setRemoteDescription, createAnswer, addIceCandidate, onnegotiationneeded, oniceconnectionstatechange, onsignalingstatechange, onaddstream

```
However, render video stream should be used by React way.

Rendering RTCView.
```javascript
var container;
var RCTWebRTCDemo = React.createClass({
  getInitialState: function() {
    return {videoURL: null};
  },
  componentDidMount: function() {
    container = this;
  },
  render: function() {
    return (
      <View>
        <RTCView streamURL={this.state.videoURL}/>
      </View>
    );
  }
});
```
And set stream to RTCView
```javascript
container.setState({videoURL: stream.toURL()});
```
## Demo
The demo project is https://github.com/oney/RCTWebRTCDemo   
And you will need a signaling server. I have written a signaling server https://react-native-webrtc.herokuapp.com/ (the repository is https://github.com/oney/react-native-webrtc-server).   
You can open this website in browser, and then set it as signaling server in the app, and run the app. After you enter the same room ID, the video stream will be connected.
