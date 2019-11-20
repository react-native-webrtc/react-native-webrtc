# react-native-webrtc

[![npm version](https://badge.fury.io/js/react-native-webrtc.svg)](https://badge.fury.io/js/react-native-webrtc)
[![npm downloads](https://img.shields.io/npm/dm/react-native-webrtc.svg?maxAge=2592000)](https://img.shields.io/npm/dm/react-native-webrtc.svg?maxAge=2592000)

A WebRTC module for React Native.
- Support iOS / Android.
- Support Video / Audio / Data Channels.

**NOTE** for Expo users: this plugin doesn't work unless you eject.

## Community

Everyone is welcome to our [Discourse community](https://react-native-webrtc.discourse.group/) to discuss any React Native and WebRTC related topics.

## WebRTC Revision

| react-native-webrtc | WebRTC Version | arch(ios) | arch(android)  | npm published | notes |
| :-------------: | :-------------:| :-----: | :-----: | :-----: | :-----: |
| 1.75.2 | [M75](https://github.com/jitsi/webrtc/commit/0cd6ce4de669bed94ba47b88cb71b9be0341bb81) | x86_64<br>i386<br>armv7<br>arm64 | armeabi-v7a<br>arm64-v8a<br>x86<br>x86_64 | :heavy_check_mark: | |
| 1.75.1 | [M75](https://github.com/jitsi/webrtc/commit/0cd6ce4de669bed94ba47b88cb71b9be0341bb81) | x86_64<br>i386<br>armv7<br>arm64 | armeabi-v7a<br>arm64-v8a<br>x86<br>x86_64 | :heavy_check_mark: | |
| 1.75.0 | [M75](https://github.com/jitsi/webrtc/commit/0cd6ce4de669bed94ba47b88cb71b9be0341bb81) | x86_64<br>i386<br>armv7<br>arm64 | armeabi-v7a<br>arm64-v8a<br>x86<br>x86_64 | :heavy_check_mark: | |
| 1.69.2 | [M69](https://github.com/jitsi/webrtc/tree/cb536cf7a368e77ec29a6779de7fbebf2c300b70) | x86_64<br>i386<br>armv7<br>arm64 | armeabi-v7a<br>arm64-v8a<br>x86<br>x86_64 | :heavy_check_mark: |  |
| 1.69.1 | [M69](https://chromium.googlesource.com/external/webrtc/+/branch-heads/69)<br>[commit](https://chromium.googlesource.com/external/webrtc/+/9110a54a60d9e0c69128338fc250319ddb751b5a)<br>(24012)<br>(+16-24348) | x86_64<br>i386<br>armv7<br>arm64 | armeabi-v7a<br>arm64-v8a<br>x86<br>x86_64 | :heavy_check_mark: |  |
| 1.69.0 | [M69](https://chromium.googlesource.com/external/webrtc/+/branch-heads/69)<br>[commit](https://chromium.googlesource.com/external/webrtc/+/9110a54a60d9e0c69128338fc250319ddb751b5a)<br>(24012)<br>(+16-24348) | x86_64<br>i386<br>armv7<br>arm64 | armeabi-v7a<br>x86 | :heavy_check_mark: |  |
| master | [M75](https://github.com/jitsi/webrtc/commit/0cd6ce4de669bed94ba47b88cb71b9be0341bb81) | x86_64<br>i386<br>armv7<br>arm64 | armeabi-v7a<br>arm64-v8a<br>x86<br>x86_64 | :warning: | Please test! |

Please see [wiki page](https://github.com/react-native-webrtc/react-native-webrtc/wiki) about revision history.

## Installation

- [iOS](https://github.com/react-native-webrtc/react-native-webrtc/blob/master/Documentation/iOSInstallation.md)
- [Android](https://github.com/react-native-webrtc/react-native-webrtc/blob/master/Documentation/AndroidInstallation.md)

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
  mediaDevices,
  registerGlobals
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
    if(sourceInfo.kind == "videoinput" && sourceInfo.facing == (isFront ? "front" : "environment")) {
      videoSourceId = sourceInfo.deviceId;
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
  pc.setLocalDescription(desc).then(() => {
    // Send pc.localDescription to peer
  });
});

pc.onicecandidate = function (event) {
  // send event.candidate to peer
};

// also support setRemoteDescription, createAnswer, addIceCandidate, onnegotiationneeded, oniceconnectionstatechange, onsignalingstatechange, onaddstream

```

### RTCView

However, render video stream should be used by React way.

Rendering RTCView.

```javascript
<RTCView streamURL={this.state.stream.toURL()}/>
```

| Name                           | Type             | Default                   | Description                                                                                                                                |
| ------------------------------ | ---------------- | ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| mirror                         | boolean          | false               | Indicates whether the video specified by "streamURL" should be mirrored during rendering. Commonly, applications choose to mirror theuser-facing camera.                                                                                                                       |
| objectFit                      | string           | 'contain'           | Can be contain or cover                                                                                                | 
| streamURL                      | string           | ''                  | This is mandatory                                                                                                                      |
| zOrder                         | number           | 0                   | Similarly to zIndex                                                                                              |


### Custom APIs

#### registerGlobals()

By calling this method the JavaScript global namespace gets "polluted" with the following additions:

* `navigator.mediaDevices.getUserMedia()`
* `navigator.mediaDevices.enumerateDevices()`
* `window.RTCPeerConnection`
* `window.RTCIceCandidate`
* `window.RTCSessionDescription`
* `window.MediaStream`
* `window.MediaStreamTrack`

This is useful to make existing WebRTC JavaScript libraries (that expect those globals to exist) work with react-native-webrtc.


#### MediaStreamTrack.prototype._switchCamera()

This function allows to switch the front / back cameras in a video track
on the fly, without the need for adding / removing tracks or renegotiating.

#### VideoTrack.enabled

Starting with version 1.67, when setting a local video track's enabled state to
`false`, the camera will be closed, but the track will remain alive. Setting
it back to `true` will re-enable the camera.

## Related projects

### react-native-incall-manager

Use [react-native-incall-manager](https://github.com/react-native-webrtc/react-native-incall-manager) to keep screen on, mute microphone, etc.

### react-native-callkeep

Use [react-native-callkeep](https://github.com/react-native-webrtc/react-native-callkeep) to use callkit on iOS or connection service on Android to have native dialer with your webrtc application.

## Sponsorship
This repository doesn't have a plan to get sponsorship.(This can be discussed afterwards by collaborators). If you would like to pay bounty to fix some bugs or get some features, be free to open a issue that adds `[BOUNTY]` category in title. Add other bounty website link like [this](https://www.bountysource.com) will be better.

## Creator
This repository is originally created by [Wan Huang Yang](https://github.com/oney/).
