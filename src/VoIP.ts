import { NativeModules, Platform } from 'react-native';

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
