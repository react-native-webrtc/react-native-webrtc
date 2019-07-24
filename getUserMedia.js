'use strict';

import {Platform, NativeModules} from 'react-native';
import * as RTCUtil from './RTCUtil';

import MediaStream from './MediaStream';
import MediaStreamError from './MediaStreamError';
import MediaStreamTrack from './MediaStreamTrack';
import permissions from './Permissions';

const { WebRTCModule } = NativeModules;


export default function getUserMedia(constraints = {}) {
  // According to
  // https://www.w3.org/TR/mediacapture-streams/#dom-mediadevices-getusermedia,
  // the constraints argument is a dictionary of type MediaStreamConstraints.
  if (typeof constraints !== 'object') {
    return Promise.reject(new TypeError('constraints is not a dictionary'));
  }

  if ((typeof constraints.audio === 'undefined' || !constraints.audio)
      && (typeof constraints.video === 'undefined' || !constraints.video)) {
    return Promise.reject(new TypeError('audio and/or video is required'));
  }

  // Normalize constraints.
  constraints = RTCUtil.normalizeConstraints(constraints);

  // Request required permissions
  const reqPermissions = [];
  if (constraints.audio) {
    reqPermissions.push(permissions.request({ name: 'microphone' }));
  } else {
    reqPermissions.push(Promise.resolve(false));
  }
  if (constraints.video) {
    reqPermissions.push(permissions.request({ name: 'camera' }));
  } else {
    reqPermissions.push(Promise.resolve(false));
  }

  return new Promise((resolve, reject) => {
    Promise.all(reqPermissions).then(results => {
      const [ audioPerm, videoPerm ] = results;

      // Check permission results and remove unneeded permissions.

      if (!audioPerm && !videoPerm) {
        // https://www.w3.org/TR/mediacapture-streams/#dom-mediadevices-getusermedia
        // step 4
        const error = {
          message: 'Permission denied.',
          name: 'SecurityError'
        };
        reject(new MediaStreamError(error));

        return;
      }

      audioPerm || (delete constraints.audio);
      videoPerm || (delete constraints.video);

      WebRTCModule.getUserMedia(
        constraints,
        /* successCallback */ (id, tracks) => {
          const stream = new MediaStream(id);
          for (const track of tracks) {
            stream.addTrack(new MediaStreamTrack(track));
          }
    
          resolve(stream);
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

          reject(error);
        });
    });
  });
}
