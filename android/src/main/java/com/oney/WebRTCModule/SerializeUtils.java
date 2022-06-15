package com.oney.WebRTCModule;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.webrtc.*;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;

public class SerializeUtils {
  
    /**
     * Serialization APIs
     */

    public static ReadableMap serializeVideoCodecInfo(VideoCodecInfo info) {
        WritableMap params = Arguments.createMap();
        params.putString("mimeType", "video/" + info.name);
        return params;
    }

    public static ReadableMap serializeStream(int id, String streamReactTag, MediaStream stream) {

        WritableMap params = Arguments.createMap();
        params.putInt("id", id);
        params.putString("streamId", stream.getId());
        params.putString("streamReactTag", streamReactTag);

        WritableArray tracks = Arguments.createArray();

        for (VideoTrack track: stream.videoTracks) {
            tracks.pushMap(SerializeUtils.serializeTrack(id, track));
        }
        for (AudioTrack track: stream.audioTracks) {
            tracks.pushMap(SerializeUtils.serializeTrack(id, track));
        }

        params.putArray("tracks", tracks);

        return params;
    }

    public static String serializeDirection(RtpTransceiver.RtpTransceiverDirection src) {
        switch(src) {
            case INACTIVE:
                return "inactive";
            case RECV_ONLY:
                return "recvonly";
            case SEND_ONLY:
                return "sendonly";
            case SEND_RECV:
                return "sendrecv";
            default:
                throw new Error("Invalid direction");
        }
    }

    public static ReadableMap serializeTrack(int id, MediaStreamTrack track) {
        WritableMap trackInfo = Arguments.createMap();
        trackInfo.putString("id", track.id());
        trackInfo.putInt("peerConnectionId", id);
        trackInfo.putString("kind", track.kind());
        trackInfo.putBoolean("enabled", track.enabled());
        trackInfo.putString("readyState", track.state().toString().toLowerCase());
        trackInfo.putBoolean("remote", true);
        return trackInfo;
    }

    /**
     * This method is currently missing serializing DtmfSender
     * and transport.
     * TODO: Add transport and dtmf fields to the serialized sender to match the web APIs.
     */
    public static ReadableMap serializeSender(int id, RtpSender sender) {
        WritableMap res = Arguments.createMap();
        res.putString("id", sender.id());
        res.putInt("peerConnectionId", id);
        if (sender.track() != null) {
            res.putMap("track", SerializeUtils.serializeTrack(id, sender.track()));
        }
        return res;
    }

    public static ReadableMap serializeReceiver(int id, RtpReceiver receiver) {
        WritableMap res = Arguments.createMap();
        res.putString("id", receiver.id());
        res.putInt("peerConnectionId", id);
        res.putMap("track", SerializeUtils.serializeTrack(id, receiver.track()));
        return res;
    }

    public static ReadableMap serializeTransceiver(int id, RtpTransceiver transceiver) {
        WritableMap res = Arguments.createMap();
        res.putString("id", transceiver.getSender().id());
        res.putInt("peerConnectionId", id);
        String mid = transceiver.getMid();
        res.putString("mid", mid);
        res.putString("direction", serializeDirection(transceiver.getDirection()));
        RtpTransceiver.RtpTransceiverDirection currentDirection = transceiver.getCurrentDirection();
        if (currentDirection != null) {
            res.putString("currentDirection", SerializeUtils.serializeDirection(transceiver.getCurrentDirection()));
        }
        res.putBoolean("isStopped", transceiver.isStopped());
        res.putMap("receiver", SerializeUtils.serializeReceiver(id, transceiver.getReceiver()));
        res.putMap("sender", SerializeUtils.serializeSender(id, transceiver.getSender()));
        return res;
    }

    /**
     * Parsing APIs
     */

    public static MediaStreamTrack.MediaType parseMediaType(String type) {
        switch(type) {
            case "audio": 
                return MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO;
            case "video":
                return MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO;
            default:
                throw new Error("Unknown media type");
        }
    }
    
    public static RtpTransceiver.RtpTransceiverDirection parseDirection(String src) {
        switch (src) {
            case "sendrecv":
                return RtpTransceiver.RtpTransceiverDirection.SEND_RECV;
            case "sendonly":
                return RtpTransceiver.RtpTransceiverDirection.SEND_ONLY;
            case "recvonly":
                return RtpTransceiver.RtpTransceiverDirection.RECV_ONLY;
            case "inactive":
                return RtpTransceiver.RtpTransceiverDirection.INACTIVE;
        }
        throw new Error("Invalid direction");
    }

    public static RtpTransceiver.RtpTransceiverInit parseTransceiverOptions(ReadableMap map) {
        RtpTransceiver.RtpTransceiverDirection direction = RtpTransceiver.RtpTransceiverDirection.INACTIVE;
        ArrayList<String> streamIds = new ArrayList<>();
        if (map != null) {
            if (map.hasKey("direction")) {
                String directionRaw = map.getString("direction");
                if (directionRaw != null) {
                    direction = SerializeUtils.parseDirection(directionRaw);
                }
            }
            if (map.hasKey("streamIds")) {
                ReadableArray rawStreamIds = map.getArray("streamIds");
                if (rawStreamIds != null) {
                    for (int i = 0; i < rawStreamIds.size(); i++) {
                        streamIds.add(rawStreamIds.getString(i));
                    }
                }
            }
        }

        return new RtpTransceiver.RtpTransceiverInit(direction, streamIds);
    }

}
