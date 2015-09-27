'use strict';
var React = require('react-native');
var { requireNativeComponent } = React;

class RTCView extends React.Component {
  render() {
    return <RTCVideoView {...this.props} />;
  }
}

RTCView.propTypes = {
  src: React.PropTypes.number,
};

var RTCVideoView = requireNativeComponent('RTCVideoView', RTCView);

module.exports = RTCView;
