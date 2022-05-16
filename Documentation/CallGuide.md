# Step By Step Call Guide

Before continuing please make sure you have followed the platform install guides and have a signalling server ready which can relay data to and from call participants.  

Generally speaking using Web Sockets is recommended due to low latency, speed and bandwidth usage.  
Be prepared to consider fallback mechanisms like a REST API which polls for messages within a set duration.  

You should also consider that network conditions won't always be great and can mostly be restrictive.  
Realtime Web Sockets might not function entirely or correctly in some circumstances.  

This guide will cover the general basic steps of how you can get a call connected between 2 clients and assumes that you have read our [basic usage doc](./BasicUsage.md) which lists most of the supported functions by this module. You should also check [this guide](./ImprovingCallReliability.md) as it covers why you should be using a STUN+TURN server to improve call reliability.  

## Step 1 - Prepare to call, grab media stream

This will give us a media stream for the front facing camera and input from a microphone.  
As you can see if we flip the `isVoiceOnly` boolean over to `true` then we'd be disabling the video track.  

If you want to start the call as voice only then you can flip the boolean but the catch is that you can enable the video track while the call is in progress, no messing around creating and adding another media stream or starting a new call.  

**Don't forget, you will be prompted to accept permissions for the camera and microphone, usually best to request them at an earlier stage for a better user experience.**  

```javascript
let mediaConstraints = {
	audio: true,
	video: {
		frameRate: 30,
		facingMode: 'user'
	}
};

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

## Step 2 - Create your peer, add the media stream



```javascript
let peerConstraints = {
	iceServers: [
		{
			url: 'stun:stun.l.google.com:19302'
		}
	]
};

let peerConnection = new RTCPeerConnection( peerConstraints );

peerConnection.addEventListener( 'connectionstatechange', event => {} );
peerConnection.addEventListener( 'icecandidate', event => {} );
peerConnection.addEventListener( 'icecandidateerror', event => {} );
peerConnection.addEventListener( 'iceconnectionstatechange', event => {} );
peerConnection.addEventListener( 'icegatheringstatechange', event => {} );
peerConnection.addEventListener( 'negotiationneeded', event => {} );
peerConnection.addEventListener( 'signalingstatechange', event => {} );
peerConnection.addEventListener( 'addstream', event => {} );
peerConnection.addEventListener( 'removestream', event => {} );

peerConnection.addStream( localMediaStream );
```

## Step 3 - Create an offer, set the local description



```javascript
let sessionConstraints = {
	mandatory: {
		OfferToReceiveAudio: true,
		OfferToReceiveVideo: true,
		VoiceActivityDetection: true
	}
};

try {
	const offerDescription = await peerConnection.createOffer( sessionConstraints );
	await peerConnection.setLocalDescription( offerDescription );
} catch( err ) {
	// Handle Errors
};
```

## Step 4 - Handling the ICE Candidates on both sides



```javascript

```

## Step 5 - Set the remote description, create an answer, set the local description



```javascript
try {
	let offerDescription = new RTCSessionDescription( offerDescription );
	await peerConnection.setRemoteDescription( offerDescription );

	const answerDescription = await peerConnection.createAnswer( sessionConstraints );
	await peerConnection.setLocalDescription( answerDescription );
} catch( err ) {
	// Handle Errors
};
```

## Step 6 - Set the remote description



```javascript
try {
	await peerConnection.setRemoteDescription( answerDescription );
} catch( err ) {
	// Handle Error
};
```

## Step 7 - 



```javascript

```

## Step 8 - 



```javascript

```

## Step 9 - 



```javascript

```

## Step 10 - 



```javascript

```