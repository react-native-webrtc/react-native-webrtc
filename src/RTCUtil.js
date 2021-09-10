'use strict';

const DEFAULT_AUDIO_CONSTRAINTS = {};

const DEFAULT_VIDEO_CONSTRAINTS = {
    facingMode: 'user',
    frameRate: 30,
    height: 720,
    width: 1280
};

const ASPECT_RATIO = 16 / 9;

const STANDARD_OA_OPTIONS = {
    icerestart: 'IceRestart',
    offertoreceiveaudio: 'OfferToReceiveAudio',
    offertoreceivevideo: 'OfferToReceiveVideo',
    voiceactivitydetection: 'VoiceActivityDetection'
};

function getDefaultMediaConstraints(mediaType) {
    switch(mediaType) {
    case 'audio':
        return DEFAULT_AUDIO_CONSTRAINTS;
    case 'video':
        return DEFAULT_VIDEO_CONSTRAINTS;
    default:
        throw new TypeError(`Invalid media type: ${mediaType}`);
    }
}

function extractString(constraints, prop) {
    const value = constraints[prop];
    const type = typeof value;

    if (type === 'object') {
        for (const v of [ 'exact', 'ideal' ]) {
            if (value[v]) {
                return value[v];
            }
        }
    } else if (type === 'string') {
        return value;
    }
}

function extractNumber(constraints, prop) {
    const value = constraints[prop];
    const type = typeof value;

    if (type === 'number') {
        return Number.parseInt(value);
    } else if (type === 'object') {
        for (const v of [ 'exact', 'ideal', 'min', 'max' ]) {
            if (value[v]) {
                return Number.parseInt(value[v]);
            }
        }
    }
}

function normalizeMediaConstraints(constraints, mediaType) {
    switch(mediaType) {
    case 'audio':
        return constraints;
    case 'video': {
        let c;
        if (constraints.mandatory) {
            // Old style.
            c = {
                deviceId: extractString(constraints.optional || {}, 'sourceId'),
                facingMode: extractString(constraints, 'facingMode'),
                frameRate: extractNumber(constraints.mandatory, 'minFrameRate'),
                height: extractNumber(constraints.mandatory, 'minHeight'),
                width: extractNumber(constraints.mandatory, 'minWidth')
            };
        } else {
            // New style.
            c = {
                deviceId: extractString(constraints, 'deviceId'),
                facingMode: extractString(constraints, 'facingMode'),
                frameRate: extractNumber(constraints, 'frameRate'),
                height: extractNumber(constraints, 'height'),
                width: extractNumber(constraints, 'width')
            };
        }

        if (!c.deviceId) {
            delete c.deviceId;
        }

        if (!c.facingMode || (c.facingMode !== 'user' && c.facingMode !== 'environment')) {
            c.facingMode = DEFAULT_VIDEO_CONSTRAINTS.facingMode;
        }

        if (!c.frameRate) {
            c.frameRate = DEFAULT_VIDEO_CONSTRAINTS.frameRate;
        }

        if (!c.height && !c.width) {
            c.height = DEFAULT_VIDEO_CONSTRAINTS.height;
            c.width = DEFAULT_VIDEO_CONSTRAINTS.width;
        } else if (!c.height) {
            c.height = Math.round(c.width / ASPECT_RATIO);
        } else if (!c.width) {
            c.width = Math.round(c.height * ASPECT_RATIO);
        }

        return c;
    }
    default:
        throw new TypeError(`Invalid media type: ${mediaType}`);
    }
}

/**
 * Utility for deep cloning an object. Object.assign() only does a shallow copy.
 *
 * @param {Object} obj - object to be cloned
 * @return {Object} cloned obj
 */
export function deepClone(obj) {
    return JSON.parse(JSON.stringify(obj));
}

/**
 * Normalize options passed to createOffer() / createAnswer().
 *
 * @param {Object} options - user supplied options
 * @return {Object} newOptions - normalized options
 */
export function normalizeOfferAnswerOptions(options = {}) {
    const newOptions = {};

    if (!options) {
        return newOptions;
    }

    // Support legacy constraints.
    if (options.mandatory) {
        options = options.mandatory;
    }

    // Convert standard options into WebRTC internal constant names.
    // See: https://github.com/jitsi/webrtc/blob/0cd6ce4de669bed94ba47b88cb71b9be0341bb81/sdk/media_constraints.cc#L113
    for (const [ key, value ] of Object.entries(options)) {
        const newKey = STANDARD_OA_OPTIONS[key.toLowerCase()];
        if (newKey) {
            newOptions[newKey] = String(Boolean(value));
        }
    }

    return newOptions;
}

/**
 * Normalize the given constraints in something we can work with.
 */
export function normalizeConstraints(constraints) {
    const c = deepClone(constraints);

    for (const mediaType of [ 'audio', 'video' ]) {
        const mediaTypeConstraints = c[mediaType];
        const typeofMediaTypeConstraints = typeof mediaTypeConstraints;

        if (typeofMediaTypeConstraints !== 'undefined') {
            if (typeofMediaTypeConstraints === 'boolean') {
                if (mediaTypeConstraints) {
                    c[mediaType] = getDefaultMediaConstraints(mediaType);
                }
            } else if (typeofMediaTypeConstraints === 'object') {
                c[mediaType] = normalizeMediaConstraints(mediaTypeConstraints, mediaType);
            } else {
                throw new TypeError(`constraints.${mediaType} is neither a boolean nor a dictionary`);
            }
        }
    }

    return c;
}
