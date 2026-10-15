
import { NativeModules } from 'react-native';

const { WebRTCModule } = NativeModules;

/**
 * End-to-end encryption (E2EE) for WebRTC media frames using AES-128-GCM.
 *
 * A frame cryptor is attached to an RTCRtpSender or RTCRtpReceiver of an
 * existing RTCPeerConnection and transparently encrypts/decrypts every
 * encoded media frame that flows through it, on top of the standard SRTP
 * encryption.
 *
 * The wire format is byte-identical across Android and iOS:
 *
 *   header || AES-128-GCM(payload) || tag(16) || IV(12) || trailer(2)
 *
 * where the header (1 byte for audio, 3/10 bytes for video) stays in
 * plaintext and is used as the GCM additional authenticated data, the IV is
 * the SSRC (4 bytes, big-endian) concatenated with a per-cryptor frame
 * counter (8 bytes, big-endian) and the trailer carries the IV length and
 * the key index. Keys are 16-byte AES keys derived as the first 16 bytes of
 * SHA-256(UTF-8(passphrase)).
 */

function assertNativeModule() {
    if (!WebRTCModule) {
        throw new Error('E2EE frame cryptors are not supported on this platform');
    }
}

/**
 * Creates a frame cryptor and attaches it to the given sender or receiver.
 *
 * @param {number} peerConnectionId - The ID of the RTCPeerConnection.
 * @param {'sender' | 'receiver'} type - Whether to attach to an RTCRtpSender
 * (frames get encrypted) or an RTCRtpReceiver (frames get decrypted).
 * @param {string} rtpId - The ID of the RTCRtpSender or RTCRtpReceiver.
 * @returns {Promise<string>} Resolves with the ID of the created cryptor.
 */
export async function e2eeCreateFrameCryptor(
    peerConnectionId: number,
    type: 'sender' | 'receiver',
    rtpId: string): Promise<string> {
    assertNativeModule();

    return WebRTCModule.e2eeCreateFrameCryptor({ peerConnectionId, type, rtpId });
}

/**
 * Sets the key (a passphrase string) at the given key ring slot. For sender
 * cryptors this also makes it the current send key.
 *
 * @param {string} cryptorId - The ID of the frame cryptor.
 * @param {number} keyIndex - The key ring slot; only the lower 4 bits are used.
 * @param {string} key - The passphrase from which the AES key is derived.
 * @returns {Promise<void>}
 */
export async function e2eeFrameCryptorSetKey(cryptorId: string, keyIndex: number, key: string): Promise<void> {
    assertNativeModule();

    return WebRTCModule.e2eeFrameCryptorSetKey({ cryptorId, keyIndex, key });
}

/**
 * Enables or disables a frame cryptor. A disabled cryptor passes frames
 * through unchanged.
 *
 * @param {string} cryptorId - The ID of the frame cryptor.
 * @param {boolean} enabled - Whether the cryptor should be enabled.
 * @returns {Promise<void>}
 */
export async function e2eeFrameCryptorSetEnabled(cryptorId: string, enabled: boolean): Promise<void> {
    assertNativeModule();

    return WebRTCModule.e2eeFrameCryptorSetEnabled({ cryptorId, enabled });
}

/**
 * Disposes a frame cryptor. The cryptor is detached logically: any frames
 * still flowing through it are passed through unchanged, and the native
 * resources are released once the sender or receiver lets go of them.
 *
 * @param {string} cryptorId - The ID of the frame cryptor.
 * @returns {Promise<void>}
 */
export async function e2eeFrameCryptorDispose(cryptorId: string): Promise<void> {
    assertNativeModule();

    return WebRTCModule.e2eeFrameCryptorDispose({ cryptorId });
}
