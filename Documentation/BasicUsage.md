# Basic Usage

For starters we're going to import everything ready to use.  
Most of the included functionality is similar to how you would deal with WebRTC in your browser.  
We support a lot of the official WebRTC APIs, see this [document](https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection) for more details.  
If you see functions listed in the document linked above but not listed below then they are likely not supported by this module yet and will most likely be supported in the future.  

```javascript
import {
	ScreenCapturePickerView,
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

## Registering Globals

You'll only really need to use this function if you are mixing project development with libraries that use browser based WebRTC functions. Also applies if you are making your project compatible with react-native-web.  

```javascript
registerGlobals();
```

Here is a list of everything that will be linked up.  
You can also find a shim for react-native-web over [here](https://github.com/react-native-webrtc/react-native-webrtc-web-shim).

```javascript
navigator.mediaDevices.getUserMedia()
navigator.mediaDevices.getDisplayMedia()
navigator.mediaDevices.enumerateDevices()
window.RTCPeerConnection
window.RTCIceCandidate
window.RTCSessionDescription
window.MediaStream
window.MediaStreamTrack
```

## Get Available Media Devices

As weird as it can be, some devices might not have more than 1 camera. The following will allow you to know how many cameras the device has. You can ofcourse use `enumerateDevices` to list other media device information.  

```javascript
let cameraCount = 0;

try {
	const devices = await mediaDevices.enumerateDevices();

	devices.map( device => {
		if ( device.kind != 'videoinput' ) { return; };

		cameraCount = cameraCount + 1;
	} );
} catch( err ) {
	// Handle Error
};
```

## Defining Media Constraints

By default we're sending both audio and video.  
This will allow us to toggle the video stream during a call.  

```javascript
let mediaConstraints = {
	audio: true,
	video: {
		frameRate: 30,
		facingMode: 'user'
	}
};
```

## Getting a Media Stream using getUserMedia

If you only want a voice call then you can flip `isVoiceOnly` over to `true`.  
You can then cycle and enable or disable the video tracks on demand during a call.  

```javascript
let localMediaStream;
let isVoiceOnly = false;

try {
	const mediaStream = await mediaDevices.getUserMedia( mediaConstraints );

	if ( isVoiceOnly ) {
		let videoTrack = await mediaStream.getVideoTracks()[ 0 ];
		videoTrack.enabled = false;
	};

	localMediaStream = mediaStream;
} catch( err ) {
	// Handle Error
};
```

## Getting a Media Stream using getDisplayMedia

This will allow you to capture the device screen.  
You will run into issues with Android 10+ due to extra requirements.  

```javascript
try {
	const mediaStream = await mediaDevices.getDisplayMedia();

	localMediaStream = mediaStream;
} catch( err ) {
	// Handle Error
};
```

## Destroying the Media Stream

Cycling all of the tracks and stopping them is more than enough to clean up after a call has finished.  
You won't need to do this for remote tracks, only local.  

```javascript
localMediaStream.getTracks().map(
	track => track.stop()
);

localMediaStream = null;
```

## Defining Peer Constraints

We're only specifying a STUN server but you should look at also using a TURN server.  
If you want to improve call reliability then check [this guide](./ImprovingCallReliability.md).  

```javascript
let peerConstraints = {
	iceServers: [
		{
			url: 'stun:stun.l.google.com:19302'
		}
	]
};
```

## Creating a Peer Connection

```javascript
let peerConnection = new RTCPeerConnection( peerConstraints );

peerConnection.onicecandidate = function( event ) {};
peerConnection.onicecandidateerror = function( err ) {};
peerConnection.oniceconnectionstatechange = function() {};
peerConnection.onconnectionstatechange = function( event ) {};
peerConnection.onsignalingstatechange = function( event ) {};
peerConnection.onnegotiationneeded = function() {};
peerConnection.onaddstream = function( event ) {};
peerConnection.onremovestream = function( event ) {};
```

## Destroying the Peer Connection

When ending a call you should always make sure to dispose of everything ready for another call. You don't necessarily need to blank out the events, usually you can get away with the last 3 lines as that will kill everything, double standards.  

```javascript
peerConnection.onicecandidate = null;
peerConnection.onicecandidateerror = null;
peerConnection.oniceconnectionstatechange = null;
peerConnection.onconnectionstatechange = null;
peerConnection.onsignalingstatechange = null;
peerConnection.onnegotiationneeded = null;
peerConnection.onaddstream = null;
peerConnection.onremovestream = null;

peerConnection._unregisterEvents();
peerConnection.close();
peerConnection = null;
```

## Adding the Media Stream

By adding a media stream to the peer you will trigger negotiations and ice candidate generation.  
Make sure you are handling ice candidates correctly or you won't get a call connected.  

```javascript
peerConnection.addStream( localMediaStream );
```

## Creating a Data Channel

```javascript

```

## Adding the Data Channel

```javascript

```

## Destroying the Data Channel

```javascript

```

## Handling Ice Candidates

```javascript

```

## Defining Session Constraints

```javascript
let sessionConstraints = {
	mandatory: {
		OfferToReceiveAudio: true,
		OfferToReceiveVideo: true,
		VoiceActivityDetection: true
	}
};
```

## Creating an Offer

```javascript
try {
	const offerDescription = await peerConnection.createOffer( sessionConstraints );
	await peerConnection.setLocalDescription( offerDescription );

	// Send the offerDescription to the other participant.
} catch( err ) {
	// Handle Errors
};
```

## Creating an Answer

```javascript
try {
	// Use the received offerDescription
	let offerDescription = new RTCSessionDescription( offerDescription );
	await peerConnection.setRemoteDescription( offerDescription );

	const answerDescription = await peerConnection.createAnswer( sessionConstraints );
	await peerConnection.setLocalDescription( answerDescription );

	// Send the answerDescription back as a response to the offerDescription.
} catch( err ) {
	// Handle Errors
};
```

## Flipping the Active Camera

Naturally we assume you'd be using the front camera by default when starting a call.  
So we set `isFrontCam` as `true` and let the value flip.  

```javascript
let isFrontCam = true;

try {
	// Taken from above, we don't want to flip if we don't have another camera.
	if ( cameraCount < 2 ) { return; };

	const videoTrack = await localMediaStream.getVideoTracks()[ 0 ];
	videoTrack._switchCamera();

	isFrontCam = !isFrontCam;
} catch( err ) {
	// Handle Error
};
```

## Screen Capture Picker - iOS?

```javascript
<ScreenCapturePickerView />
```

## Media Stream Rendering

```javascript
<RTCView
	mirror={true}
	objectFit={'cover'}
	streamURL={localStream}
	zOrder={0}
/>
```

| Param | Type | Default | Description |
| :----- | :---- | :------- | :----------- |
| mirror | boolean | false | Indicates whether the video specified by `streamURL` should be mirrored.  Usually you'd mirror the user facing self preview camera. |
| objectFit | string | 'contain' | Can be `'contain'` or `'cover'` nothing more or less. | 
| streamURL | string | 'streamurl' | Requred to have an actual video stream rendering. |
| zOrder | number | 0 | Similar to zIndex. |