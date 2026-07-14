import { useCallback, useEffect, useRef } from 'react';
import { Platform } from 'react-native';

import { addListener, removeListener } from './EventEmitter';
import {
    type CallEndedReason,
    endTelecomCall,
    hasActiveTelecomCall,
    isTelecomCallAnswered,
    startTelecomCall,
    type TelecomConfig,
    type TelecomEvent,
} from './Telecom';

export type UseTelecomResult = {
    startCall: (config: TelecomConfig) => Promise<void>;
    endCall: (reason?: CallEndedReason) => Promise<void>;
    hasActiveCall: () => boolean;
    isAnswered: () => boolean;
};

function useTelecomAndroid(): UseTelecomResult {
    const startCall = useCallback(
        (config: TelecomConfig) => startTelecomCall(config),
        [],
    );
    const endCall = useCallback(
        (reason?: CallEndedReason) => endTelecomCall(reason),
        [],
    );
    const hasActiveCall = useCallback(() => hasActiveTelecomCall(), []);
    const isAnswered = useCallback(() => isTelecomCallAnswered(), []);

    return {
        startCall,
        endCall,
        hasActiveCall,
        isAnswered,
    };
}

const noop = async () => {};
const emptyResult: UseTelecomResult = {
    startCall: noop,
    endCall: noop,
    hasActiveCall: () => false,
    isAnswered: () => false,
};

export const useTelecom = Platform.select({
    android: useTelecomAndroid,
    default: () => emptyResult,
}) as typeof useTelecomAndroid;

export function useTelecomEvent(callback: (event: TelecomEvent) => void): void {
    const callbackRef = useRef(callback);
    callbackRef.current = callback;
    const listener = useRef({});

    useEffect(() => {
        if (Platform.OS !== 'android') {
            return;
        }

        addListener(listener.current, 'telecomActionPerformed', (event) => {
            if (event && typeof event === 'object') {
                callbackRef.current(event as TelecomEvent);
            }
        });
        const current = listener.current;
        return () => removeListener(current);
    }, []);
}
