'use strict';

/**
 * Merge custom constraints with the default one. The custom one take precedence.
 *
 * @param {Object} custom - custom webrtc constraints
 * @param {Object} def - default webrtc constraints
 * @return {Object} constraints - merged webrtc constraints
 */
export function mergeMediaConstraints(custom, def) {
  const constraints = (def ? Object.assign({}, def) : {});
  if (custom) {
    if (custom.mandatory) {
      constraints.mandatory = {...constraints.mandatory, ...custom.mandatory};
    }
    if (custom.optional && Array.isArray(custom.optional)) {
      // `optional` is an array, webrtc only finds first and ignore the rest if duplicate.
      constraints.optional = custom.optional.concat(constraints.optional);
    }
    if (custom.facingMode) {
      constraints.facingMode = custom.facingMode.toString(); // string, 'user' or the default 'environment'
    }
  }
  return constraints;
}

/**
* Returns a Promise and legacy callbacks compatible function
* @param {Function} method - The function to make Promise and legacy callbacks compatible
* @param {Boolean} [callbackFirst] - Indicate whether or not the success and failure callbacks are the firsts arguments
* @returns {Function}
*/
export function promisify(method, callbackFirst) {
  return function(...origArgs) {
    let onSuccess = origArgs[(callbackFirst ? 0 : origArgs.length - 2)];
    let onFailure = origArgs[(callbackFirst ? 1 : origArgs.length - 1)];
    let args = origArgs;

    // --- should promisify functions which has only one success callback.

    if (typeof onSuccess === 'function') {
      args = args.filter(item => item !== onSuccess);
    } else {
      onSuccess = function(res) { return res; };
    }

    if (typeof onFailure === 'function') {
      args = args.filter(item => item !== onFailure);
    } else {
      onFailure = function(err) { throw err; };
    }

    // --- always pass resolve/reject as callback
    return new Promise(function(resolve, reject) {
      const newArgs = (callbackFirst ? [resolve, reject, ...args] : [...args, resolve, reject]);
      method(...newArgs);
    })
    .then(onSuccess)
    .catch(onFailure);
  };
}
