import { NativeModules, Platform } from 'react-native';
const { WebRTCModule } = NativeModules;

if (WebRTCModule === null) {
    throw new Error(`WebRTC native module not found.\n${Platform.OS === 'ios' ?
        'Try executing the "pod install" command inside your projects ios folder.' :
        'Try executing the "npm install" command inside your projects folder.'
    }`);
}

import {
    AudioDeviceModule,
    AudioEngineMuteMode,
    AudioEngineAvailability,
    type AutomaticAudioSessionConfiguration,
    type AutomaticAppleAudioConfiguration,
    type AutomaticAppleAudioCategory,
    type AutomaticAppleAudioMode,
    type AutomaticAppleAudioCategoryOption,
} from './AudioDeviceModule';
import { audioDeviceModuleEvents } from './AudioDeviceModuleEvents';
import { setupNativeEvents } from './EventEmitter';
import Logger from './Logger';
import mediaDevices from './MediaDevices';
import MediaStream from './MediaStream';
import MediaStreamTrack, { type MediaTrackSettings } from './MediaStreamTrack';
import MediaStreamTrackEvent from './MediaStreamTrackEvent';
import permissions from './Permissions';
import RTCAudioSession from './RTCAudioSession';
import RTCCertificate from './RTCCertificate';
import RTCDataPacketCryptor, { RTCEncryptedPacket } from './RTCDataPacketCryptor';
import RTCDataPacketCryptorFactory from './RTCDataPacketCryptorFactory';
import RTCErrorEvent from './RTCErrorEvent';
import RTCFrameCryptor, { RTCFrameCryptorState } from './RTCFrameCryptor';
import RTCFrameCryptorFactory, {
    RTCFrameCryptorAlgorithm, RTCKeyDerivationAlgorithm, RTCKeyProviderOptions,
} from './RTCFrameCryptorFactory';
import RTCIceCandidate from './RTCIceCandidate';
import RTCKeyProvider from './RTCKeyProvider';
import RTCPIPView, { startIOSPIP, stopIOSPIP } from './RTCPIPView';
import RTCPeerConnection from './RTCPeerConnection';
import RTCRtpEncodingParameters, { type RTCRtpEncodingParametersInit } from './RTCRtpEncodingParameters';
import RTCRtpReceiver from './RTCRtpReceiver';
import RTCRtpSendParameters, { type RTCRtpSendParametersInit } from './RTCRtpSendParameters';
import RTCRtpSender from './RTCRtpSender';
import RTCRtpTransceiver from './RTCRtpTransceiver';
import RTCSessionDescription from './RTCSessionDescription';
import RTCView, { type RTCVideoViewProps, type RTCIOSPIPOptions } from './RTCView';
import ScreenCapturePickerView from './ScreenCapturePickerView';
import { Event, EventTarget, getEventAttributeValue, setEventAttributeValue } from './vendor/event-target-shim';

Logger.enable(`${Logger.ROOT_PREFIX}:*`);

// Add listeners for the native events early, since they are added asynchronously.
setupNativeEvents();

export {
    Event,
    EventTarget,
    getEventAttributeValue,
    setEventAttributeValue,
    RTCIceCandidate,
    RTCPeerConnection,
    RTCSessionDescription,
    RTCCertificate,
    RTCView,
    RTCPIPView,
    ScreenCapturePickerView,
    RTCRtpEncodingParameters,
    RTCRtpTransceiver,
    RTCRtpReceiver,
    RTCRtpSender,
    RTCRtpSendParameters,
    RTCErrorEvent,
    RTCAudioSession,
    RTCDataPacketCryptor,
    RTCDataPacketCryptorFactory,
    RTCEncryptedPacket,
    RTCFrameCryptor,
    RTCFrameCryptorAlgorithm,
    RTCFrameCryptorState,
    RTCKeyDerivationAlgorithm,
    RTCFrameCryptorFactory,
    RTCKeyProvider,
    RTCKeyProviderOptions,
    MediaStream,
    MediaStreamTrack,
    type MediaTrackSettings,
    type RTCRtpEncodingParametersInit,
    type RTCRtpSendParametersInit,
    type RTCVideoViewProps,
    type RTCIOSPIPOptions,
    mediaDevices,
    permissions,
    registerGlobals,
    startIOSPIP,
    stopIOSPIP,
    AudioDeviceModule,
    AudioEngineMuteMode,
    AudioEngineAvailability,
    type AutomaticAudioSessionConfiguration,
    type AutomaticAppleAudioConfiguration,
    type AutomaticAppleAudioCategory,
    type AutomaticAppleAudioMode,
    type AutomaticAppleAudioCategoryOption,
    audioDeviceModuleEvents,
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

    global.navigator.mediaDevices.getUserMedia = mediaDevices.getUserMedia.bind(mediaDevices);
    global.navigator.mediaDevices.getDisplayMedia = mediaDevices.getDisplayMedia.bind(mediaDevices);
    global.navigator.mediaDevices.enumerateDevices = mediaDevices.enumerateDevices.bind(mediaDevices);

    global.RTCIceCandidate = RTCIceCandidate;
    global.RTCCertificate = RTCCertificate;
    global.RTCPeerConnection = RTCPeerConnection;
    global.RTCSessionDescription = RTCSessionDescription;
    global.MediaStream = MediaStream;
    global.MediaStreamTrack = MediaStreamTrack;
    global.MediaStreamTrackEvent = MediaStreamTrackEvent;
    global.RTCRtpTransceiver = RTCRtpTransceiver;
    global.RTCRtpReceiver = RTCRtpReceiver;
    global.RTCRtpSender = RTCRtpSender;
    global.RTCErrorEvent = RTCErrorEvent;

    // Ensure audioDeviceModuleEvents is initialized and event listeners are registered
    audioDeviceModuleEvents.setupListeners();
}
