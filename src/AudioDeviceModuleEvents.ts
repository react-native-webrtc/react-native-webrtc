import { NativeModules, Platform } from 'react-native';

import { addListener, removeListener } from './EventEmitter';

const { WebRTCModule } = NativeModules;

export type SpeechActivityEvent = 'started' | 'ended';

export interface SpeechActivityEventData {
  event: SpeechActivityEvent;
}

export interface EngineStateEventData {
  isPlayoutEnabled: boolean;
  isRecordingEnabled: boolean;
}

/**
 * Raw native event payload. Every engine event carries a `requestId` that must
 * be echoed back to the matching resolve call so the native side can drop a
 * response from a round that already timed out. This id is internal and is not
 * surfaced to app-registered handlers.
 */
interface EngineEventPayload {
  requestId: number;
}

interface EngineStateEventPayload extends EngineEventPayload, EngineStateEventData {}

export type AudioDeviceModuleEventType =
  | 'speechActivity'
  | 'devicesUpdated';

export type AudioDeviceModuleEventData =
  | SpeechActivityEventData
  | EngineStateEventData
  | Record<string, never>; // Empty object for events with no data

export type AudioDeviceModuleEventListener = (data: AudioDeviceModuleEventData) => void;

/**
 * Handler function that must return a number (0 for success, non-zero for error)
 */
export type AudioEngineEventNoParamsHandler = () => Promise<void>;
export type AudioEngineEventHandler = (params: {
    isPlayoutEnabled: boolean;
    isRecordingEnabled: boolean;
}) => Promise<void>;

/**
 * Event emitter for RTCAudioDeviceModule delegate callbacks.
 * iOS/macOS only.
 */
class AudioDeviceModuleEventEmitter {
    private engineCreatedHandler: AudioEngineEventNoParamsHandler | null = null;
    private willEnableEngineHandler: AudioEngineEventHandler | null = null;
    private willStartEngineHandler: AudioEngineEventHandler | null = null;
    private didStopEngineHandler: AudioEngineEventHandler | null = null;
    private didDisableEngineHandler: AudioEngineEventHandler | null = null;
    private willReleaseEngineHandler: AudioEngineEventNoParamsHandler | null = null;

    public setupListeners() {
        if (Platform.OS !== 'android' && WebRTCModule) {
            // Setup handlers for blocking delegate methods
            addListener(this, 'audioDeviceModuleEngineCreated', async (event: unknown) => {
                const { requestId } = event as EngineEventPayload;
                let result = 0;

                if (this.engineCreatedHandler) {
                    try {
                        await this.engineCreatedHandler();
                    } catch (error) {
                        // If error is a number, use it as the error code, otherwise use -1
                        result = typeof error === 'number' ? error : -1;
                    }
                }

                WebRTCModule.audioDeviceModuleResolveEngineCreated(requestId, result);
            });

            addListener(
                this,
                'audioDeviceModuleEngineWillEnable',
                async (event: unknown) => {
                    const { requestId, isPlayoutEnabled, isRecordingEnabled } = event as EngineStateEventPayload;
                    let result = 0;

                    if (this.willEnableEngineHandler) {
                        try {
                            await this.willEnableEngineHandler({ isPlayoutEnabled, isRecordingEnabled });
                        } catch (error) {
                            // If error is a number, use it as the error code, otherwise use -1
                            result = typeof error === 'number' ? error : -1;
                        }
                    }

                    WebRTCModule.audioDeviceModuleResolveWillEnableEngine(requestId, result);
                },
            );

            addListener(
                this,
                'audioDeviceModuleEngineWillStart',
                async (event: unknown) => {
                    const { requestId, isPlayoutEnabled, isRecordingEnabled } = event as EngineStateEventPayload;
                    let result = 0;

                    if (this.willStartEngineHandler) {
                        try {
                            await this.willStartEngineHandler({ isPlayoutEnabled, isRecordingEnabled });
                        } catch (error) {
                            // If error is a number, use it as the error code, otherwise use -1
                            result = typeof error === 'number' ? error : -1;
                        }
                    }

                    WebRTCModule.audioDeviceModuleResolveWillStartEngine(requestId, result);
                },
            );

            addListener(
                this,
                'audioDeviceModuleEngineDidStop',
                async (event: unknown) => {
                    const { requestId, isPlayoutEnabled, isRecordingEnabled } = event as EngineStateEventPayload;
                    let result = 0;

                    if (this.didStopEngineHandler) {
                        try {
                            await this.didStopEngineHandler({ isPlayoutEnabled, isRecordingEnabled });
                        } catch (error) {
                            // If error is a number, use it as the error code, otherwise use -1
                            result = typeof error === 'number' ? error : -1;
                        }
                    }

                    WebRTCModule.audioDeviceModuleResolveDidStopEngine(requestId, result);
                },
            );

            addListener(
                this,
                'audioDeviceModuleEngineDidDisable',
                async (event: unknown) => {
                    const { requestId, isPlayoutEnabled, isRecordingEnabled } = event as EngineStateEventPayload;
                    let result = 0;

                    if (this.didDisableEngineHandler) {
                        try {
                            await this.didDisableEngineHandler({ isPlayoutEnabled, isRecordingEnabled });
                        } catch (error) {
                            // If error is a number, use it as the error code, otherwise use -1
                            result = typeof error === 'number' ? error : -1;
                        }
                    }

                    WebRTCModule.audioDeviceModuleResolveDidDisableEngine(requestId, result);
                },
            );

            addListener(this, 'audioDeviceModuleEngineWillRelease', async (event: unknown) => {
                const { requestId } = event as EngineEventPayload;
                let result = 0;

                if (this.willReleaseEngineHandler) {
                    try {
                        await this.willReleaseEngineHandler();
                    } catch (error) {
                        // If error is a number, use it as the error code, otherwise use -1
                        result = typeof error === 'number' ? error : -1;
                    }
                }

                WebRTCModule.audioDeviceModuleResolveWillReleaseEngine(requestId, result);
            });
        }
    }

    /**
     * Subscribe to speech activity events (started/ended)
     */
    addSpeechActivityListener(listener: (data: SpeechActivityEventData) => void) {
        addListener(listener, 'audioDeviceModuleSpeechActivity', listener as (event: unknown) => void);
    }

    /**
     * Remove a previously registered speech activity listener
     */
    removeSpeechActivityListener(listener: (data: SpeechActivityEventData) => void) {
        removeListener(listener);
    }

    /**
     * Subscribe to devices updated event (input/output devices changed)
     */
    addDevicesUpdatedListener(listener: () => void) {
        addListener(listener, 'audioDeviceModuleDevicesUpdated', listener as (event: unknown) => void);
    }

    /**
     * Remove a previously registered devices updated listener
     */
    removeDevicesUpdatedListener(listener: () => void) {
        removeListener(listener);
    }

    /**
     * Set handler for engine created delegate - MUST return 0 for success or error code
     * This handler blocks the native thread until it returns, throw to cancel audio engine's operation
     */
    setEngineCreatedHandler(handler: AudioEngineEventNoParamsHandler | null) {
        this.engineCreatedHandler = handler;
    }

    /**
     * Set handler for will enable engine delegate - MUST return 0 for success or error code
     * This handler blocks the native thread until it returns, throw to cancel audio engine's operation
     */
    setWillEnableEngineHandler(handler: AudioEngineEventHandler | null) {
        this.willEnableEngineHandler = handler;
    }

    /**
     * Set handler for will start engine delegate - MUST return 0 for success or error code
     * This handler blocks the native thread until it returns, throw to cancel audio engine's operation
     */
    setWillStartEngineHandler(handler: AudioEngineEventHandler | null) {
        this.willStartEngineHandler = handler;
    }

    /**
     * Set handler for did stop engine delegate - MUST return 0 for success or error code
     * This handler blocks the native thread until it returns, throw to cancel audio engine's operation
     */
    setDidStopEngineHandler(handler: AudioEngineEventHandler | null) {
        this.didStopEngineHandler = handler;
    }

    /**
     * Set handler for did disable engine delegate - MUST return 0 for success or error code
     * This handler blocks the native thread until it returns, throw to cancel audio engine's operation
     */
    setDidDisableEngineHandler(handler: AudioEngineEventHandler | null) {
        this.didDisableEngineHandler = handler;
    }

    /**
     * Set handler for will release engine delegate
     * This handler blocks the native thread until it returns, throw to cancel audio engine's operation
     */
    setWillReleaseEngineHandler(handler: AudioEngineEventNoParamsHandler | null) {
        this.willReleaseEngineHandler = handler;
    }
}

export const audioDeviceModuleEvents = new AudioDeviceModuleEventEmitter();
