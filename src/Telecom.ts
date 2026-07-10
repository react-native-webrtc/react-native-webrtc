import { NativeModules, Platform } from 'react-native';

const { WebRTCModule } = NativeModules;

export type TelecomConfig = {
    displayName: string;
    isVideo: boolean;
};

export type TelecomEventType =
    | 'started'
    | 'answer'
    | 'ended'
    | 'failed'
    | 'muteChanged';

/**
 * Cross-platform reason a call ended, surfaced from both Telecom (Android) and
 * CallKit (iOS):
 * - `local` — this device hung up (or the system CallKit UI's End/Decline, which
 *   iOS can't distinguish from a plain hang-up).
 * - `rejected` — the callee actively declined a ringing incoming call (Android only
 *   — CallKit has no ended-reason case for a local decline, so on iOS this also
 *   surfaces as `local`).
 * - `missed` — an incoming call rang and was never answered.
 * - `remote` — the other party ended the call, or an outgoing call wasn't answered.
 * - `answeredElsewhere` — answered on another of the user's devices while ringing.
 * - `failed` — call setup (e.g. room join) failed.
 */
export type CallEndedReason =
    | 'local'
    | 'rejected'
    | 'missed'
    | 'remote'
    | 'answeredElsewhere'
    | 'failed';

export type TelecomEvent = {
    event: TelecomEventType;
    reason?: CallEndedReason | string;
    muted?: boolean;
};

const isAndroid = Platform.OS === 'android';

export async function startTelecomCall(config: TelecomConfig): Promise<void> {
    if (!isAndroid) {
        return;
    }
    await WebRTCModule.startTelecomCall(config.displayName, config.isVideo);
}

export async function setTelecomCallActive(): Promise<void> {
    if (!isAndroid) {
        return;
    }
    await WebRTCModule.setTelecomCallActive();
}

export async function endTelecomCall(
    reason: CallEndedReason = 'local',
): Promise<void> {
    if (!isAndroid) {
        return;
    }
    await WebRTCModule.endTelecomCall(reason);
}

export function hasActiveTelecomCall(): boolean {
    if (!isAndroid) {
        return false;
    }
    return WebRTCModule.hasActiveTelecomCall();
}

export function isTelecomCallAnswered(): boolean {
    if (!isAndroid) {
        return false;
    }
    return WebRTCModule.isTelecomCallAnswered();
}
