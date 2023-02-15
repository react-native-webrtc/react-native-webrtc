
const DEFAULT_AUDIO_CONSTRAINTS = {};

const DEFAULT_VIDEO_CONSTRAINTS = {
    facingMode: 'user',
    frameRate: 30,
    height: 720,
    width: 1280
};

const FACING_MODES = [ 'user', 'environment' ];

const ASPECT_RATIO = 16 / 9;

const STANDARD_OFFER_OPTIONS = {
    icerestart: 'IceRestart',
    offertoreceiveaudio: 'OfferToReceiveAudio',
    offertoreceivevideo: 'OfferToReceiveVideo',
    voiceactivitydetection: 'VoiceActivityDetection'
};

const SDP_TYPES = [
    'offer',
    'pranswer',
    'answer',
    'rollback'
];

function getDefaultMediaConstraints(mediaType) {
    switch (mediaType) {
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
        for (const v of [ 'exact', 'ideal', 'max', 'min' ]) {
            if (value[v]) {
                return Number.parseInt(value[v]);
            }
        }
    }
}

function normalizeMediaConstraints(constraints, mediaType) {
    switch (mediaType) {
        case 'audio':
            return constraints;

        case 'video': {
            const c = {
                deviceId: extractString(constraints, 'deviceId'),
                facingMode: extractString(constraints, 'facingMode'),
                frameRate: extractNumber(constraints, 'frameRate'),
                height: extractNumber(constraints, 'height'),
                width: extractNumber(constraints, 'width')
            };

            if (!c.deviceId) {
                delete c.deviceId;
            }

            if (!FACING_MODES.includes(c.facingMode)) {
                c.facingMode = DEFAULT_VIDEO_CONSTRAINTS.facingMode;
            }

            if (!c.frameRate) {
                c.frameRate = DEFAULT_VIDEO_CONSTRAINTS.frameRate;
            }

            if (!c.height && !c.width) {
                c.height = DEFAULT_VIDEO_CONSTRAINTS.height;
                c.width = DEFAULT_VIDEO_CONSTRAINTS.width;
            } else if (!c.height && c.width) {
                c.height = Math.round(c.width / ASPECT_RATIO);
            } else if (!c.width && c.height) {
                c.width = Math.round(c.height * ASPECT_RATIO);
            }

            return c;
        }

        default:
            throw new TypeError(`Invalid media type: ${mediaType}`);
    }
}

/**
 * Utility for creating short random strings from float point values.
 * We take 4 characters from the end after converting to a string.
 * Conversion to string gives us some letters as we don't want just numbers.
 * Should be suitable to pass for enough randomness.
 *
 * @return {String} 4 random characters
 */
function chr4() {
    return Math.random().toString(16).slice(-4);
}

/**
 * Put together a random string in UUIDv4 format {xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}
 *
 * @return {String} uuidv4
 */
export function uniqueID(): string {
    return `${chr4()}${chr4()}-${chr4()}-${chr4()}-${chr4()}-${chr4()}${chr4()}${chr4()}`;
}

/**
 * Utility for deep cloning an object. Object.assign() only does a shallow copy.
 *
 * @param {Object} obj - object to be cloned
 * @return {Object} cloned obj
 */
export function deepClone<T>(obj: T): T {
    return JSON.parse(JSON.stringify(obj));
}

/**
 * Checks whether an SDP type is valid or not.
 *
 * @param type SDP type to check.
 * @returns Whether the SDP type is valid or not.
 */
export function isSdpTypeValid(type: string): boolean {
    return SDP_TYPES.includes(type);
}

/**
 * Normalize options passed to createOffer().
 *
 * @param options - user supplied options
 * @return Normalized options
 */
export function normalizeOfferOptions(options: object = {}): object {
    const newOptions = {};

    if (!options) {
        return newOptions;
    }

    // Convert standard options into WebRTC internal constant names.
    // See: https://github.com/jitsi/webrtc/blob/0cd6ce4de669bed94ba47b88cb71b9be0341bb81/sdk/media_constraints.cc#L113
    for (const [ key, value ] of Object.entries(options)) {
        const newKey = STANDARD_OFFER_OPTIONS[key.toLowerCase()];

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
