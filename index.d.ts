// Type definitions for react-native-webrtc 106 with additional Daily type

import mediaDevices from './lib/typescript/MediaDevices';
import MediaStream from './lib/typescript/MediaStream';
import MediaStreamTrack from './lib/typescript/MediaStreamTrack';
import permissions from './lib/typescript/Permissions';
import RTCErrorEvent from './lib/typescript/RTCErrorEvent';
import RTCIceCandidate from './lib/typescript/RTCIceCandidate';
import RTCPeerConnection from './lib/typescript/RTCPeerConnection';
import RTCRtpReceiver from './lib/typescript/RTCRtpReceiver';
import RTCRtpSender from './lib/typescript/RTCRtpSender';
import RTCRtpTransceiver from './lib/typescript/RTCRtpTransceiver';
import RTCSessionDescription from './lib/typescript/RTCSessionDescription';
import RTCView from './lib/typescript/RTCView';
import ScreenCapturePickerView from './lib/typescript/ScreenCapturePickerView';
import { ViewStyle } from 'react-native';
export { RTCIceCandidate, RTCPeerConnection, RTCSessionDescription, RTCView, ScreenCapturePickerView, RTCRtpTransceiver, RTCRtpReceiver, RTCRtpSender, RTCErrorEvent, MediaStream, MediaStreamTrack, mediaDevices, permissions, registerGlobals };
declare function registerGlobals(): void;

// Daily types
export interface RTCViewProps {
  streamURL: string;
  mirror?: boolean | undefined;
  zOrder?: number | undefined;
  objectFit?: 'contain' | 'cover' | undefined;
  style?: ViewStyle | undefined;
}

export interface MandatoryMedia {
  minWidth: number;
  minHeight: number;
  minFrameRate: number;
}

export interface MediaSources {
  sourceId: string;
}

export interface MediaTrackConstraints {
  mandatory?: MandatoryMedia;
  width?: number;
  height?: number;
  frameRate?: number;
  facingMode?: 'user' | 'environment';
  deviceId?: string;
  optional?: MediaSources[];
}

export type MediaDeviceKind = 'audio' | 'videoinput';

export class MediaDeviceInfo {
  readonly deviceId: string;
  readonly groupId: string;
  readonly kind: MediaDeviceKind;
  readonly label: string;
  toJSON(): any;
}


