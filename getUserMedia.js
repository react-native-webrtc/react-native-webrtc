'use strict';

import {Platform, NativeModules} from 'react-native';
import * as RTCUtil from './RTCUtil';

import MediaStream from './MediaStream';
import MediaStreamError from './MediaStreamError';
import MediaStreamTrack from './MediaStreamTrack';
import withLegacyCallbacks from './withLegacyCallbacks';

const {WebRTCModule} = NativeModules;

// native side consume string eventually
const DEFAULT_VIDEO_CONSTRAINTS = {
  mandatory: {
    minWidth: '1280',
    minHeight: '720',
    minFrameRate: '30',
  },
  optional: [],
};

function getDefaultMediaConstraints(mediaType) {
  return (mediaType === 'audio'
      ? true // no audio default constraint currently
      : RTCUtil.mergeMediaConstraints(DEFAULT_VIDEO_CONSTRAINTS));
}

// this will make sure we have the correct constraint structure
// TODO: support width/height range and the latest param names according to spec
//   media constraints param name should follow spec. then we need a function to convert these `js names`
//   into the real `const name that native defined` on both iOS and Android.
// see mediaTrackConstraints: https://www.w3.org/TR/mediacapture-streams/#dom-mediatrackconstraints
function parseMediaConstraints(customConstraints, mediaType) {
  return (mediaType === 'audio'
      ? RTCUtil.mergeMediaConstraints(customConstraints, {}) // no audio default constraint currently
      : RTCUtil.mergeMediaConstraints(customConstraints, DEFAULT_VIDEO_CONSTRAINTS));
}

// this will make sure we have the correct value type
function normalizeMediaConstraints(constraints, mediaType) {
  if (mediaType === 'audio') {
    ; // to be added
  } else {
    // NOTE: android only support minXXX currently
    for (const param of ['minWidth', 'minHeight', 'minFrameRate', 'maxWidth', 'maxHeight', 'maxFrameRate', ]) {
      if (constraints.mandatory.hasOwnProperty(param)) {
        // convert to correct type here so that native can consume directly without worries.
        constraints.mandatory[param] = (Platform.OS === 'ios'
            ? constraints.mandatory[param].toString() // ios consumes string
            : parseInt(constraints.mandatory[param])); // android eats integer
      }
    }
  }

  return constraints;
}

export default withLegacyCallbacks(constraints => {
  // According to
  // https://www.w3.org/TR/mediacapture-streams/#dom-mediadevices-getusermedia,
  // the constraints argument is a dictionary of type MediaStreamConstraints.
  if (typeof constraints === 'object') {
    // According to step 2 of the getUserMedia() algorithm, requestedMediaTypes
    // is the set of media types in constraints with either a dictionary value
    // or a value of "true".
    let requestedMediaTypes = 0;
    for (const mediaType of [ 'audio', 'video' ]) {
      // According to the spec, the types of the audio and video members of
      // MediaStreamConstraints are either boolean or MediaTrackConstraints
      // (i.e. dictionary).
      const mediaTypeConstraints = constraints[mediaType];
      const typeofMediaTypeConstraints = typeof mediaTypeConstraints;
      if (typeofMediaTypeConstraints !== 'undefined') {
        if (typeofMediaTypeConstraints === 'boolean') {
          if (mediaTypeConstraints) {
            ++requestedMediaTypes;
            constraints[mediaType] = getDefaultMediaConstraints(mediaType);
          }
        } else if (typeofMediaTypeConstraints == 'object') {
          // Note: object constraints for audio is not implemented in native side
          ++requestedMediaTypes;
          constraints[mediaType] = parseMediaConstraints(constraints[mediaType], mediaType);
        } else {
          throw new TypeError('constraints.' + mediaType + ' is neither a boolean nor a dictionary');
        }

        // final check constraints and convert value to native accepted type
        if (typeof constraints[mediaType] === 'object') {
          constraints[mediaType] = normalizeMediaConstraints(constraints[mediaType], mediaType);
        }
      }
    }
    // According to step 3 of the getUserMedia() algorithm, if
    // requestedMediaTypes is the empty set, the method invocation fails with
    // a TypeError.
    if (requestedMediaTypes === 0) {
      throw new TypeError('constraints requests no media types');
    }
  } else {
    throw new TypeError('constraints is not a dictionary');
  }

  return WebRTCModule.getUserMedia(constraints)
    .then(([id, tracks]) => {
      const stream = new MediaStream(id);
      for (const track of tracks) {
        stream.addTrack(new MediaStreamTrack(track));
      }
      return stream;
    })
    .catch(({ message, code }) => {
      let error;
      switch (code) {
      case 'DOMException':
        // According to
        // https://www.w3.org/TR/mediacapture-streams/#idl-def-MediaStreamError,
        // MediaStreamError is either a DOMException object or an
        // OverconstrainedError object. We are very likely to not have a
        // definition of DOMException on React Native (unless the client has
        // provided such a definition). If necessary, we will fall back to our
        // definition of MediaStreamError.
        if (typeof DOMException === 'function') {
          error = new DOMException(/* message */ undefined, /* name */ message);
        }
        break;
      case 'OverconstrainedError':
        if (typeof OverconstrainedError === 'function') {
          error = new OverconstrainedError(/* constraint */ undefined, message);
        }
        break;
      case 'TypeError':
        error = new TypeError(message);
        break;
      }
      if (!error) {
        error = new MediaStreamError({ message, name: code });
      }

      throw error;
    });
});
