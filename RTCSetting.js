'use strict';
var WebRTCModule = require('react-native').NativeModules.WebRTCModule;

var RTCSetting = {
  setAudioOutput: function(value) {
    WebRTCModule.setAudioOutput(value);
  },
  setKeepScreenOn: function(value) {
    WebRTCModule.setKeepScreenOn(value);
  },
  setProximityScreenOff: function(value) {
    WebRTCModule.setProximityScreenOff(value);
  },
};

module.exports = RTCSetting;
