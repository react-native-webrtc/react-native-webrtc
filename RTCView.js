'use strict';

import {
  DeviceEventEmitter,
  NativeModules,
  requireNativeComponent,
} from 'react-native';
const WebRTCModule = NativeModules.WebRTCModule;

import {
  PropTypes,
} from 'react';

const RTCView = {
  name: 'RTCVideoView',
  propTypes: {
    streamURL: PropTypes.number,
  },
};

const v = requireNativeComponent('RTCVideoView', RTCView, {nativeOnly: {
  testID: true,
  accessibilityComponentType: true,
  renderToHardwareTextureAndroid: true,
  accessibilityLabel: true,
  accessibilityLiveRegion: true,
  importantForAccessibility: true,
  onLayout: true,
}});

module.exports = v;
