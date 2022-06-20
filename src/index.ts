import RTCIceCandidate from './RTCIceCandidate';
import RTCPeerConnection from './RTCPeerConnection';
import RTCRtpReceiver from './RTCRtpReceiver';
import RTCRtpSender from './RTCRtpSender';
import RTCRtpTransceiver from './RTCRtpTransceiver';
import RTCRtpCapabilities from './RTCRtpCapabilities';
import RTCRtpCodecCapability from './RTCRtpCodecCapability';
import RTCRtpEncodingParameters from './RTCRtpEncodingParameters';
import RTCRtpCodecParameters from './RTCRtpCodecParameters';
import RTCRtpParameters from './RTCRtpParameters';
import RTCRtcpParameters from './RTCRtcpParameters';
import RTCRtpHeaderExtension from './RTCRtpHeaderExtension';
import RTCRtpSendParameters from './RTCRtpSendParameters';
import RTCRtpReceiveParameters from './RTCRtpReceiveParameters';
import RTCSessionDescription from './RTCSessionDescription';
import RTCErrorEvent from './RTCErrorEvent';
import RTCView from './RTCView';
import ScreenCapturePickerView from './ScreenCapturePickerView';
import MediaStream from './MediaStream';
import MediaStreamTrack from './MediaStreamTrack';
import mediaDevices from './MediaDevices';
import permissions from './Permissions';

export {
    RTCIceCandidate,
    RTCPeerConnection,
    RTCSessionDescription,
    RTCView,
    ScreenCapturePickerView,
    RTCRtpTransceiver,
    RTCRtpReceiver,
    RTCRtpSender,
    RTCErrorEvent,
    RTCRtpCapabilities,
    RTCRtpCodecCapability,
    RTCRtpCodecParameters,
    RTCRtpEncodingParameters,
    RTCRtpParameters,
    RTCRtpSendParameters,
    RTCRtpReceiveParameters,
    RTCRtcpParameters,
    RTCRtpHeaderExtension,
    MediaStream,
    MediaStreamTrack,
    mediaDevices,
    permissions,
    registerGlobals
};

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
    global.RTCPeerConnection = RTCPeerConnection;
    global.RTCRtpReceiver = RTCRtpReceiver;
    global.RTCRtpSender = RTCRtpReceiver;
    global.RTCSessionDescription = RTCSessionDescription;
    global.MediaStream = MediaStream;
    global.MediaStreamTrack = MediaStreamTrack;
    global.RTCRtpTransceiver = RTCRtpTransceiver;
    global.RTCRtpReceiver = RTCRtpReceiver;
    global.RTCRtpSender = RTCRtpSender;
    global.RTCErrorEvent = RTCErrorEvent;
    global.RTCRtpCapabilities = RTCRtpCapabilities;
    global.RTCRtpCodecCapability = RTCRtpCodecCapability;
    global.RTCRtpCodecParameters = RTCRtpCodecParameters;
    global.RTCRtpEncodingParameters = RTCRtpEncodingParameters;
    global.RTCRtpParameters = RTCRtpParameters;
    global.RTCRtpSendParameters = RTCRtpSendParameters;
    global.RTCRtpReceiverParameters = RTCRtpReceiveParameters;
    global.RTCRtcpParameters = RTCRtcpParameters;
    global.RTCRtpHeaderExtension = RTCRtpHeaderExtension;
}
