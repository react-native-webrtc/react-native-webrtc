import { useCallback, useEffect, useRef } from 'react';
import { Platform } from 'react-native';

import {
    CallKitAction,
    CallKitConfig,
    endCallKitSession,
    hasActiveCallKitSession,
    isCallKitCallHeld,
    setCallKitCallHeld,
    startCallKitSession,
} from './CallKit';
import { addListener, removeListener } from './EventEmitter';
import type { CallEndedReason } from './Telecom';

export type UseCallKitResult = {
    startCallKitSession: (config: CallKitConfig) => Promise<void>;
    endCallKitSession: (reason?: CallEndedReason) => Promise<void>;
    getCallKitSessionStatus: () => Promise<boolean>;
    setCallHeld: (onHold: boolean) => Promise<void>;
    isHeld: () => boolean;
};

function useCallKitIos(): UseCallKitResult {
    const startCallKitSessionCb = useCallback(async (config: CallKitConfig) => {
        try {
            await startCallKitSession(config);
        } catch (error) {
            console.error('Failed to start CallKit session:', error);
            throw error;
        }
    }, []);

    const endCallKitSessionCb = useCallback(async (reason?: CallEndedReason) => {
        try {
            await endCallKitSession(reason);
        } catch (error) {
            console.error('Failed to end CallKit session:', error);
            throw error;
        }
    }, []);

    const getCallKitSessionStatus = useCallback(async () => {
        return hasActiveCallKitSession();
    }, []);
    const setCallHeld = useCallback(
        (onHold: boolean) => setCallKitCallHeld(onHold),
        [],
    );
    const isHeld = useCallback(() => isCallKitCallHeld(), []);

    return {
        startCallKitSession: startCallKitSessionCb,
        endCallKitSession: endCallKitSessionCb,
        getCallKitSessionStatus,
        setCallHeld,
        isHeld,
    };
}

const useCallKitServiceIos = (config: CallKitConfig) => {
    const { displayName, isVideo } = config;
    const { startCallKitSession, endCallKitSession } = useCallKitIos();

    useEffect(() => {
        startCallKitSession({ displayName, isVideo }).catch(console.error);

        return () => {
            endCallKitSession().catch(console.error);
        };
    }, [startCallKitSession, endCallKitSession, displayName, isVideo]);
};

const emptyFunction = () => {};

const useCallKitEventIos = <T extends keyof CallKitAction>(
    action: T,
    callback: (event: CallKitAction[T]) => void,
) => {
    const listener = useRef({});

    useEffect(() => {
        addListener(listener.current, 'callKitActionPerformed', (event) => {
            if (event && typeof event === 'object') {
                const payload = event as Record<string, unknown>;
                if (action in payload) {
                    callback(payload[action] as CallKitAction[T]);
                }
            }
        });
        return () => {
            removeListener(listener.current);
        };
    }, [action, callback]);
};

export const useCallKitEvent = Platform.select({
    ios: useCallKitEventIos,
    default: emptyFunction,
}) as typeof useCallKitEventIos;

export const useCallKit = Platform.select({
    ios: useCallKitIos,
    default: emptyFunction,
}) as typeof useCallKitIos;

export const useCallKitService = Platform.select({
    ios: useCallKitServiceIos,
    default: emptyFunction,
}) as typeof useCallKitServiceIos;
