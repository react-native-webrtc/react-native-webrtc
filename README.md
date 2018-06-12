# react-native-webrtc

[![npm version](https://badge.fury.io/js/react-native-webrtc.svg)](https://badge.fury.io/js/react-native-webrtc)
[![npm downloads](https://img.shields.io/npm/dm/react-native-webrtc.svg?maxAge=2592000)](https://img.shields.io/npm/dm/react-native-webrtc.svg?maxAge=2592000)

A WebRTC module for React Native.

# BREAKING FOR RN 40:

`master` branch needs RN >= 40 for now.
if your RN version is under 40, use branch [rn-less-40](https://github.com/oney/react-native-webrtc/tree/rn-less-40) (npm version `0.54.7`)

see [#190](https://github.com/oney/react-native-webrtc/pull/190) for detials

## Support
- Currently support for iOS and Android.  
- Support video and audio communication.  
- Supports data channels.  
- You can use it to build an iOS/Android app that can communicate with web browser.  

## WebRTC Revision

Since `0.53`, we use same branch version number like in webrtc native.
please see [wiki page](https://github.com/oney/react-native-webrtc/wiki) about revision history 

### format:

`${branch_name} stable (${branched_from_revision})(+${Cherry-Picks-Num}-${Last-Cherry-Picks-Revision})`

* the webrtc revision in brackets is extracting frrom `Cr-Branched-From` instead `Cr-Commit-Position`  
* the number follows with `+` is the additional amount of cherry-picks since `Branched-From` revision.

### note:
the order of commit revision is nothing to do with the order of cherry-picks, for example, the earlier committed `cherry-pick-#2` may have higher revision than `cherry-pick-#3` and vice versa.

| react-native-webrtc | WebRTC Version | arch(ios) | arch(android)  | npm published | note | additional picks |
| :-------------: | :-------------:| :-----: | :-----: | :-----: | :-----: | :-----: |
| 0.54.7 | [M54](https://chromium.googlesource.com/external/webrtc/+/branch-heads/54)<br>(13869)<br>(+6-14091) | x86_64<br>i386<br>armv7<br>arm64 | armeabi-v7a<br>x86 | :heavy_check_mark: | RN < 40 | |
| 1.57.1 | [M57](https://chromium.googlesource.com/external/webrtc/+/branch-heads/57)<br>(16123)<br>(+7-16178) | x86_64<br>i386<br>armv7<br>arm64 | armeabi-v7a<br>x86 | :heavy_check_mark: | |* [16805](https://chromium.googlesource.com/external/webrtc/+/0e22a4cfd3790d80ad1ae699891341fe322cb418)<br>* [16462](https://chromium.googlesource.com/external/webrtc.git/+/1634e160426df926e14cf9f1e5346d2a1dc9c909)  |
| 1.58.3 | [M58](https://chromium.googlesource.com/external/webrtc/+/branch-heads/58)<br>[commit](https://chromium.googlesource.com/external/webrtc/+/6504196a312da382d75c5ff577d10207793f2907)<br>(16937)<br>(+21-18206) | x86_64<br>i386<br>armv7<br>arm64 | armeabi-v7a<br>x86 | :heavy_check_mark: | :sparkles: Promise Support :sparkles: | * [17065](https://chromium.googlesource.com/external/webrtc/+/d1587ad244af3388c1282a715cdf05032ba0c2fc)<br>* [17925](https://chromium.googlesource.com/external/webrtc/+/f68426954154918ec5bf7e6b3096fa3d7acb0944)<br>* [18140](https://chromium.googlesource.com/external/webrtc/+/7daab660ce0e35fecad717fefab4cf935d3c253e)<br>* [18277](https://chromium.googlesource.com/external/webrtc/+/eae4564cb7dea3ad9f5963814e82fd823267ff89) |
| 1.63.0 | [M63](https://chromium.googlesource.com/external/webrtc/+/branch-heads/63)<br>[commit](https://chromium.googlesource.com/external/webrtc/+/dc5904661c4c8b357e19595d80b2881db3f4a71b)<br>(20237)<br>(+14-21432) | x86_64<br>i386<br>armv7<br>arm64 | armeabi-v7a<br>x86 | :heavy_check_mark: |  | * [20611](https://chromium.googlesource.com/external/webrtc/+/7281f92e7210fb48f8b34c1bbb8f037808a73868)<br>* [21519](https://chromium.googlesource.com/external/webrtc/+/730add8e2c8b033eae0cf3589be14d0afc4acdd3) |
| master | [M63](https://chromium.googlesource.com/external/webrtc/+/branch-heads/63)<br>[commit](https://chromium.googlesource.com/external/webrtc/+/dc5904661c4c8b357e19595d80b2881db3f4a71b)<br>(20237)<br>(+14-21432) | x86_64<br>i386<br>armv7<br>arm64 | armeabi-v7a<br>x86 | :warning: | test me plz | * [20611](https://chromium.googlesource.com/external/webrtc/+/7281f92e7210fb48f8b34c1bbb8f037808a73868)<br>* [21519](https://chromium.googlesource.com/external/webrtc/+/730add8e2c8b033eae0cf3589be14d0afc4acdd3) |

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
  RTCIceCandidate,
  RTCSessionDescription,
  RTCView,
  MediaStream,
  MediaStreamTrack,
  getUserMedia,
} = WebRTC;
```
Anything about using RTCPeerConnection, RTCSessionDescription and RTCIceCandidate is like browser.  
Support most WebRTC APIs, please see the [Document](https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection).
```javascript
var configuration = {"iceServers": [{"url": "stun:stun.l.google.com:19302"}]};
var pc = new RTCPeerConnection(configuration);

let isFront = true;
MediaStreamTrack.getSources(sourceInfos => {
  console.log(sourceInfos);
  let videoSourceId;
  for (const i = 0; i < sourceInfos.length; i++) {
    const sourceInfo = sourceInfos[i];
    if(sourceInfo.kind == "video" && sourceInfo.facing == (isFront ? "front" : "back")) {
      videoSourceId = sourceInfo.id;
    }
  }
  getUserMedia({
    audio: true,
    video: {
      mandatory: {
        minWidth: 500, // Provide your own width, height and frame rate here
        minHeight: 300,
        minFrameRate: 30
      },
      facingMode: (isFront ? "user" : "environment"),
      optional: (videoSourceId ? [{sourceId: videoSourceId}] : [])
    }
  }, function (stream) {
    console.log('dddd', stream);
    callback(stream);
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

### Custom APIs

#### MediaStreamTrack.prototype._switchCamera()

This function allows to switch the front / back cameras in a video track
on the fly, without the need for adding / removing tracks or renegotiating.

## Demos

**Official Demo**

author: [@oney](https://github.com/oney)

The demo project is https://github.com/oney/RCTWebRTCDemo   
And you will need a signaling server. I have written a signaling server https://react-native-webrtc.herokuapp.com/ (the repository is https://github.com/oney/react-native-webrtc-server).   
You can open this website in browser, and then set it as signaling server in the app, and run the app. After you enter the same room ID, the video stream will be connected.

**Demo by Folks**

author: [@thoqbk](https://github.com/thoqbk)
- Signaling server and web app: https://rewebrtc.herokuapp.com/ (the repository is https://github.com/thoqbk/rewebrtc-server)
- React native app repository: https://github.com/thoqbk/rewebrtc

## Native control
Use [react-native-incall-manager](https://github.com/zxcpoiu/react-native-incall-manager) to keep screen on, mute microphone, etc.

## Sponsorship
This repository doesn't have a plan to get sponsorship.(This can be discussed afterwards by collaborators). If you would like to pay bounty to fix some bugs or get some features, be free to open a issue that adds `[BOUNTY]` category in title. Add other bounty website link like [this](https://www.bountysource.com) will be better.

