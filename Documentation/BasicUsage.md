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
	permissions,
	registerGlobals
} from 'react-native-webrtc';
```

## Defining Media Constraints

```javascript
var mediaConstraints = {
	audio: {
		googEchoCancellation: true,
		googAutoGainControl: true,
		googNoiseSuppression: true,
		googHighpassFilter: true,
		googEchoCancellation2: true,
		googAutoGainControl2: true,
		googNoiseSuppression2: true
	},
	video: {
		mandatory: {
			minFrameRate: '30'
		},
		facingMode: 'user',
		optional: []
	}
};
```

## Getting a Media Stream

```javascript
var localMediaStream;
var isVoiceOnly: boolean = false;

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

Promise based method.

```javascript
mediaDevices.getUserMedia( mediaConstraints ).then( function( mediaStream ) {
	if ( isVoiceOnly ) {
		let videoTrack = await mediaStream.getVideoTracks()[ 0 ];
		videoTrack.enabled = false;
	};

	localMediaStream = mediaStream;
} ).catch( function( err ) {
	// Handle Error
} );
```

## Defining Peer Constraints

```javascript
var peerConstraints = {
	iceServers: [
		{
			url: 'stun:stun.l.google.com:19302'
		}
	],
	iceTransportPolicy: 'all',
	bundlePolicy: 'balanced',
	rtcpMuxPolicy: 'require'
};
```

## Creating a Peer Connection

```javascript
var peerConnection = new RTCPeerConnection( peerConstraints );

peerConnection.onicecandidate = handleLocalCandidate;
peerConnection.onicecandidateerror = handleCandidateError;
peerConnection.oniceconnectionstatechange = handleICEConnectionChange;
peerConnection.onconnectionstatechange = handleConnectionStateChange;
peerConnection.onsignalingstatechange = handleSignalingChange;
peerConnection.onnegotiationneeded = handleNegotiation;
peerConnection.onaddstream = handleStreamAdded;
peerConnection.onremovestream = handleStreamRemoved;

peerConnection.addStream( localMediaStream );
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

## Dealing with ICE Candidates and Peer Events

```javascript
function handleLocalCandidate( iceEvent: any ) {
	// If we've reached the end, don't send anything.
	if ( !iceEvent.candidate ) { return; };

	// Send the ICE Candidate to the call recipient.
};

function handleCandidateError( err: any ) {
	// Handle Error
};

function handleICEConnectionChange( event: any ) {

};

function handleConnectionStateChange( event: any ) {

};

function handleSignalingChange( event: any ) {

};

function handleNegotiation() {

};

function handleStreamAdded( streamEvent: any ) {

};

function handleStreamRemoved( streamEvent: any ) {

};
```

## Defining Session Constraints

```javascript
var sessionConstraints = {
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
	// Create an offer.
	const offerDescription = await peerConnection.createOffer( sessionConstraints );
	await peerConnection.setLocalDescription( offerDescription );

	// Send the offerDescription to the other participant.
} catch( err ) {
	// Handle Errors
};
```

Promise based method.

```javascript
peerConnection.createOffer( sessionConstraints ).then( function( offerDescription ) {

	peerConnection.setLocalDescription( offerDescription ).then( function() {

	} ).catch( function( err ) {
		// Handle Error
	} );

} ).catch( function( err ) {
	// Handle Error
} );
```

## Creating an Answer

```javascript
try {
	// This is the received offerDescription.
	var offerDescription = new RTCSessionDescription( offerDescription );
	await peerConnection.setRemoteDescription( offerDescription );

	// Create an answer.
	const answerDescription = await peerConnection.createAnswer( sessionConstraints );
	await peerConnection.setLocalDescription( answerDescription );

	// Send the answerDescription back as a response to the offerDescription.
} catch( err ) {
	// Handle Errors
};
```

Promise based method.

```javascript
// This is the received offerDescription.
var offerDescription = new RTCSessionDescription( offerDescription );

peerConnection.setRemoteDescription( offerDescription ).then( function() {

	peerConnection.createAnswer( sessionConstraints ).then( function( answerDescription ) {

		peerConnection.setLocalDescription( answerDescription ).then( function() {

		} ).catch( function( err ) {
			// Handle Error
		} );

	} ).catch( function( err ) {
		// Handle Error
	} );

} ).catch( function( err ) {
	// Handle Error
} );
```

## Rendering a Media Stream

```javascript
<RTCView
	objectFit={'cover'}
	zOrder={0}
	mirror={true}
	streamURL={localStream}
/>
```
