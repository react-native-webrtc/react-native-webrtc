'use strict';

import { NativeModules, PermissionsAndroid, Platform } from 'react-native';

const { WebRTCModule } = NativeModules;

/**
 * Type declaration for a permissions descriptor.
 */
type PermissionDescriptor = {
    name: string;
}

/**
 * Class implementing a subset of W3C's Permissions API as defined by:
 * https://www.w3.org/TR/permissions/
 */
class Permissions {
    /**
     * Possible result values for {@link query}, in accordance with:
     * https://www.w3.org/TR/permissions/#status-of-a-permission
     */
    RESULT = {
        DENIED: 'denied',
        GRANTED: 'granted',
        PROMPT: 'prompt'
    };

    /**
     * This implementation only supports requesting these permissions, a subset
     * of: https://www.w3.org/TR/permissions/#permission-registry
     */
    VALID_PERMISSIONS = [ 'camera', 'microphone' ];

    _lastReq = Promise.resolve();

    /**
     * Helper for requesting Android permissions. On Android only one permission
     * can be requested at a time (unless the multi-permission API is used,
     * but we are not using that for symmetry with the W3C API for querying)
     * so we'll queue them up.
     * 
     * @param {string} perm - The requested permission from
     * {@link PermissionsAndroid.PERMISSIONS}
     * https://facebook.github.io/react-native/docs/permissionsandroid#permissions-that-require-prompting-the-user
     */
    _requestPermissionAndroid(perm) {
        return new Promise((resolve, reject) => {
            PermissionsAndroid.request(perm).then(
                granted => resolve(granted === true || granted === PermissionsAndroid.RESULTS.GRANTED),
                () => resolve(false));
        });
    }

    /**
     * Validates the given permission descriptor.
     */
    _validatePermissionDescriptior(permissionDesc) {
        if (typeof permissionDesc !== "object") {
            throw new TypeError("Argument 1 of Permissions.query is not an object.");
        }
        if (typeof permissionDesc.name === "undefined") {
            throw new TypeError("Missing required 'name' member of PermissionDescriptor.");
        }
        if (this.VALID_PERMISSIONS.indexOf(permissionDesc.name) === -1) {
            throw new TypeError("'name' member of PermissionDescriptor is not a valid value for enumeration PermissionName.");
        }
    }

    /**
     * Method for querying the status of a permission, according to:
     * https://www.w3.org/TR/permissions/#permissions-interface
     */
    query(permissionDesc: PermissionDescriptor) {
        try {
            this._validatePermissionDescriptior(permissionDesc);
        } catch (e) {
            return Promise.reject(e);
        }
        if (Platform.OS === 'android') {
            const perm = permissionDesc.name === 'camera'
                ? PermissionsAndroid.PERMISSIONS.CAMERA
                : PermissionsAndroid.PERMISSIONS.RECORD_AUDIO;
            return new Promise((resolve, reject) => {
                PermissionsAndroid.check(perm).then(
                    granted => resolve(granted ? this.RESULT.GRANTED : this.RESULT.PROMPT),
                    () => resolve(this.RESULT.PROMPT));
            });
        } else if (Platform.OS === 'ios') {
            return WebRTCModule.checkPermission(permissionDesc.name);
        } else {
            return Promise.reject(new TypeError("Unsupported platform."));
        }
    }

    /**
     * Custom method NOT defined by W3C's permissions API, which allows the
     * caller to request a permission.
     */
    request(permissionDesc: PermissionDescriptor) {
        try {
            this._validatePermissionDescriptior(permissionDesc);
        } catch (e) {
            return Promise.reject(e);
        }
        if (Platform.OS === 'android') {
            const perm = permissionDesc.name === 'camera'
                ? PermissionsAndroid.PERMISSIONS.CAMERA
                : PermissionsAndroid.PERMISSIONS.RECORD_AUDIO;
            const requestPermission
                = () => this._requestPermissionAndroid(perm);
            this._lastReq
                = this._lastReq.then(requestPermission, requestPermission);
            return this._lastReq;
        } else if (Platform.OS === 'ios') {
            return WebRTCModule.requestPermission(permissionDesc.name);
        } else {
            return Promise.reject(new TypeError("Unsupported platform."));
        }
    }
}

export default new Permissions();
