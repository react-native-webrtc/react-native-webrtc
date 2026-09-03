# Technical Documentation: `examples/GumTestApp_macOS/App.js`

## Overview

The `App.js` file is a React Native functional component that demonstrates basic video stream capture and rendering using the `react-native-webrtc` library. It provides user interface controls to start media device enumeration and camera capture (`getUserMedia`), render the active video stream using `RTCView`, and stop/release the media stream.

---

## Code Breakdown

### Imports

```javascript
import React, {useState} from 'react';
import {
  Button,
  SafeAreaView,
  StyleSheet,
  ScrollView,
  View,
  Text,
  StatusBar,
} from 'react-native';
import { Colors } from 'react-native/Libraries/NewAppScreen';
import { mediaDevices, RTCView } from 'react-native-webrtc';
```

- **React & Hooks**: Imports `React` and the `useState` hook from `'react'`.
- **React Native Components**:
  - `Button`, `SafeAreaView`, `StyleSheet`, `View`, `StatusBar`: UI components and styling utilities used in rendering the interface.
  - `ScrollView`, `Text`: Imported from `'react-native'`, though unused in the render tree.
- **NewAppScreen**: Imports `Colors` from `'react-native/Libraries/NewAppScreen'` for default UI color references.
- **react-native-webrtc**:
  - `mediaDevices`: Core API used to enumerate available input/output devices and request video streams.
  - `RTCView`: Native component used to render WebRTC media streams.

---

## State Management

```javascript
const [stream, setStream] = useState(null);
```

- **`stream`**: Holds the media stream object returned by `mediaDevices.getUserMedia()`. Initialized to `null`.

---

## Methods and Logic

### `start()`

```javascript
const start = async () => {
  console.log('start');
  const devices = await mediaDevices.enumerateDevices();
  console.log(devices);
  if (!stream) {
    let s;
    try {
      s = await mediaDevices.getUserMedia({ video: true });
      setStream(s);
    } catch(e) {
      console.error(e);
    }
  }
};
```

1. Logs `'start'` to the console.
2. Calls `await mediaDevices.enumerateDevices()` to retrieve available media devices and logs the list to the console.
3. Checks if a `stream` is currently active (`!stream`).
4. If no stream exists, calls `mediaDevices.getUserMedia({ video: true })` to request access to the video input (camera).
5. Updates state using `setStream(s)` with the acquired stream.
6. Catches any errors during stream acquisition and logs them using `console.error(e)`.

---

### `stop()`

```javascript
const stop = () => {
  console.log('stop');
  if (stream) {
    stream.release();
    setStream(null);
  }
};
```

1. Logs `'stop'` to the console.
2. Checks if an active `stream` exists in state.
3. If active, calls `stream.release()` to stop and clean up the stream resources.
4. Resets the `stream` state back to `null`.

---

## User Interface & Layout

```javascript
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
```

### Component Structure:
- **`StatusBar`**: Configured with `barStyle="dark-content"`.
- **`SafeAreaView`**: Encloses the main application body, styled with `styles.body`.
- **Conditional `RTCView` Rendering**:
  - Renders only when `stream` is truthy.
  - Obtains the stream URL via `stream.toURL()`.
  - Applied with `styles.stream`.
- **Footer `View`**:
  - Positioned at the bottom using `styles.footer`.
  - Contains two action buttons:
    - **"Start" Button**: Triggers the `start` function on press.
    - **"Stop" Button**: Triggers the `stop` function on press.

---

## Styles

```javascript
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
```

- **`body`**: Sets background color to `Colors.white` and fills the absolute screen area via `...StyleSheet.absoluteFill`.
- **`stream`**: Sets `flex: 1` so that the video component expands to fill available space within the body.
- **`footer`**: Absolute position at the bottom of the screen (`bottom: 0`, `left: 0`, `right: 0`) with background set to `Colors.lighter`.