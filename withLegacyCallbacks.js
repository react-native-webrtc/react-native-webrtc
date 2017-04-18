module.exports = withLegacyCallbacks;

/**
* Returns a Promise and legacy callbacks compatible function
* @param {Function} method - The function to make Promise and legacy callbacks compatible
* @param {Boolean} [callbackFirst] - Indicate whether or not the success and failure callbacks are the firsts arguments
* @param {Number} [successIndex] - Index of the success callback
* @param {Number} [failureIndex] - Index of the failure callback
* @returns {Function}
*/
function withLegacyCallbacks(method, callbackFirst, successIndex, failureIndex) {
  return function(...args) {
    const successPos = successIndex || (callbackFirst ? 0 : args.length - 2);
    const failurePos = failureIndex || (callbackFirst ? 1 : args.length - 1);
    const success = args[successPos];
    const failure = args[failurePos];
    // If legacy callbacks
    if (typeof success === 'function' &&
        typeof failure === 'function') {
      const newArgs = args.filter(item => item !== success && item !== failure);
      return method(...newArgs)
        .then(success)
        .catch(failure);
    }
    return method(...args);
  };
}
