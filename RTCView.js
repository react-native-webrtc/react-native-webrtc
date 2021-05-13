'use strict';

import {
  NativeModules,
  requireNativeComponent,
} from 'react-native';
import PropTypes from 'prop-types';

const {WebRTCModule} = NativeModules;

const RTCView = {
  name: 'RTCVideoView',
  propTypes: {

    /**
     * Indicates whether the video specified by {@link #streamURL} should be
     * mirrored during rendering. Commonly, applications choose to mirror the
     * user-facing camera.
     */
    mirror: PropTypes.bool,

    /**
     * In the fashion of
     * https://www.w3.org/TR/html5/embedded-content-0.html#dom-video-videowidth
     * and https://www.w3.org/TR/html5/rendering.html#video-object-fit,
     * resembles the CSS style object-fit.
     */
    objectFit: PropTypes.oneOf(['contain', 'cover']),

    streamURL: PropTypes.string,

    /**
     * Similarly to the CSS property z-index, specifies the z-order of this
     * RTCView in the stacking space of all RTCViews. When RTCViews overlap,
     * zOrder determines which one covers the other. An RTCView with a larger
     * zOrder generally covers an RTCView with a lower one.
     *
     * Non-overlapping RTCViews may safely share a z-order (because one does not
     * have to cover the other).
     *
     * The support for zOrder is platform-dependent and/or
     * implementation-specific. Thus, specifying a value for zOrder is to be
     * thought of as giving a hint rather than as imposing a requirement. For
     * example, video renderers such as RTCView are commonly implemented using
     * OpenGL and OpenGL views may have different numbers of layers in their
     * stacking space. Android has three: a layer bellow the window (aka
     * default), a layer bellow the window again but above the previous layer
     * (aka media overlay), and above the window. Consequently, it is advisable
     * to limit the number of utilized layers in the stacking space to the
     * minimum sufficient for the desired display. For example, a video call
     * application usually needs a maximum of two zOrder values: 0 for the
     * remote video(s) which appear in the background, and 1 for the local
     * video(s) which appear above the remote video(s).
     */
    zOrder: PropTypes.number
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
  nativeID: true,
}});

export default View;
