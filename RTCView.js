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
    /**
     * In the fashion of
     * https://www.w3.org/TR/html5/embedded-content-0.html#dom-video-videowidth
     * and https://www.w3.org/TR/html5/rendering.html#video-object-fit,
     * resembles the CSS style object-fit.
     */
    objectFit: PropTypes.oneOf(['contain', 'cover']),
    streamURL: PropTypes.string,
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
