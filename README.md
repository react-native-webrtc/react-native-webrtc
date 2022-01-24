# react-native-webrtc

[![npm version](https://badge.fury.io/js/react-native-webrtc.svg)](https://badge.fury.io/js/react-native-webrtc)
[![npm downloads](https://img.shields.io/npm/dm/react-native-webrtc.svg?maxAge=2592000)](https://img.shields.io/npm/dm/react-native-webrtc.svg?maxAge=2592000)

A WebRTC module for React Native.
- Support iOS / macOS / Android.
- Support Video / Audio / Data Channels.

> 🚨 Expo: This package is not available in the [Expo Go](https://expo.dev/client) app. Learn how you can use this package in [Custom Dev Clients](https://docs.expo.dev/development/getting-started/) via the out-of-tree [Expo Config Plugin](https://github.com/expo/config-plugins/tree/master/packages/react-native-webrtc).

## Community

Everyone is welcome to our [Discourse community](https://react-native-webrtc.discourse.group/) to discuss any React Native and WebRTC related topics.

## WebRTC Revision

* Currently used revision: [M94](https://github.com/jitsi/webrtc/releases/tag/v94.0.0)
* Supported architectures
  * Android: armeabi-v7a, arm64-v8a, x86, x86_64
  * iOS: arm64, x86_64 (for bitcode support, run [this script](https://github.com/react-native-webrtc/react-native-webrtc/blob/master/tools/downloadBitcode.sh))
  * macOS: x86_64

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
      width: 640,
      height: 480,
      frameRate: 30,
      facingMode: (isFront ? "user" : "environment"),
      deviceId: videoSourceId
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
* `navigator.mediaDevices.getDisplayMedia()`
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

The [react-native-webrtc](https://github.com/react-native-webrtc) organization provides a number of packages which are useful when developing Real Time Communications applications.

The [react-native-webrtc-web-shim](https://github.com/react-native-webrtc/react-native-webrtc-web-shim) project provides a shim for react-native-web support, allowing you to use [(almost)](https://github.com/react-native-webrtc/react-native-webrtc-web-shim/tree/main#setup) the same code in react-native-web as in react-native.

## Acknowledgements

Thanks to all [contributors](https://github.com/react-native-webrtc/react-native-webrtc/graphs/contributors) for helping with the project!

Special thanks to [Wan Huang Yang](https://github.com/oney/) for creating the first version of this package.
