'use strict';

import {NativeModules} from 'react-native';

import MediaStream from './MediaStream';
import MediaStreamError from './MediaStreamError';
import MediaStreamTrack from './MediaStreamTrack';

const {WebRTCModule} = NativeModules;

export default function getUserMedia(
    constraints,
    successCallback,
    errorCallback) {
  if (typeof successCallback !== 'function') {
    throw new TypeError('successCallback is non-nullable and required');
  }
  if (typeof errorCallback !== 'function') {
    throw new TypeError('errorCallback is non-nullable and required');
  }
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
          mediaTypeConstraints && ++requestedMediaTypes;
        } else if (typeofMediaTypeConstraints == 'object') {
          ++requestedMediaTypes;
        } else {
          errorCallback(
            new TypeError(
              'constraints.' + mediaType
                + ' is neither a boolean nor a dictionary'));
          return;
        }
      }
    }
    // According to step 3 of the getUserMedia() algorithm, if
    // requestedMediaTypes is the empty set, the method invocation fails with
    // a TypeError.
    if (requestedMediaTypes === 0) {
      errorCallback(new TypeError('constraints requests no media types'));
      return;
    }
  } else {
    errorCallback(new TypeError('constraints is not a dictionary'));
    return;
  }

  WebRTCModule.getUserMedia(
    constraints,
    /* successCallback */ (id, tracks) => {
      const stream = new MediaStream(id);
      for (const track of tracks) {
        stream.addTrack(new MediaStreamTrack(track));
      }

      successCallback(stream);
    },
    /* errorCallback */ (type, message) => {
      let error;
      switch (type) {
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
        error = new MediaStreamError({ message, name: type });
      }

      errorCallback(error);
    });
}
