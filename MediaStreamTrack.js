'use strict';

var React = require('react-native');
var {
  NativeModules,
} = React;

var WebRTCModule = NativeModules.WebRTCModule;

class SourceInfo {
  id: string;
  label: string;
  facing: string;
  kind: string;
  constructor(info) {
    this.id = info.id;
    this.label = info.label;
    this.facing = info.facing;
    this.kind = info.kind;
  }
  toJSON() {
    return {
      id: this.id,
      label: this.label,
      facing: this.facing,
      kind: this.kind,
    };
  }
}

class MediaStreamTrack {
  static getSources = function(success) {
    WebRTCModule.mediaStreamTrackGetSources(sources => {
      var sourceInfos = sources.map(s => new SourceInfo(s));
      success(sourceInfos);
    });
  };

  enabled: boolean;
  id: string;
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
