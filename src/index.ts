import RTCIceCandidate from './RTCIceCandidate';
import RTCPeerConnection from './RTCPeerConnection';
import RTCRtpReceiver from './RTCRtpReceiver';
import RTCRtpSender from './RTCRtpSender';
import RTCSessionDescription from './RTCSessionDescription';
import RTCView from './RTCView';
import ScreenCapturePickerView from './ScreenCapturePickerView';
import MediaStream from './MediaStream';
import MediaStreamTrack from './MediaStreamTrack';
import mediaDevices from './MediaDevices';
import permissions from './Permissions';

export {
    MediaStream,
    MediaStreamTrack,
    RTCIceCandidate,
    RTCPeerConnection,
    RTCRtpReceiver,
    RTCRtpSender,
    RTCSessionDescription,
    RTCView,
    ScreenCapturePickerView,
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
}
