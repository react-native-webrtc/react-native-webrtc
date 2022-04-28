# Basic Usage

For starters we're going to import everything ready to use.  
Most of the included functionality is similar to how you would deal with WebRTC in your browser.  
We support most of the official WebRTC APIs, see this [document](https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection) for more details.

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

```javascript
registerGlobals();
```

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

## Defining Media Constraints

```javascript
let mediaConstraints = {
	audio: true,
	video: {
		frameRate: 30,
		facingMode: 'user'
	}
};
```

## Getting a Media Stream

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

## Destroying a Media Stream

```javascript
localMediaStream.getTracks().map(
	track => track.stop()
);

localMediaStream = null;
```

## Defining Peer Constraints

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

peerConnection.onicecandidate = handleLocalCandidate;
peerConnection.onicecandidateerror = handleCandidateError;
peerConnection.oniceconnectionstatechange = handleICEConnectionStateChange;
peerConnection.onconnectionstatechange = handleConnectionStateChange;
peerConnection.onsignalingstatechange = handleSignalingStateChange;
peerConnection.onnegotiationneeded = handleNegotiation;
peerConnection.onaddstream = handleStreamAdded;
peerConnection.onremovestream = handleStreamRemoved;
```

## Destroying a Peer Connection

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

## Adding a Data Channel

```javascript

```

## Adding a Media Stream

```javascript
peerConnection.addStream( localMediaStream );
```

## Dealing with ICE Candidates and Peer Events

```javascript
function handleLocalCandidate( event ) {
	// If we've reached the end, don't send anything.
	if ( !event.candidate ) { return; };

	// Send the ICE Candidate to the call recipient.
};

function handleCandidateError( err ) {
	// Handle Error
};

function handleICEConnectionStateChange( event ) {

};

function handleConnectionStateChange( event ) {

};

function handleSignalingStateChange( event ) {

};

function handleNegotiation() {

};

function handleStreamAdded( event ) {

};

function handleStreamRemoved( event ) {

};
```

## Defining Session Constraints

```javascript
let sessionConstraints = {
	mandatory: {
		OfferToReceiveAudio: true,
		OfferToReceiveVideo: true,
		VoiceActivityDetection: true
	},
	optional: [
		{
			DtlsSrtpKeyAgreement: true
		}
	]
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
	const offerDescription = new RTCSessionDescription( offerDescription );
	await peerConnection.setRemoteDescription( offerDescription );

	const answerDescription = await peerConnection.createAnswer( sessionConstraints );
	await peerConnection.setLocalDescription( answerDescription );

	// Send the answerDescription back as a response to the offerDescription.
} catch( err ) {
	// Handle Errors
};
```

## Rendering a Media Stream

```javascript
<RTCView
	mirror={true}
	objectFit={'cover'}
	streamURL={localStream}
	zOrder={0}
/>
```

|  | | Type | | Default | | Description |
| - | - | :---- | - | :------- | - | :----------- |
| mirror | | boolean | | false | | Indicates whether the video specified by `streamURL` should be mirrored.  Usually you'd mirror the user facing self preview camera. |
| objectFit | | string | | 'contain' | | Can be `'contain'` or `'cover'` nothing more or less. | 
| streamURL | | string | | 'streamurl' | | Requred to have an actual video stream rendering. |
| zOrder | | number | | 0 | | Similar to zIndex. |