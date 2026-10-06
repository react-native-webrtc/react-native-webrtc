/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow strict-local
 */

import React, {useState, useRef} from 'react';
import {
  Button,
  SafeAreaView,
  StyleSheet,
  View,
  StatusBar,
  Platform,
} from 'react-native';
import { Colors } from 'react-native/Libraries/NewAppScreen';
import { 
  mediaDevices, 
  startIOSPIP, 
  stopIOSPIP, 
  RTCPIPView, 
  ScreenCapturePickerView,
} from 'react-native-webrtc';
import { findNodeHandle, NativeModules } from 'react-native';


const App = () => {
  const view = useRef()
  const screenCapturePicker = useRef(null);
  const [stream, setStream] = useState(null);
  const startCamera = async () => {
    console.log('startCamera');
    if (!stream) {
      try {
        const s = await mediaDevices.getUserMedia({ video: true });
        setStream(s);
      } catch(e) {
        console.error(e);
      }
    }
  };
  const startScreenShare = async () => {
    console.log('startScreenShare');
    if (!stream) {
      try {
        if (Platform.OS === 'ios') {
          const reactTag = findNodeHandle(screenCapturePicker.current);
          await NativeModules.ScreenCapturePickerViewManager.show(reactTag);
        }
    
        const s = await mediaDevices.getDisplayMedia();
        setStream(s);
      } catch(e) {
        console.error(e);
      }
    }
  };
  const startPIP = () => {
    startIOSPIP(view);
  };
  const stopPIP = () => {
    stopIOSPIP(view);
  };
  const stop = () => {
    console.log('stop');
    if (stream) {
      stream.release();
      setStream(null);
    }
  };
  let pipOptions = {
    startAutomatically: true,
    fallbackView: (<View style={{ height: 50, width: 50, backgroundColor: 'red' }} />),
    preferredSize: {
      width: 400,
      height: 800,
    }
  }
  return (
    <>
      <StatusBar barStyle="dark-content" />
      <SafeAreaView style={styles.body}>
      {
        stream &&
        <RTCPIPView
            ref={view}
            streamURL={stream.toURL()}
            style={styles.stream}
            iosPIP={pipOptions} >
        </RTCPIPView>
      }
        <View
          style={styles.footer}>
          <Button
            title = "Start Camera"
            onPress = {startCamera} />
          <Button
            title = "Start Screen Share"
            onPress = {startScreenShare} />
          <Button
            title = "Start PIP"
            onPress = {startPIP} />
          <Button
            title = "Stop PIP"
            onPress = {stopPIP} />
          <Button
            title = "Stop"
            onPress = {stop} />
        </View>
        {Platform.OS === 'ios' && (
          <ScreenCapturePickerView ref={screenCapturePicker} />
        )}
      </SafeAreaView>
    </>
  );
};

const styles = StyleSheet.create({
  body: {
    backgroundColor: Colors.white,
    ...StyleSheet.absoluteFill
  },
  stream: {
    flex: 1
  },
  footer: {
    backgroundColor: Colors.lighter,
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0
  },
});

export default App;
