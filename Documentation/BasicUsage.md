# Basic Usage

For starters, we're going to import everything ready to use.  
Most of the included functionality is similar to how you would deal with WebRTC in your browser.  
We support a lot of the official WebRTC APIs, see this [document](https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection) for more details.  
If you see functions that are listed in the document above but not listed below then they are likely not supported by this module yet and will most likely be supported in the near future, we're open to contributions.

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

`navigator.mediaDevices.getUserMedia()`  
`navigator.mediaDevices.getDisplayMedia()`  
`navigator.mediaDevices.enumerateDevices()`  
`window.RTCPeerConnection`  
`window.RTCIceCandidate`  
`window.RTCSessionDescription`  
`window.MediaStream`  
`window.MediaStreamTrack`  

## Get Available Media Devices

Some devices might not have more than 1 camera. The following will allow you to know how many cameras the device has. You can use `enumerateDevices` to list other media device information too.  

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

This will allow capturing the device screen, also requests permission on execution.  
Android 10+ requires that a foreground service is running otherwise capturing won't work, follow [this solution](./AndroidInstallation.md#screen-capture-support---android-10).  

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
You usually won't need to do this for remote tracks, only local.  

```javascript
localMediaStream.getTracks().forEach(
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
			urls: 'stun:stun.l.google.com:19302'
		}
	]
};
```

## Creating a Peer Connection

Here, we're creating a peer connection required to get a call started.  
You can also hook up events by directly overwriting functions instead of using event listeners.  

```javascript
let peerConnection = new RTCPeerConnection( peerConstraints );

peerConnection.addEventListener( 'connectionstatechange', event => {} );
peerConnection.addEventListener( 'icecandidate', event => {} );
peerConnection.addEventListener( 'icecandidateerror', event => {} );
peerConnection.addEventListener( 'iceconnectionstatechange', event => {} );
peerConnection.addEventListener( 'icegatheringstatechange', event => {} );
peerConnection.addEventListener( 'negotiationneeded', event => {} );
peerConnection.addEventListener( 'signalingstatechange', event => {} );
peerConnection.addEventListener( 'track', event => {} );
```

## Destroying the Peer Connection

When ending a call you should always make sure to dispose of everything ready for another call.  
The following should dispose of everything related to the peer connection.  

```javascript
peerConnection.close();
peerConnection = null;
```

## Adding the Media Stream

After using one of the media functions above, you can then add the media stream to the peer.  
The negotiation needed event will be triggered on the peer connection afterwards.  

```javascript
localMediaStream.getTracks().forEach( 
	track => peerConnection.addTrack( track, localMediaStream );
);
```

## Creating a Data Channel

Usually the call initialiser would create the data channel but it can be done on both sides.  
The negotiation needed event will be triggered on the peer connection afterwords.  

```javascript
let datachannel = peerConnection.createDataChannel( 'my_channel' );

datachannel.addEventListener( 'open', event => {} );
datachannel.addEventListener( 'close', event => {} );
datachannel.addEventListener( 'message', message => {} );
```

## Handling Data Channels

The following event is for the second client, not the client which created the data channel.  
Unless you want both sides to create separate data channels.  

```javascript
peerConnection.addEventListener( 'datachannel', event => {
	let datachannel = event.channel;

	// Now you've got the datachannel.
	// You can hookup and use the same events as above ^
} );
```

## Sending a Message via the Data Channel

You can send a range of different data types over data channels, we're going to send a simple string.  
Bear in mind that there are limits to sending large amounts of data which isn't usually advised.  

```javascript
datachannel.send( 'Hey There!' );
```

## Destroying the Data Channel

When the peer connection is destroyed, data channels should also be destroyed automatically.  
But as good practice, you can always close them yourself.  

```javascript
datachannel.close();
datachannel = null;
```

## Defining Session Constraints

As mentioned above, by default we're going for the approach of offering both video and voice.  
That will allow you to enable and disable video streams on demand while a call is active.  

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

Executed by the call initialiser after media streams have been added to the peer connection.  
ICE Candidate creation and gathering will start as soon as an offer has been created.  

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

All parties must ensure the proper handling of ICE Candidates.
Otherwise, the offer-answer handshake stage might encounter some unexpected behavior.

```javascript
try {
	// Use the received offerDescription
	const offerDescription = new RTCSessionDescription( offerDescription );
	await peerConnection.setRemoteDescription( offerDescription );

	const answerDescription = await peerConnection.createAnswer();
	await peerConnection.setLocalDescription( answerDescription );

	// Send the answerDescription back as a response to the offerDescription.
} catch( err ) {
	// Handle Errors
};
```

## Toggle the Active Microphone

While engaging in a live call, you may find it necessary to mute your microphone. 
This can be easily achieved by toggling the track enabled value to false.
This functionality is also applicable to remote tracks.

```javascript
let isMuted = false;

try {
	const audioTrack = await localMediaStream.getAudioTracks()[ 0 ];
	audioTrack.enabled = !audioTrack.enabled;

	isMuted = !isMuted;
} catch( err ) {
	// Handle Error
};
```

## Switching the Active Camera

Naturally, we assume you'll be using the front camera by default when starting a call.  
So we set `isFrontCam` as `true` and let the value flip on execution.  

```javascript
let isFrontCam = true;

try {
	// Taken from above, we don't want to flip if we don't have another camera.
	if ( cameraCount < 2 ) { return; };

	const videoTrack = localMediaStream.getVideoTracks()[0];
	const constraints = { facingMode: isFrontCam ? 'user' : 'environment' };

	videoTrack.applyConstraints(constraints);

	// _switchCamera is deprecated as of 124.0.5
	// videoTrack._switchCamera();

	isFrontCam = !isFrontCam;
} catch( err ) {
	// Handle Error
};
```

## Rendering the Media Stream

Once you've gained a local and/or remote stream then rendering it is as follows.  
Don't forget, the user facing camera is usually mirrored.  

```javascript
<RTCView
	mirror={true}
	objectFit={'cover'}
	streamURL={localMediaStream.toURL()}
	zOrder={0}
/>
```

| Param | Type | Default | Description |
| :----- | :---- | :------- | :----------- |
| mirror | boolean | false | Indicates whether the video specified by `streamURL` should be mirrored. |
| objectFit | string | 'contain' | Can be `'contain'` or `'cover'` nothing more or less. | 
| streamURL | string | 'streamURL' | Required to have an actual video stream rendering. |
| zOrder | number | 0 | Similar to zIndex. |

## Controlling remote audio tracks

Remote audio tracks are automatically handled and played through your audio output.
The volume of individual tracks can be set with the `_setVolume` function. 
It takes in a number between 0 to 10, defaults to 1.

```javascript
const audioTrack = remoteMediaStream.getAudioTracks()[0];
audioTrack._setVolume(0.5);
```