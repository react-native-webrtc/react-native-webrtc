# react-native-webrtc

[![npm version](https://badge.fury.io/js/react-native-webrtc.svg)](https://badge.fury.io/js/react-native-webrtc)
[![npm downloads](https://img.shields.io/npm/dm/react-native-webrtc.svg?maxAge=2592000)](https://img.shields.io/npm/dm/react-native-webrtc.svg?maxAge=2592000)

A WebRTC module for React Native.

## Support
- Currently support for iOS and Android.  
- Support video and audio communication.  
- Supports data channels.  
- You can use it to build an iOS/Android app that can communicate with web browser.  
- The WebRTC Library is based on [webrtc-build-scripts](https://github.com/pristineio/webrtc-build-scripts)

## WebRTC Revision

Since `0.53`, we use same branch version number like in webrtc native.
please see [wiki page](https://github.com/oney/react-native-webrtc/wiki) about revision history 

### format:

`${branch_name} stable (${branched_from_revision})(+${Cherry-Picks-Num}-${Last-Cherry-Picks-Revision})`

* the webrtc revision in brackets is extracting frrom `Cr-Branched-From` instead `Cr-Commit-Position`  
* the number follows with `+` is the additional amount of cherry-picks since `Branched-From` revision.

### note:
the order of commit revision is nothing to do with the order of cherry-picks, for example, the esarlier committed `cherry-pick-#2` may have higher revision than `cherry-pick-#3` and vice versa.

| react-native-webrtc | WebRTC(ios) | WebRTC(android)  | npm published | note |
| :-------------: | :-------------:| :-----: | :-----: | :-----: | :-----: |
| 0.53.2 | 53 stable<br>(13317)<br>(+6-13855)<br>32/64 | 53 stable<br>(13317)<br>(+6-13855)<br>32 | :heavy_check_mark: | |
| master | 53 stable<br>(13317)<br>(+6-13855)<br>32/64 | 53 stable<br>(13317)<br>(+6-13855)<br>32 | :warning:          | |

## Installation

### react-native-webrtc:

- [iOS](https://github.com/oney/react-native-webrtc/blob/master/Documentation/iOSInstallation.md)
- [Android](https://github.com/oney/react-native-webrtc/blob/master/Documentation/AndroidInstallation.md)

note: 0.10.0~0.12.0 required `git-lfs`, see: [git-lfs-installation](https://github.com/oney/react-native-webrtc/blob/master/Documentation/git-lfs-installation.md) 

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

## Sponsorship
This repository doesn't have a plan to get sponsorship.(This can be discussed afterwards by collaborators). If you would like to pay bounty to fix some bugs or get some features, be free to open a issue that adds `[BOUNTY]` category in title. Add other bounty website link like [this](https://www.bountysource.com) will be better.

