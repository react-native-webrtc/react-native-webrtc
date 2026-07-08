import { NativeModules, Platform } from 'react-native';
const { WebRTCModule } = NativeModules;

if (WebRTCModule === null) {
    throw new Error(
        `WebRTC native module not found.\n${
            Platform.OS === 'ios'
                ? 'Try executing the "pod install" command inside your projects ios folder.'
                : 'Try executing the "npm install" command inside your projects folder.'
        }`,
    );
}

import {
    startAudioExtraction,
    type AudioExtractionOptions,
    type AudioTrackData,
} from './AudioExtraction';
import { type CallKitAction, type CallKitConfig } from './CallKit';
import { setupNativeEvents } from './EventEmitter';
import Logger from './Logger';
import mediaDevices from './MediaDevices';
import MediaStream from './MediaStream';
import MediaStreamTrack, { type MediaTrackSettings } from './MediaStreamTrack';
import MediaStreamTrackEvent from './MediaStreamTrackEvent';
import permissions from './Permissions';
import {
    clearPendingIncomingCall,
    getPendingIncomingCall,
    getVoipToken,
} from './PushKit';
import RTCAudioSession from './RTCAudioSession';
import RTCCertificate from './RTCCertificate';
import RTCErrorEvent from './RTCErrorEvent';
import RTCIceCandidate from './RTCIceCandidate';
import RTCPIPView, {
    startPIP,
    stopPIP,
    type RTCPIPViewProps,
} from './RTCPIPView';
import RTCPeerConnection from './RTCPeerConnection';
import RTCRtpEncodingParameters, {
    type RTCRtpEncodingParametersInit,
} from './RTCRtpEncodingParameters';
import RTCRtpReceiver from './RTCRtpReceiver';
import RTCRtpSendParameters, {
    type RTCRtpSendParametersInit,
} from './RTCRtpSendParameters';
import RTCRtpSender from './RTCRtpSender';
import RTCRtpTransceiver from './RTCRtpTransceiver';
import RTCSessionDescription from './RTCSessionDescription';
import RTCView, { type RTCPIPOptions, type RTCVideoViewProps } from './RTCView';
import ScreenCapturePickerView from './ScreenCapturePickerView';
import {
    AudioDeviceType,
    AudioOutputManager,
    type AudioDevice,
    type AudioOutputChangedInfo,
} from './audioOutputManager';
import {
    createCustomVideoBufferPool,
    createCustomVideoTrack,
    pushFrame,
    forwardFrame,
    type CustomVideoBufferPoolInit,
    type CustomVideoBufferPool,
    type CustomVideoBuffer,
    type CustomVideoFrameFence,
    type CustomVideoSink,
    type PooledTrack,
    type ForwardTrack,
    type CustomVideoTrackResult,
    type PushFrameArgs,
    type ForwardFrameArgs,
} from './createCustomVideoTrack';
import presentBroadcastPicker from './presentBroadcastPicker';
import presentLivestreamBroadcastPicker from './presentLivestreamBroadcastPicker';
import { useAudioOutput, type UseAudioOutputResult } from './useAudioOutput';
import { useCallKit, useCallKitEvent, useCallKitService } from './useCallKit';
import {
    useForegroundService,
    type ForegroundServiceConfig,
} from './useForegroundService';
import {
    useLivestreamStatus,
    type LivestreamStatus,
    type LivestreamStatusInfo,
} from './useLivestreamStatus';
import {
    useVoIPEvents,
    type VoIPEventHandlers,
    type VoipIncomingPayload,
} from './useVoIPEvents';
import { Event, EventTarget } from './vendor/event-target-shim';
import writeLivestreamCredentials, {
    type LivestreamCredentials,
} from './writeLivestreamCredentials';

Logger.enable(`${Logger.ROOT_PREFIX}:*`);

// Add listeners for the native events early, since they are added asynchronously.
setupNativeEvents();

export {
    AudioDeviceType,
    AudioOutputManager,
    clearPendingIncomingCall,
    createCustomVideoBufferPool,
    createCustomVideoTrack,
    Event,
    EventTarget,
    forwardFrame,
    getPendingIncomingCall,
    getVoipToken,
    mediaDevices,
    MediaStream,
    MediaStreamTrack,
    permissions,
    presentBroadcastPicker,
    presentLivestreamBroadcastPicker,
    pushFrame,
    registerGlobals,
    RTCAudioSession,
    RTCCertificate,
    RTCErrorEvent,
    RTCIceCandidate,
    RTCPeerConnection,
    RTCPIPView,
    RTCRtpEncodingParameters,
    RTCRtpReceiver,
    RTCRtpSender,
    RTCRtpSendParameters,
    RTCRtpTransceiver,
    RTCSessionDescription,
    RTCView,
    ScreenCapturePickerView,
    startAudioExtraction,
    startPIP,
    stopPIP,
    useAudioOutput,
    useCallKit,
    useCallKitEvent,
    useCallKitService,
    useForegroundService,
    useLivestreamStatus,
    useVoIPEvents,
    writeLivestreamCredentials,
    type AudioDevice,
    type AudioExtractionOptions,
    type AudioOutputChangedInfo,
    type AudioTrackData,
    type CallKitAction,
    type CallKitConfig,
    type CustomVideoBuffer,
    type CustomVideoBufferPool,
    type CustomVideoBufferPoolInit,
    type CustomVideoFrameFence,
    type CustomVideoSink,
    type CustomVideoTrackResult,
    type ForegroundServiceConfig,
    type ForwardFrameArgs,
    type ForwardTrack,
    type LivestreamCredentials,
    type LivestreamStatus,
    type LivestreamStatusInfo,
    type MediaTrackSettings,
    type PooledTrack,
    type PushFrameArgs,
    type RTCPIPOptions,
    type RTCPIPViewProps,
    type RTCRtpEncodingParametersInit,
    type RTCRtpSendParametersInit,
    type RTCVideoViewProps,
    type UseAudioOutputResult,
    type VoIPEventHandlers,
    type VoipIncomingPayload,
};

declare const global: any;

function registerGlobals(): void {
    // Should not happen. React Native has a global navigator object.
    if (typeof global.navigator !== 'object') {
        throw new Error('navigator is not an object');
    }

    if (!global.navigator.mediaDevices) {
        global.navigator.mediaDevices = {};
    }

    global.navigator.mediaDevices.getUserMedia =
        mediaDevices.getUserMedia.bind(mediaDevices);
    global.navigator.mediaDevices.getDisplayMedia =
        mediaDevices.getDisplayMedia.bind(mediaDevices);
    global.navigator.mediaDevices.enumerateDevices =
        mediaDevices.enumerateDevices.bind(mediaDevices);

    global.RTCIceCandidate = RTCIceCandidate;
    global.RTCCertificate = RTCCertificate;
    global.RTCPeerConnection = RTCPeerConnection;
    global.RTCRtpReceiver = RTCRtpReceiver;
    global.RTCRtpSender = RTCRtpReceiver;
    global.RTCSessionDescription = RTCSessionDescription;
    global.MediaStream = MediaStream;
    global.MediaStreamTrack = MediaStreamTrack;
    global.MediaStreamTrackEvent = MediaStreamTrackEvent;
    global.RTCRtpTransceiver = RTCRtpTransceiver;
    global.RTCRtpReceiver = RTCRtpReceiver;
    global.RTCRtpSender = RTCRtpSender;
    global.RTCErrorEvent = RTCErrorEvent;
}
