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
  }
  return constraints;
}
