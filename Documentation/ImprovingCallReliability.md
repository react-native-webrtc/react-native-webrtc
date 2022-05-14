# Improving Call Reliability

When talking about WebRTC in a general sense we're usually referring to the underlining technology which allows the creation of a peer to peer connection between multiple browsers/clients.  
Once a connection has been established you can transport data in many forms between all parties involved.  

Typically realtime video and voice data and even some basic data messages.  
You do on the other hand need help from a Signalling Server which transports Session Descriptions between all involved parties to actually get a peer to peer connection established.  

Session Descriptions contain all of the general information about each client device and connection details which can then be used to ensure clients are compatible and can connect together nicely.  
Now we need to consider what STUN and TURN servers are and why they're important parts of the handshaking process and how they can improve your call service quality.  

## What is a STUN server and why do i need it?



## What is a TURN server and why do i need it?



## How can i test that they work?

https://icetest.info/
https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/

## Can i setup and host my own?

https://github.com/coturn/coturn
