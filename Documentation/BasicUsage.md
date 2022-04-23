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
