import { NativeEventEmitter, type NativeModule } from 'react-native';

import WebRTCModule from './NativeWebRTCModule';

// --- Custom EventEmitter (replaces private RN import) ---
interface Subscription {
    remove(): void;
}

type Handler = (...args: unknown[]) => void;

class Emitter {
    private listeners = new Map<string, Set<Handler>>();

    on(event: string, handler: Handler): Subscription {
        if (!this.listeners.has(event)) {
            this.listeners.set(event, new Set());
        }

        this.listeners.get(event)?.add(handler);

        return {
            remove: () => {
                this.listeners.get(event)?.delete(handler);
            },
        };
    }

    emit(event: string, ...args: unknown[]): void {
        this.listeners.get(event)?.forEach(h => h(...args));
    }
}

// --- Native events setup ---
const NATIVE_EVENTS = [
    'peerConnectionSignalingStateChanged',
    'peerConnectionStateChanged',
    'peerConnectionOnRenegotiationNeeded',
    'peerConnectionIceConnectionChanged',
    'peerConnectionIceGatheringChanged',
    'peerConnectionGotICECandidate',
    'peerConnectionDidOpenDataChannel',
    'peerConnectionOnRemoveTrack',
    'peerConnectionOnTrack',
    'dataChannelStateChanged',
    'dataChannelReceiveMessage',
    'dataChannelDidChangeBufferedAmount',
    'mediaStreamTrackMuteChanged',
    'mediaStreamTrackEnded',
];

const emitter = new Emitter();

export function setupNativeEvents(): void {
    const module = WebRTCModule as unknown as Record<string, unknown>;
    const isNewArch = typeof module.peerConnectionOnRenegotiationNeeded === 'function';

    for (const event of NATIVE_EVENTS) {
        if (isNewArch) {
            // New architecture: codegen emits directly to AsyncEventEmitter
            (module[event] as (fn: Handler) => { remove(): void })(ev => emitter.emit(event, ev));
        } else {
            // Old architecture: NativeEventEmitter delivers via RCTDeviceEventEmitter
            new NativeEventEmitter(WebRTCModule as NativeModule).addListener(event, (...args) =>
                emitter.emit(event, ...args),
            );
        }
    }
}

// --- Public API (used by RTCPeerConnection, RTCDataChannel, etc.) ---
const subscriptions = new Map<object, Subscription[]>();

export function addListener(owner: object, event: string, handler: Handler): void {
    if (!NATIVE_EVENTS.includes(event)) {
        throw new Error(`Unknown event: ${event}`);
    }

    if (!subscriptions.has(owner)) {
        subscriptions.set(owner, []);
    }

    const subs = subscriptions.get(owner);

    if (subs) {
        subs.push(emitter.on(event, handler));
    }
}

export function removeListener(owner: object): void {
    subscriptions.get(owner)?.forEach(s => s.remove());
    subscriptions.delete(owner);
}
