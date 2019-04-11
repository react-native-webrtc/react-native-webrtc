import React, { Component } from 'react';
import {
  Text,
  View,
  TouchableOpacity,
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
import s                    from './style';


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
      // Get all devices (video/audio) in array list
      const sourceInfos = await mediaDevices.enumerateDevices();
      console.log(sourceInfos);
      
      // Iterate the list above and find the front camera
      await Promise.all(sourceInfos.map(async sourceInfo => {
        console.log(sourceInfo);
        
        if (sourceInfo.kind === 'videoinput' && sourceInfo.label === 'Camera 1, Facing front, Orientation 270') {
          this.setState({ videoSourceId: sourceInfo.deviceId });
        }
      }));
      
      // Get the stream of front camera
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