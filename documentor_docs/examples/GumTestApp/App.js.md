# Technical Documentation: `examples/GumTestApp/App.js`

## Overview

The `examples/GumTestApp/App.js` file is a sample React Native application component that demonstrates how to capture local video using `react-native-webrtc` and render it using an `RTCPIPView` component with iOS Picture-in-Picture (PiP) capabilities.

The component provides a basic user interface with buttons to control the media stream state (start/stop) and to trigger or stop iOS Picture-in-Picture mode.

---

## Dependencies & Imports

### React & React Native Core
* **`React`, `useState`, `useRef`**: React core module and hooks used for state management and referencing component DOM/native nodes.
* **`Button`, `SafeAreaView`, `StyleSheet`, `View`, `StatusBar`**: Native UI components from `react-native` used to structure and style the screen.
* **`Colors`**: Default color palette provided by `react-native/Libraries/NewAppScreen`.

### React Native WebRTC
* **`mediaDevices`**: Interface used to access media input devices, specifically calling `getUserMedia` for local video streams.
* **`startIOSPIP`**: Function to programmatically initiate iOS Picture-in-Picture mode for a given view reference.
* **`stopIOSPIP`**: Function to programmatically terminate iOS Picture-in-Picture mode for a given view reference.
* **`RTCPIPView`**: Specialized view component that renders a WebRTC media stream and supports iOS Picture-in-Picture.

---

## State and Refs

| Identifier | Type | Initial Value | Description |
| :--- | :--- | :--- | :--- |
| `view` | `React.MutableRefObject` | `useRef()` | Reference attached to the `RTCPIPView` component instance. Passed to `startIOSPIP` and `stopIOSPIP`. |
| `stream` | State (`MediaStream` \| `null`) | `null` | Stores the active WebRTC media stream acquired from `mediaDevices.getUserMedia()`. |

---

## Functions and Event Handlers

### `start()`
* **Type**: `async () => void`
* **Description**: Requests access to the video input media stream.
* **Logic**:
  1. Checks if `stream` is currently `null` or falsy.
  2. Calls `mediaDevices.getUserMedia({ video: true })` to request camera access.
  3. Updates the `stream` state with the returned media stream object (`setStream(s)`).
  4. Catches and logs any errors encountered during stream creation using `console.error`.

### `startPIP()`
* **Type**: `() => void`
* **Description**: Initiates iOS Picture-in-Picture mode.
* **Logic**: Calls `startIOSPIP(view)` passing the ref attached to the `RTCPIPView`.

### `stopPIP()`
* **Type**: `() => void`
* **Description**: Stops iOS Picture-in-Picture mode.
* **Logic**: Calls `stopIOSPIP(view)` passing the ref attached to the `RTCPIPView`.

### `stop()`
* **Type**: `() => void`
* **Description**: Stops and cleans up the active media stream.
* **Logic**:
  1. Checks if `stream` exists.
  2. Calls `stream.release()` on the active media stream instance.
  3. Sets the `stream` state back to `null`.

---

## Configuration Objects

### `pipOptions`
An object defining configuration options for iOS Picture-in-Picture functionality passed to `<RTCPIPView>`:

```javascript
let pipOptions = {
  startAutomatically: true,
  fallbackView: (<View style={{ height: 50, width: 50, backgroundColor: 'red' }} />),
  preferredSize: {
    width: 400,
    height: 800,
  }
}
```

* **`startAutomatically`** (`boolean`): Configured to `true`.
* **`fallbackView`** (`ReactElement`): A JSX `<View>` fallback element styled with a height and width of 50 and a red background (`backgroundColor: 'red'`).
* **`preferredSize`** (`object`): Defines preferred width (`400`) and height (`800`) dimensions for the PiP view.

---

## Layout & Render Structure

The visual output is wrapped in a `<SafeAreaView>` and structured as follows:

1. **`StatusBar`**: Configured with `barStyle="dark-content"`.
2. **`RTCPIPView`** *(Rendered conditionally when `stream` is present)*:
   * **`ref`**: Attached to `view`.
   * **`streamURL`**: Receives the URL string generated from `stream.toURL()`.
   * **`style`**: Applied via `styles.stream`.
   * **`iosPIP`**: Receives the `pipOptions` object.
3. **Footer Control Panel (`View`)**: Positioned at the bottom of the screen containing four `Button` elements:
   * **"Start"**: Executes `start()`.
   * **"Start PIP"**: Executes `startPIP()`.
   * **"Stop PIP"**: Executes `stopPIP()`.
   * **"Stop"**: Executes `stop()`.

---

## Style Sheet

The component utilizes `StyleSheet.create` with the following rules:

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

* **`body`**: Spans the absolute container frame using `...StyleSheet.absoluteFill` with a white background (`Colors.white`).
* **`stream`**: Takes up available vertical space (`flex: 1`).
* **`footer`**: Positioned absolutely at the bottom stretch of the screen (`bottom: 0`, `left: 0`, `right: 0`) with a lighter background color (`Colors.lighter`).