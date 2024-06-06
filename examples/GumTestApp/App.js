/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow strict-local
 */

import React, {useState} from 'react';
import {
  Button,
  SafeAreaView,
  StyleSheet,
  View,
  StatusBar,
} from 'react-native';
import { Colors } from 'react-native/Libraries/NewAppScreen';
import { mediaDevices, RTCView } from 'react-native-webrtc';


const App = () => {
  const [stream, setStream] = useState(null);
  const start = async () => {
    console.log('start');
    if (!stream) {
      try {
        const s = await mediaDevices.getUserMedia({ video: true });
        setStream(s);
      } catch(e) {
        console.error(e);
      }
    }
  };
  const stop = () => {
    console.log('stop');
    if (stream) {
      stream.release();
      setStream(null);
    }
  };
  return (
    <>
      <StatusBar barStyle="dark-content" />
      <SafeAreaView style={styles.body}>
      {
        stream &&
          <RTCView
            streamURL={stream.toURL()}
            style={styles.stream} />
      }
        <View
          style={styles.footer}>
          <Button
            title = "Start"
            onPress = {start} />
          <Button
            title = "Stop"
            onPress = {stop} />
        </View>
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
