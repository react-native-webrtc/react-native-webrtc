# Step By Step Call Guide

Before continuing please make sure you have followed the platform install guides and have a working signalling server which can relay data to and from call participants.  

Generally speaking using Web Sockets is recommended due to low latency, speed and bandwidth usage but be prepared to consider fallback mechanisms like a REST API which usually polls for messages within a set duration.  

You should also consider that network conditions won't always be great and can mostly be restrictive.  
Realtime Web Sockets might not function entirely or correctly in some circumstances.  

This guide covers the general basic steps of how you can get a call connected between 2 clients and assumes that you have read our [basic usage doc](./BasicUsage.md) which lists most of the supported functions by this module.  
You should also check [this guide](./ImprovingCallReliability.md) as it covers why you should be using a STUN+TURN server to improve call reliability.  

## Step 1 - Prepare to call, grab media stream

This will give us a media stream for the front facing camera and input from a microphone.  
As you can see if we flip the `isVoiceOnly` boolean over to `true` then we'd be disabling the video track.  

If you want to start the call as voice only then you can flip the boolean but the catch is that you can enable the video track while the call is in progress, no messing around creating and adding another media stream or starting a new call.  

**Don't forget, you will be prompted to accept permissions for the camera and microphone.**  
**Usually it is better to request permissions at an earlier stage to improve the user experience.**  

```javascript
let mediaConstraints = {
	audio: true,
	video: {
		frameRate: 30,
		facingMode: 'user'
	}
};

let localMediaStream;
let remoteMediaStream;
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

Now that we've got the media stream which consists of an audio and video track we can actually start getting the peer connection created and ready to connect. Once the media stream has been added to the peer then the `negotiationneeded` event will fire to indicate that you can now start creating an offer.  

In some instances you might find that the `negotiationneeded` event can fire multiple times at random and as such if you decide to run the `createOffer` function within that event then you need to ensure that you don't allow running `createOffer` multiple times, otherwise the peer will most likely get stuck in a weird state.  

The `iceServers` below include one of Googles public STUN servers, you should also provide your own TURN server alongside a STUN server to ensure that connections can actually be established between callers.  

```javascript
let peerConstraints = {
	iceServers: [
		{
			urls: 'stun:stun.l.google.com:19302'
		}
	]
};

let peerConnection = new RTCPeerConnection( peerConstraints );

peerConnection.addEventListener( 'connectionstatechange', event => {
	switch( peerConnection.connectionState ) {
		case 'closed':
			// You can handle the call being disconnected here.

			break;
	};
} );

peerConnection.addEventListener( 'icecandidate', event => {
	// When you find a null candidate then there are no more candidates.
	// Gathering of candidates has finished.
	if ( !event.candidate ) { return; };

	// Send the event.candidate onto the person you're calling.
	// Keeping to Trickle ICE Standards, you should send the candidates immediately.
} );

peerConnection.addEventListener( 'icecandidateerror', event => {
	// You can ignore some candidate errors.
	// Connections can still be made even when errors occur.
} );

peerConnection.addEventListener( 'iceconnectionstatechange', event => {
	switch( peerConnection.iceConnectionState ) {
		case 'connected':
		case 'completed':
			// You can handle the call being connected here.
			// Like setting the video streams to visible.

			break;
	};
} );

peerConnection.addEventListener( 'negotiationneeded', event => {
	// You can start the offer stages here.
	// Be careful as this event can be called multiple times.
} );

peerConnection.addEventListener( 'signalingstatechange', event => {
	switch( peerConnection.signalingState ) {
		case 'closed':
			// You can handle the call being disconnected here.

			break;
	};
} );

peerConnection.addEventListener( 'track', event => {
	// Grab the remote track from the connected participant.
	remoteMediaStream = remoteMediaStream || new MediaStream();
	remoteMediaStream.addTrack( event.track, remoteMediaStream );
} );

// Add our stream to the peer connection.
localMediaStream.getTracks().forEach( 
	track => peerConnection.addTrack( track, localMediaStream );
);
```

## Step 3 - Signal that you're starting a call

Now would be a good time to officially announce the incoming call to other call participant.  
Using your signalling server you will need to trigger a call start event.  

Wether you trigger the event via Notifications or Web Sockets, the purpose of the event is to ensure that the call participant runs the code above ^ they will then be prepared to start the handshake process.  

We can't give any sample code for the signalling stages.  
But do intend to provide an example app along with signalling app in the near future.  

## Step 4 - Create an offer, set the local description

We've added the media stream to the peer connection and got most of the basic events hooked up.  
So you can now start creating an offer which then needs sending send off to the other call participant.  

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

	// Send the offerDescription to the other participant.
} catch( err ) {
	// Handle Errors
};
```

## Step 5 - Handling the ICE Candidates on both sides

As specified in the `icecandidate` event above, you should send candidates as soon as they are generated.  
You can trigger the following `handleRemoteCandidate` function to handle the received candidates on both sides.  

In some circumstances candidates can't be immediately processed.  
An easy solution is to hold onto some of the candidates and process them immediately after.  

We will process any leftover candidates in the next step.  

```javascript
let remoteCandidates = [];

function handleRemoteCandidate( iceCandidate ) {
	iceCandidate = new RTCIceCandidate( iceCandidate );

	if ( peerConnection.remoteDescription == null ) {
		return remoteCandidates.push( iceCandidate );
	};

	return peerConnection.addIceCandidate( iceCandidate );
};

function processCandidates() {
	if ( remoteCandidates.length < 1 ) { return; };

	remoteCandidates.map( candidate => peerConnection.addIceCandidate( candidate ) );
	remoteCandidates = [];
};
```

## Step 6 - Set the remote description, create an answer, set the local description

Now that you've received an offer we can create the compatible answer.  
Set the offer as the remote description, create an answer and set the local description as your answer.  

You can now process any candidates if any got caught inbetween the handshake process.  
Lastly you need to send the answer back to the caller who gave the offer.  

```javascript
try {
	// Use the received offerDescription
	const offerDescription = new RTCSessionDescription( offerDescription );
	await peerConnection.setRemoteDescription( offerDescription );

	const answerDescription = await peerConnection.createAnswer();
	await peerConnection.setLocalDescription( answerDescription );

	// Here is a good place to process candidates.
	processCandidates();

	// Send the answerDescription back as a response to the offerDescription.
} catch( err ) {
	// Handle Errors
};
```

## Step 7 - Set the remote description

To finalise the handshake mechanism the call initialiser sets the received answer as their remote description.  
Then the waiting game begins and the connection is a success or a failure.  
Hopefully the whole process wasn't too complex to understand.  
But it definitely can get more complex when involving more participants.  

```javascript
try {
	// Use the received answerDescription
	const answerDescription = new RTCSessionDescription( answerDescription );
	await peerConnection.setRemoteDescription( answerDescription );
} catch( err ) {
	// Handle Error
};
```

## Step 8 - You should now be connected

Hopefully everything went as expected and the call is now connected.  
If the `iceconnectionstatechange` event triggered on `connected` and/or `completed` then you're good to go.  
You might want to bare in mind that some of them events can be triggered multiple times.  
If you're having issues after following the guide then feel free to create a post on our [Discourse](https://react-native-webrtc.discourse.group/).  