'use strict';
var ReactNative = require('react-native');
var React = require('react');
var { requireNativeComponent } = ReactNative;
var { PropTypes } = React

var RTCView = {
  name: 'RTCVideoView',
  propTypes: {
    streamURL: PropTypes.number,
  },
};

var v = requireNativeComponent('RTCVideoView', RTCView, {nativeOnly: {
  testID: true,
  accessibilityComponentType: true,
  renderToHardwareTextureAndroid: true,
  accessibilityLabel: true,
  accessibilityLiveRegion: true,
  importantForAccessibility: true,
  onLayout: true,
}});

module.exports = v;
