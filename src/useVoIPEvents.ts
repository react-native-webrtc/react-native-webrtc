import { useEffect, useRef } from 'react';
import { Platform } from 'react-native';

import { hasActiveCallKitSession, isCallAnswered } from './CallKit';
import { addListener, removeListener } from './EventEmitter';
import { hasActiveTelecomCall, isTelecomCallAnswered } from './Telecom';
import {
    clearPendingIncomingCall,
    getPendingIncomingCall,
    getVoipToken,
} from './VoIP';
import { useCallKitEvent } from './useCallKit';

// If you don't provide displayName it will default to incoming call, isVideo to false
export type VoipIncomingPayload = {
    roomName: string;
    displayName: string;
    isVideo: boolean;
};

export type VoIPEventHandlers = {
    onIncoming?: (payload: VoipIncomingPayload) => void;
    onAnswered?: () => void;
    onEnded?: () => void;
    onRegistered?: (token: string) => void;
};

const assertRoomName = (raw: unknown): string => {
    if (!raw || typeof raw !== 'object') {
        throw new Error('VoIP incoming payload must be an object');
    }

    const dict = raw as Record<string, unknown>;
    const roomName = dict.roomName as string;
    if (typeof roomName !== 'string' || roomName.trim() === '') {
        throw new Error('VoIP incoming payload missing roomName');
    }
    return roomName;
};

const useVoIPEventsIos = (handlers: VoIPEventHandlers): void => {
    // Keep the latest handlers in a ref so the subscription stays stable across
    // renders even when callers pass an inline object.
    const handlersRef = useRef(handlers);
    handlersRef.current = handlers;
    const listener = useRef({});

    useCallKitEvent('answer', () => handlersRef.current.onAnswered?.());
    useCallKitEvent('ended', () => {
        clearPendingIncomingCall();
        handlersRef.current.onEnded?.();
    });

    useEffect(() => {
        // VoIP push events (registered / incoming) arrive on the VoIP push channel.
        addListener(listener.current, 'voipPushEvent', (event) => {
            if (!event || typeof event !== 'object') {
                return;
            }
            const payload = event as Record<string, unknown>;
            if ('registered' in payload) {
                handlersRef.current.onRegistered?.(
                    payload.registered as string,
                );
            }
            if ('incoming' in payload) {
                assertRoomName(payload.incoming);

                handlersRef.current.onIncoming?.(
                    payload.incoming as VoipIncomingPayload,
                );
            }
        });

        getVoipToken().then((token) => {
            if (token) {
                handlersRef.current.onRegistered?.(token);
            }
        });

        const pendingCall = getPendingIncomingCall();
        if (pendingCall && hasActiveCallKitSession()) {
            try {
                assertRoomName(pendingCall);
                handlersRef.current.onIncoming?.(
                    pendingCall as unknown as VoipIncomingPayload,
                );

                // The user may have accepted before JS was
                // ready, so the live onAnswered was missed. Recover it from
                // native state so the call connects instead of staying stuck.
                if (isCallAnswered()) {
                    handlersRef.current.onAnswered?.();
                }
            } catch {
                // Ignore a malformed buffered payload.
            }
        }

        return () => {
            removeListener(listener.current);
        };
    }, []);
};

const useVoIPEventsAndroid = (handlers: VoIPEventHandlers): void => {
    const handlersRef = useRef(handlers);
    handlersRef.current = handlers;
    const listener = useRef({});

    useEffect(() => {
        addListener(listener.current, 'telecomActionPerformed', (event) => {
            if (!event || typeof event !== 'object') {
                return;
            }
            const payload = event as { event?: string };
            if (payload.event === 'answer') {
                handlersRef.current.onAnswered?.();
            } else if (payload.event === 'ended') {
                clearPendingIncomingCall();
                handlersRef.current.onEnded?.();
            }
        });

        addListener(listener.current, 'voipPushEvent', (event) => {
            if (!event || typeof event !== 'object') {
                return;
            }
            const payload = event as Record<string, unknown>;
            if ('registered' in payload) {
                handlersRef.current.onRegistered?.(
                    payload.registered as string,
                );
            }
            if ('incoming' in payload) {
                assertRoomName(payload.incoming);

                handlersRef.current.onIncoming?.(
                    payload.incoming as VoipIncomingPayload,
                );
            }
        });

        getVoipToken().then((token) => {
            if (token) {
                handlersRef.current.onRegistered?.(token);
            }
        });

        const pendingCall = getPendingIncomingCall();
        if (pendingCall && hasActiveTelecomCall()) {
            try {
                assertRoomName(pendingCall);
                handlersRef.current.onIncoming?.(
                    pendingCall as unknown as VoipIncomingPayload,
                );

                // The user may have accepted before JS was
                // ready, so the live onAnswered was missed. Recover it from
                // native state so the call connects instead of staying stuck.
                if (isTelecomCallAnswered()) {
                    handlersRef.current.onAnswered?.();
                }
            } catch {
                // Ignore a malformed buffered payload.
            }
        }

        return () => {
            removeListener(listener.current);
        };
    }, []);
};

export const useVoIPEvents = Platform.select({
    ios: useVoIPEventsIos,
    android: useVoIPEventsAndroid,
}) as typeof useVoIPEventsIos;
