# react-native-webrtc

[![npm version](https://badge.fury.io/js/react-native-webrtc.svg)](https://badge.fury.io/js/react-native-webrtc)
[![npm downloads](https://img.shields.io/npm/dm/react-native-webrtc.svg?maxAge=2592000)](https://img.shields.io/npm/dm/react-native-webrtc.svg?maxAge=2592000)

A WebRTC module for React Native.
- Support iOS / Android.
- Support Video / Audio / Data Channels.

**NOTE** for Expo users: this plugin doesn't work unless you eject.

## Comunity

Everyone is welcome to you our [Discourse community](https://react-native-webrtc.discourse.group/) to discuss any React Native and WebRTC related topics.

## WebRTC Revision

| react-native-webrtc | WebRTC Version | arch(ios) | arch(android)  | npm published | note | additional picks |
| :-------------: | :-------------:| :-----: | :-----: | :-----: | :-----: | :-----: |
| 1.69.0 | [M69](https://chromium.googlesource.com/external/webrtc/+/branch-heads/69)<br>[commit](https://chromium.googlesource.com/external/webrtc/+/9110a54a60d9e0c69128338fc250319ddb751b5a)<br>(24012)<br>(+16-24348) | x86_64<br>i386<br>armv7<br>arm64 | armeabi-v7a<br>x86 | :heavy_check_mark: |  |  |
| master | [M69](https://chromium.googlesource.com/external/webrtc/+/branch-heads/69)<br>[commit](https://chromium.googlesource.com/external/webrtc/+/9110a54a60d9e0c69128338fc250319ddb751b5a)<br>(24012)<br>(+16-24348) | x86_64<br>i386<br>armv7<br>arm64 | armeabi-v7a<br>x86 | :warning: | test me plz |  |

Please see [wiki page](https://github.com/oney/react-native-webrtc/wiki) about revision history.

## Installation

- [iOS](https://github.com/oney/react-native-webrtc/blob/master/Documentation/iOSInstallation.md)
- [Android](https://github.com/oney/react-native-webrtc/blob/master/Documentation/AndroidInstallation.md)

## Usage
Now, you can use WebRTC like in browser.
In your `index.ios.js`/`index.android.js`, you can require WebRTC to import RTCPeerConnection, RTCSessionDescription, etc.

```javascript
import {
  RTCPeerConnection,
  RTCIceCandidate,
  RTCSessionDescription,
  RTCView,
  MediaStream,
  MediaStreamTrack,
  mediaDevices
} from 'react-native-webrtc';
```
Anything about using RTCPeerConnection, RTCSessionDescription and RTCIceCandidate is like browser.
Support most WebRTC APIs, please see the [Document](https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection).

```javascript
const configuration = {"iceServers": [{"url": "stun:stun.l.google.com:19302"}]};
const pc = new RTCPeerConnection(configuration);

let isFront = true;
mediaDevices.enumerateDevices().then(sourceInfos => {
  console.log(sourceInfos);
  let videoSourceId;
  for (let i = 0; i < sourceInfos.length; i++) {
    const sourceInfo = sourceInfos[i];
    if(sourceInfo.kind == "video" && sourceInfo.facing == (isFront ? "front" : "back")) {
      videoSourceId = sourceInfo.id;
    }
  }
  mediaDevices.getUserMedia({
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
  })
  .then(stream => {
    // Got stream!
  })
  .catch(error => {
    // Log error
  });
});

pc.createOffer().then(desc => {
  pc.setLocalDescription(des).then(() => {
    // Send pc.localDescription to peer
  });
});

pc.onicecandidate = function (event) {
  // send event.candidate to peer
};

// also support setRemoteDescription, createAnswer, addIceCandidate, onnegotiationneeded, oniceconnectionstatechange, onsignalingstatechange, onaddstream

```
However, render video stream should be used by React way.

Rendering RTCView.

```javascript
<RTCView streamURL={this.state.stream.toURL()}/>
```

### Custom APIs

#### MediaStreamTrack.prototype._switchCamera()

This function allows to switch the front / back cameras in a video track
on the fly, without the need for adding / removing tracks or renegotiating.

#### VideoTrack.enabled

Starting with version 1.67, when setting a local video track's enabled state to
`false`, the camera will be closed, but the track will remain alive. Setting
it back to `true` will re-enable the camera.

## Related projects

### react-native-incall-manager

Use [react-native-incall-manager](https://github.com/zxcpoiu/react-native-incall-manager) to keep screen on, mute microphone, etc.

## Sponsorship
This repository doesn't have a plan to get sponsorship.(This can be discussed afterwards by collaborators). If you would like to pay bounty to fix some bugs or get some features, be free to open a issue that adds `[BOUNTY]` category in title. Add other bounty website link like [this](https://www.bountysource.com) will be better.

