import { NativeModules, Platform } from 'react-native';

const { WebRTCModule } = NativeModules;

export type TelecomConfig = {
    /** Label shown in the system call UI and in the call log. */
    displayName: string;
    /**
     * Stable identifier for the remote party (e.g. a user id), used as the call's Telecom
     * address. Unlike iOS there is no redial-from-Recents path on Android today, so this is
     * identity only - it is not handed back to the app. Defaults to `displayName`.
     */
    handle?: string;
    isVideo: boolean;
};

export type TelecomEventType =
    | 'started'
    | 'answer'
    | 'ended'
    | 'failed'
    | 'muteChanged'
    | 'holdChanged';

/**
 * Cross-platform reason a call ended, surfaced from both Telecom (Android) and
 * CallKit (iOS):
 * - `local` — this device hung up (or the system CallKit UI's End/Decline, which
 *   iOS can't distinguish from a plain hang-up).
 * - `rejected` — the callee actively declined a ringing incoming call (Android only
 *   — CallKit has no ended-reason case for a local decline, so on iOS this also
 *   surfaces as `local`).
 * - `missed` — an incoming call rang and was never answered, including the native
 *   ring timeout (default 45 seconds), or an Android outgoing call did not connect
 *   before its native timeout (default 60 seconds).
 * - `remote` — the other party ended the call.
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
    requestId?: string;
    reason?: CallEndedReason | string;
    muted?: boolean;
    held?: boolean;
};

const isAndroid = Platform.OS === 'android';

export async function startTelecomCall(config: TelecomConfig): Promise<void> {
    if (!isAndroid) {
        return;
    }
    await WebRTCModule.startTelecomCall(
        config.displayName,
        config.handle ?? config.displayName,
        config.isVideo,
    );
}

export async function reportTelecomCallConnected(): Promise<void> {
    if (!isAndroid) {
        return;
    }
    await WebRTCModule.reportOutgoingCallConnected();
}

export async function fulfillTelecomCallAnswered(
    requestId: string,
): Promise<boolean> {
    if (!isAndroid) {
        return false;
    }
    return WebRTCModule.fulfillTelecomCallAnswered(requestId);
}

export async function failTelecomCallAnswered(
    requestId: string,
): Promise<void> {
    if (!isAndroid) {
        return;
    }
    await WebRTCModule.failTelecomCallAnswered(requestId);
}

export function getPendingAnswerRequestId(): string | null {
    if (!isAndroid) {
        return null;
    }
    const requestId: unknown = WebRTCModule.getPendingAnswerRequestId();
    return typeof requestId === 'string' ? requestId : null;
}

export async function endTelecomCall(
    reason: CallEndedReason = 'local',
): Promise<void> {
    if (!isAndroid) {
        return;
    }
    await WebRTCModule.endTelecomCall(reason);
}

export async function setTelecomCallHeld(onHold: boolean): Promise<void> {
    if (!isAndroid) {
        return;
    }
    await WebRTCModule.setTelecomCallHeld(onHold);
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

export function isTelecomCallHeld(): boolean {
    if (!isAndroid) {
        return false;
    }
    return WebRTCModule.isTelecomCallHeld();
}
