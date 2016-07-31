# react-native-webrtc

A WebRTC module for React Native.

## Support
- Currently support for iOS and Android.  
- Support video and audio communication.  
- Supports data channels.  
- You can use it to build an iOS/Android app that can communicate with web browser.  
- The WebRTC Library is based on [webrtc-build-scripts](https://github.com/pristineio/webrtc-build-scripts)

## WebRTC Revision

please see [#79](https://github.com/oney/react-native-webrtc/issues/79) for discussions.

| react-native-webrtc | WebRTC(ios) | WebRTC(android)  |
| :-------------: |:-------------:| :-----:|
| <= 0.9.0    | branch ~47 beta (11177)  | branch ~47 beta (11139)  |
| 0.10.0      | branch 52 stable (13039) | branch 52 stable (13039) |

## Installation

### prerequisite: Git Large File Storage ( Git LFS )
since 0.10.0, we upgrade webrtc library to branch 52 stable release, and store library on [Git Large File Storage](https://git-lfs.github.com/)  
you may need to install `git lfs` to automatically download library when `git clone` or `npm install`.  

belows are brief memo, please go to [Git LFS official website](https://git-lfs.github.com/) for details.  

**Linux:** download `git-lfs tar file` and execute `install.sh` inside it.  
**Mac:** `brew install git-lfs` or `port install git-lfs` then `git lfs install`  

### react-native-webrtc:

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
  RTCView,
  MediaStreamTrack,
  getUserMedia,
} = WebRTC;
```
Anything about using RTCPeerConnection, RTCSessionDescription and RTCIceCandidate is like browser.  
Support most WebRTC APIs, please see the [Document](https://developer.mozilla.org/zh-TW/docs/Web/API/RTCPeerConnection).
```javascript
var configuration = {"iceServers": [{"url": "stun:stun.l.google.com:19302"}]};
var pc = new RTCPeerConnection(configuration);
MediaStreamTrack.getSources(sourceInfos => {
  var videoSourceId;
  for (var i = 0; i < sourceInfos.length; i++) {
    var sourceInfo = sourceInfos[i];
    if(sourceInfo.kind == "video" && sourceInfo.facing == "front") {
      videoSourceId = sourceInfo.id;
    }
  }
  getUserMedia({
    "audio": true,
    "video": {
      optional: [{sourceId: videoSourceId}]
    }
  }, function (stream) {
    pc.addStream(stream);
  }, logError);
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

## Native control
Use [react-native-incall-manager](https://github.com/zxcpoiu/react-native-incall-manager) to keep screen on, mute microphone, etc.
