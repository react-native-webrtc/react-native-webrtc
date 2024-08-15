import { NativeModules } from 'react-native';

const isTurboModuleEnabled = global.__turboModuleProxy != null;

const WebRTC = isTurboModuleEnabled ? require('./NativeWebRTC').default : NativeModules.WebRTC;

export default WebRTC;