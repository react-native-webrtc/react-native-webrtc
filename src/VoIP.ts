import { NativeModules, Platform } from 'react-native';

import {
    failIncomingCallConnected as failCallKitAnswer,
    fulfillIncomingCallConnected as fulfillCallKitAnswer,
    getPendingAnswerRequestId as getPendingCallKitAnswerRequestId,
} from './CallKit';
import {
    failTelecomCallAnswered,
    fulfillTelecomCallAnswered,
    getPendingAnswerRequestId as getPendingTelecomAnswerRequestId,
} from './Telecom';

const { WebRTCModule } = NativeModules;

export function getVoipToken(): Promise<string | null> {
    if (Platform.OS === 'ios') {
        const token = WebRTCModule.getVoipToken();
        return Promise.resolve(typeof token === 'string' ? token : null);
    }
    return WebRTCModule.getVoipToken().then((token: unknown) =>
        typeof token === 'string' ? token : null,
    );
}

export function getPendingIncomingCall(): Record<string, unknown> | null {
    const call = WebRTCModule.getPendingIncomingCall();
    return call && typeof call === 'object'
        ? (call as Record<string, unknown>)
        : null;
}

export function clearPendingIncomingCall(): void {
    WebRTCModule.clearPendingIncomingCall();
}

/**
 * Resolves the parked native answer action once incoming-call media is live.
 * Returns false when the request has already timed out or been resolved.
 */
export function fulfillIncomingCallConnected(
    requestId: string,
): Promise<boolean> {
    return Platform.OS === 'ios'
        ? fulfillCallKitAnswer(requestId)
        : fulfillTelecomCallAnswered(requestId);
}

/**
 * Aborts the parked native answer action. Safe to call after it has timed out.
 */
export function failIncomingCallConnected(
    requestId: string,
): Promise<void> {
    return Platform.OS === 'ios'
        ? failCallKitAnswer(requestId)
        : failTelecomCallAnswered(requestId);
}

/** Returns the answer request that is still awaiting media, if any. */
export function getPendingAnswerRequestId(): string | null {
    return Platform.OS === 'ios'
        ? getPendingCallKitAnswerRequestId()
        : getPendingTelecomAnswerRequestId();
}
