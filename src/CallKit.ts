import { NativeModules, Platform } from 'react-native';

import type { CallEndedReason } from './Telecom';

const { WebRTCModule } = NativeModules;

export type CallKitConfig = {
    displayName: string;
    isVideo: boolean;
};

export type CallKitAction = {
    started?: undefined;
    answer?: undefined;
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
    await WebRTCModule.startCallKitSession(config.displayName, config.isVideo);
}

export async function endCallKitSession(
    reason: CallEndedReason = 'local',
): Promise<void> {
    if (Platform.OS !== 'ios') {
        return;
    }
    await WebRTCModule.endCallKitSession(reason);
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
