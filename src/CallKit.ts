import { NativeModules, Platform } from 'react-native';

import type { CallEndedReason } from './Telecom';

const { WebRTCModule } = NativeModules;

export type CallKitConfig = {
    /** Label shown in the system call UI and in Recents. */
    displayName: string;
    /**
     * Stable identifier for the remote party (e.g. a user id). It is what iOS persists
     * in Recents and hands back in the redial intent, so it must be something your app
     * can resolve - `displayName` alone is ambiguous when two users share a name.
     * Defaults to `displayName`.
     */
    handle?: string;
    isVideo: boolean;
};

export type CallKitAction = {
    started?: undefined;
    answer?: string;
    ended?: CallEndedReason;
    failed?: string;
    muted?: boolean;
    held?: boolean;
};

export async function startCallKitSession(
    config: CallKitConfig,
): Promise<void> {
    if (Platform.OS !== 'ios') {
        return;
    }
    await WebRTCModule.startCallKitSession(
        config.displayName,
        config.handle ?? config.displayName,
        config.isVideo,
    );
}

export async function endCallKitSession(
    reason: CallEndedReason = 'local',
): Promise<void> {
    if (Platform.OS !== 'ios') {
        return;
    }
    await WebRTCModule.endCallKitSession(reason);
}

export async function fulfillIncomingCallConnected(
    requestId: string,
): Promise<boolean> {
    if (Platform.OS !== 'ios') {
        return false;
    }
    return WebRTCModule.fulfillIncomingCallConnected(requestId);
}

export async function failIncomingCallConnected(
    requestId: string,
): Promise<void> {
    if (Platform.OS !== 'ios') {
        return;
    }
    await WebRTCModule.failIncomingCallConnected(requestId);
}

export async function reportOutgoingCallConnected(): Promise<void> {
    if (Platform.OS !== 'ios') {
        return;
    }
    await WebRTCModule.reportOutgoingCallConnected();
}

export async function setCallKitCallHeld(onHold: boolean): Promise<void> {
    if (Platform.OS !== 'ios') {
        return;
    }
    await WebRTCModule.setCallKitCallHeld(onHold);
}

export function getPendingAnswerRequestId(): string | null {
    if (Platform.OS !== 'ios') {
        return null;
    }
    const requestId: unknown = WebRTCModule.getPendingAnswerRequestId();
    return typeof requestId === 'string' ? requestId : null;
}

export function hasActiveCallKitSession(): boolean {
    if (Platform.OS !== 'ios') {
        return false;
    }
    return WebRTCModule.hasActiveCallKitSession();
}

export function isCallAnswered(): boolean {
    if (Platform.OS !== 'ios') {
        return false;
    }
    return WebRTCModule.isCallAnswered();
}

export function isCallKitCallHeld(): boolean {
    if (Platform.OS !== 'ios') {
        return false;
    }
    return WebRTCModule.isCallKitCallHeld();
}
