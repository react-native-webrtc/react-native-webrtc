'use strict';

import {
  DeviceEventEmitter,
  NativeModules,
  requireNativeComponent,
} from 'react-native';
import {PropTypes} from 'react';

const {WebRTCModule} = NativeModules;

const RTCView = {
  name: 'RTCVideoView',
  propTypes: {
    streamURL: PropTypes.number,
  },
};

const View = requireNativeComponent('RTCVideoView', RTCView, {nativeOnly: {
  testID: true,
  accessibilityComponentType: true,
  renderToHardwareTextureAndroid: true,
  accessibilityLabel: true,
  accessibilityLiveRegion: true,
  importantForAccessibility: true,
  onLayout: true,
}});

export default View;
