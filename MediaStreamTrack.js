'use strict';

var React = require('react-native');
var {
  NativeModules,
} = React;

var WebRTCModule = NativeModules.WebRTCModule;

type SourceInfo = {
  id: string;
  label: string;
  facing: string;
  kind: string;
};

class MediaStreamTrack {
  static getSources(success: (sources: Array<SourceInfo>) => void) {
    WebRTCModule.mediaStreamTrackGetSources(success);
  }

  enabled: boolean;
  id: number; // NOTE: spec wants string here
  kind: string;
  label: string;
  muted: boolean;
  constructor(info) {
    this.id = info.id;
    this.enabled = true;
    this.kind = info.kind;
    this.label = info.label;
    this.muted = false;
  }
}

module.exports = MediaStreamTrack;
