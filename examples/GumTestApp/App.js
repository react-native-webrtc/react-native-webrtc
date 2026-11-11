/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow strict-local
 */

import {useState, useRef} from 'react';
import {Button, SafeAreaView, StyleSheet, View, StatusBar} from 'react-native';
import {Colors} from 'react-native/Libraries/NewAppScreen';
import {
  RTCPeerConnection,
  RTCIceCandidate,
  mediaDevices,
  RTCView,
} from 'react-native-webrtc';

const WebRTCIceServers = {
  iceServers: [
    {urls: 'stun:stun.l.google.com:19302'},
    {urls: 'stun:stun1.l.google.com:19302'},
    {urls: 'stun:stun2.l.google.com:19302'},
    {urls: 'stun:stun3.l.google.com:19302'},
  ],
};

const WebRTCSessionConstraints = {
  mandatory: {
    OfferToReceiveAudio: true,
    OfferToReceiveVideo: true,
    VoiceActivityDetection: true,
  },
};

const App = () => {
  const ref = useRef(null);
  const [localStream, setLocalStream] = useState(null);
  const [remoteStream, setRemoteStream] = useState(null);

  const start = async () => {
    const stream = await getUserStream();

    setLocalStream(stream);

    const pc1 = new RTCPeerConnection(WebRTCIceServers);

    stream?.getTracks().forEach((track) => {
      pc1.addTrack(track, stream);
    });

    const pc2 = new RTCPeerConnection(WebRTCIceServers);

    pc1.addEventListener('signalingstatechange', () =>
      console.info('signalingstatechange pc1', pc1.signalingState),
    );
    pc2.addEventListener('signalingstatechange', () =>
      console.info('signalingstatechange pc2', pc2.signalingState),
    );

    pc1.addEventListener('connectionstatechange', () =>
      console.info('connectionstatechange pc1', pc1.connectionState),
    );
    pc2.addEventListener('connectionstatechange', () =>
      console.info('connectionstatechange pc2', pc2.connectionState),
    );

    pc1.addEventListener('iceconnectionstatechange', () => {
      console.info('iceconnectionstatechange pc1', pc1.iceConnectionState);
      if (pc1.iceConnectionState === 'failed') {
        pc1.close();
        pc2.close();
        localStream?.getTracks().forEach((track) => track.stop());
        remoteStream?.getTracks().forEach((track) => track.stop());
        setLocalStream(null);
        setRemoteStream(null);
      }
    });
    pc2.addEventListener('iceconnectionstatechange', () => {
      console.info('iceconnectionstatechange pc2', pc2.iceConnectionState);
      if (pc1.iceConnectionState === 'failed') {
        pc1.close();
        pc2.close();
        localStream?.getTracks().forEach((track) => track.stop());
        remoteStream?.getTracks().forEach((track) => track.stop());
        setLocalStream(null);
        setRemoteStream(null);
      }
    });

    pc1.addEventListener('icecandidate', (event) => {
      const candidate = event.candidate;
      if (!candidate) return;
      pc2.addIceCandidate(new RTCIceCandidate(candidate));
    });

    pc2.addEventListener('icecandidate', (event) => {
      const candidate = event.candidate;
      if (!candidate) return;
      pc1.addIceCandidate(new RTCIceCandidate(candidate));
    });

    pc2.addEventListener('track', (event) => {
      const stream = event.streams[0];
      if (!stream) return;
      setRemoteStream(stream);
    });

    const offer = await pc1.createOffer(WebRTCSessionConstraints);
    await pc1.setLocalDescription(offer);

    await pc2.setRemoteDescription(offer);
    const answer = await pc2.createAnswer();
    await pc2.setLocalDescription(answer);

    await pc1.setRemoteDescription(answer);
  };

  const getUserStream = async () => {
    const stream = await mediaDevices.getUserMedia({video: true});
    return stream;
  };

  const startPIP = () => {
    ref.current?.startPictureInPicture();
  };
  const stopPIP = () => {
    ref.current?.stopPictureInPicture();
  };

  const stop = () => {
    console.log('stop');
    localStream?.release();
    localStream?.getTracks().forEach((track) => track.stop());
    remoteStream?.release();
    remoteStream?.getTracks().forEach((track) => track.stop());
    setLocalStream(null);
    setRemoteStream(null);
  };

  return (
    <>
      <StatusBar barStyle="dark-content" />
      <SafeAreaView style={styles.body}>
        <View style={styles.content}>
          {remoteStream && (
            <RTCView
              ref={ref}
              streamURL={remoteStream.toURL()}
              style={styles.remoteStream}
              objectFit="cover"
              onPictureInPictureChange={(isInPictureInPicture) =>
                console.log({isInPictureInPicture})
              }
              pictureInPictureEnabled={true}
              autoStartPictureInPicture={true}
              pictureInPicturePreferredSize={{
                width: 150,
                height: 190,
              }}>
              <View
                style={{width: 100, height: 100, backgroundColor: 'blue'}}
              />
            </RTCView>
          )}

          {remoteStream && localStream && (
            <RTCView
              streamURL={localStream.toURL()}
              style={styles.localStream}
              objectFit="cover"
              zOrder={1}
              mirror
            />
          )}
        </View>

        <View style={styles.footer}>
          <Button title="Start" onPress={start} />
          <Button title="Start PIP" onPress={startPIP} />
          <Button title="Stop PIP" onPress={stopPIP} />
          <Button title="Stop" onPress={stop} />
        </View>
      </SafeAreaView>
    </>
  );
};

const styles = StyleSheet.create({
  body: {
    backgroundColor: Colors.white,
    ...StyleSheet.absoluteFill,
  },
  content: {
    flex: 1,
    width: '100%',
    justifyContent: 'flex-end',
    alignItems: 'flex-end',
  },
  remoteStream: {
    flex: 1,
    width: '100%',
  },
  localStream: {
    position: 'absolute',
    height: 200,
    width: 150,
    bottom: 5,
    right: 5,
  },
  footer: {
    backgroundColor: Colors.lighter,
  },
});

export default App;
