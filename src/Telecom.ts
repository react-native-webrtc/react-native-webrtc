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

export type TelecomEvent = {
    event: TelecomEventType;
    reason?: string;
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

export async function endTelecomCall(): Promise<void> {
    if (!isAndroid) {
        return;
    }
    await WebRTCModule.endTelecomCall();
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
