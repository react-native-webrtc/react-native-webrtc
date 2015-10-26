'use strict';
var React = require('react-native');
var { requireNativeComponent } = React;

var RTCView = {
  name: 'RTCVideoView',
  propTypes: {
    streamURL: React.PropTypes.number,
  },
};

var v = requireNativeComponent('RTCVideoView', RTCView, {nativeOnly: {
  'scaleX': true,
  'scaleY': true,
  'testID': true,
  'decomposedMatrix': true,
  'backgroundColor': true,
  'accessibilityComponentType': true,
  'renderToHardwareTextureAndroid': true,
  'translateY': true,
  'translateX': true,
  'accessibilityLabel': true,
  'accessibilityLiveRegion': true,
  'importantForAccessibility': true,
  'rotation': true,
  'opacity': true,
}});

module.exports = v;
