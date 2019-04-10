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
| 1.69.1 | [M69](https://chromium.googlesource.com/external/webrtc/+/branch-heads/69)<br>[commit](https://chromium.googlesource.com/external/webrtc/+/9110a54a60d9e0c69128338fc250319ddb751b5a)<br>(24012)<br>(+16-24348) | x86_64<br>i386<br>armv7<br>arm64 | armeabi-v7a<br>arm64-v8a<br>x86<br>x86_64 | :heavy_check_mark: |  |  |
| 1.69.0 | [M69](https://chromium.googlesource.com/external/webrtc/+/branch-heads/69)<br>[commit](https://chromium.googlesource.com/external/webrtc/+/9110a54a60d9e0c69128338fc250319ddb751b5a)<br>(24012)<br>(+16-24348) | x86_64<br>i386<br>armv7<br>arm64 | armeabi-v7a<br>x86 | :heavy_check_mark: |  |  |
| master | [M69](https://chromium.googlesource.com/external/webrtc/+/branch-heads/69)<br>[commit](https://chromium.googlesource.com/external/webrtc/+/9110a54a60d9e0c69128338fc250319ddb751b5a)<br>(24012)<br>(+16-24348) | x86_64<br>i386<br>armv7<br>arm64 | armeabi-v7a<br>x86 | :warning: | test me plz |  |

Please see [wiki page](https://github.com/react-native-webrtc/react-native-webrtc/wiki) about revision history.

## Installation

- [iOS](https://github.com/react-native-webrtc/react-native-webrtc/blob/master/Documentation/iOSInstallation.md)
- [Android](https://github.com/react-native-webrtc/react-native-webrtc/blob/master/Documentation/AndroidInstallation.md)

## Usage
Now, you can use WebRTC like in browser.
In your `index.ios.js`/`index.android.js`, you can require WebRTC to import RTCPeerConnection, RTCSessionDescription, etc.

Anything about using RTCPeerConnection, RTCSessionDescription and RTCIceCandidate is like browser.
Support most WebRTC APIs, please see the [Document](https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection).

```Javascript
import React, { Component } from 'react';
import {
  Text,
  View,
  TouchableOpacity,
  StyleSheet,
}                           from 'react-native';
import {
  RTCPeerConnection,
  RTCIceCandidate,
  RTCSessionDescription,
  MediaStream,
  MediaStreamTrack,
  RTCView,
  mediaDevices,
}                           from 'react-native-webrtc';


const configuration = { "iceServers": [{ "url": "stun:stun.l.google.com:19302" }] };
const pc = new RTCPeerConnection(configuration);

pc.createOffer().then(desc => {
  pc.setLocalDescription(desc).then(() => {
    // Send pc.localDescription to peer
  });
});

pc.onicecandidate = function (event) {
  // send event.candidate to peer
};

// also support setRemoteDescription, createAnswer, addIceCandidate, onnegotiationneeded, oniceconnectionstatechange, onsignalingstatechange, onaddstream

export default class App extends Component {
  state = {
    stream: "",
    isFront: true,
    videoSourceId: null,
    mirror: false,
    objectFit: 'contain',
  };
  
  async componentDidMount() {
    await this.initStream();
  }
  
  initStream = async () => {
    try {
      const sourceInfos = await mediaDevices.enumerateDevices();
      console.log(sourceInfos);
      
      await Promise.all(sourceInfos.map(async sourceInfo => {
        console.log(sourceInfo);
        
        if (sourceInfo.kind === 'videoinput' && sourceInfo.label === 'Camera 1, Facing front, Orientation 270') {
          this.setState({ videoSourceId: sourceInfo.deviceId });
        }
      }));
      
      const stream = await mediaDevices.getUserMedia({
        audio: true,
        video: {
          mandatory: {
            minWidth: 500, // Provide your own width, height and frame rate here
            minHeight: 300,
            minFrameRate: 30,
          },
          facingMode: (this.state.isFront ? 'user' : 'environment'),
          optional: [{ sourceId: this.state.videoSourceId }],
        },
      });
      
      this.setState({ stream });
      console.log(stream);
    } catch (error) {console.log(error);}
  };
  
  switchCamera = async () => {
    this.setState({ isFront: !this.state.isFront });
    await this.initStream();
  };
  
  objectFit = () => {
    if (this.state.objectFit === 'contain') {
      this.setState({ objectFit: 'cover' });
    }
    if (this.state.objectFit === 'cover') {
      this.setState({ objectFit: 'contain' });
    }
  };
  
  button = (func, text) => (
    <TouchableOpacity style={s.button} onPress={func}>
      <Text style={s.buttonText}>{text}</Text>
    </TouchableOpacity>
  );
  
  render() {
    const { stream, mirror, objectFit } = this.state;
    
    return (
      <View style={s.container}>
        <RTCView
          style={s.rtcView}
          streamURL={stream.id}
          mirror={mirror}
          objectFit={objectFit}
        />
        {this.button(this.switchCamera, 'Change Camera')}
        {this.button(() => this.setState({ mirror: !mirror }), 'Mirror')}
        {this.button(this.objectFit, 'Object Fit (contain/cover)')}
      </View>
    );
  }
}

const s = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: "center",
    backgroundColor: "#F5FCFF",
  },
  rtcView: {
    flex: 1,
    width: '100%',
    marginTop: 10,
  },
  button: {
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'blue',
    borderRadius: 10,
    margin: 5,
    padding: 10,
  },
  buttonText: {
    fontSize: 20,
    color: 'white',
  },
});
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

### react-native-callkeep

Use [react-native-callkeep](https://github.com/wazo-pbx/react-native-callkeep) to use callkit on iOS or connection service on Android to have native dialer with your webrtc application.

## Sponsorship
This repository doesn't have a plan to get sponsorship.(This can be discussed afterwards by collaborators). If you would like to pay bounty to fix some bugs or get some features, be free to open a issue that adds `[BOUNTY]` category in title. Add other bounty website link like [this](https://www.bountysource.com) will be better.

## Creator
This repository is originally created by [Wan Huang Yang](https://github.com/oney/).
