package com.oney.WebRTCModule;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import org.webrtc.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SerializeUtils {
    /**
     * Serialization APIs
     */

    public static ReadableMap serializeVideoCodecInfo(VideoCodecInfo info) {
        WritableMap params = Arguments.createMap();
        params.putString("mimeType", "video/" + info.name);
        return params;
    }

    public static ReadableMap serializeStream(int pcId, String streamReactTag, MediaStream stream) {
        WritableMap params = Arguments.createMap();
        params.putString("streamId", stream.getId());
        params.putString("streamReactTag", streamReactTag);

        WritableArray tracks = Arguments.createArray();

        for (VideoTrack track : stream.videoTracks) {
            tracks.pushMap(SerializeUtils.serializeTrack(pcId, track));
        }
        for (AudioTrack track : stream.audioTracks) {
            tracks.pushMap(SerializeUtils.serializeTrack(pcId, track));
        }

        params.putArray("tracks", tracks);

        return params;
    }

    public static String serializeDirection(RtpTransceiver.RtpTransceiverDirection src) {
        switch (src) {
            case INACTIVE:
                return "inactive";
            case RECV_ONLY:
                return "recvonly";
            case SEND_ONLY:
                return "sendonly";
            case SEND_RECV:
                return "sendrecv";
            case STOPPED:
                return "stopped";
            default:
                throw new Error("Invalid direction");
        }
    }

    public static ReadableMap serializeTrack(int pcId, MediaStreamTrack track) {
        WritableMap trackInfo = Arguments.createMap();
        trackInfo.putString("id", track.id());
        trackInfo.putInt("peerConnectionId", pcId);
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
        res.putMap("rtpParameters", SerializeUtils.serializeRtpParameters(sender.getParameters()));
        return res;
    }

    public static ReadableMap serializeReceiver(int id, RtpReceiver receiver) {
        WritableMap res = Arguments.createMap();
        res.putString("id", receiver.id());
        res.putInt("peerConnectionId", id);
        if (receiver.track() != null) {
            res.putMap("track", SerializeUtils.serializeTrack(id, receiver.track()));
        }
        res.putMap("rtpParameters", SerializeUtils.serializeRtpParameters(receiver.getParameters()));
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

    public static ReadableMap serializeRtpParameters(RtpParameters params) {
        WritableMap result = Arguments.createMap();
        WritableArray encodings = Arguments.createArray();
        WritableArray codecs = Arguments.createArray();
        WritableArray headerExtensions = Arguments.createArray();
        WritableMap rtcp = Arguments.createMap();

        // Preparing RTCP
        rtcp.putString("cname", params.getRtcp().getCname());
        rtcp.putBoolean("reducedSize", params.getRtcp().getReducedSize());

        // Preparing header extensions
        params.getHeaderExtensions().forEach(extension -> {
            WritableMap extensionMap = Arguments.createMap();
            extensionMap.putInt("id", extension.getId());
            extensionMap.putString("uri", extension.getUri());
            extensionMap.putBoolean("encrypted", extension.getEncrypted());
            headerExtensions.pushMap(extensionMap);
        });

        // Preparing encodings
        params.encodings.forEach(encoding -> {
            WritableMap encodingMap = Arguments.createMap();
            encodingMap.putBoolean("active", encoding.active);
            if (encoding.rid != null) {
                encodingMap.putString("rid", encoding.rid);
            }
            // Since they return integer objects that are nullable,
            // while the map does not accept nullable integer values.
            if (encoding.maxBitrateBps != null) {
                encodingMap.putInt("maxBitrate", encoding.maxBitrateBps);
            }
            if (encoding.maxFramerate != null) {
                encodingMap.putInt("maxFramerate", encoding.maxFramerate);
            }
            if (encoding.scaleResolutionDownBy != null) {
                encodingMap.putDouble("scaleResolutionDownBy", encoding.scaleResolutionDownBy);
            }
            encodings.pushMap(encodingMap);
        });

        // Preparing codecs
        params.codecs.forEach(codec -> {
            WritableMap codecMap = Arguments.createMap();
            codecMap.putInt("payloadType", codec.payloadType);
            codecMap.putString("mimeType", codec.name);
            codecMap.putInt("clockRate", codec.clockRate);
            if (codec.numChannels != null) {
                codecMap.putInt("channels", codec.numChannels);
            }
            // Serializing sdpFmptLine.
            if (!codec.parameters.isEmpty()) {
                final String sdpFmptLineParams = codec.parameters.keySet()
                                                         .stream()
                                                         .map(key -> key + "=" + codec.parameters.get(key))
                                                         .collect(Collectors.joining(";"));
                codecMap.putString("sdpFmtpLine", sdpFmptLineParams);
            }

            codecs.pushMap(codecMap);
        });

        result.putString("transactionId", params.transactionId);
        result.putMap("rtcp", rtcp);
        result.putArray("encodings", encodings);
        result.putArray("codecs", codecs);
        result.putArray("headerExtensions", headerExtensions);
        if (params.degradationPreference != null) {
            result.putString("degradationPreference", params.degradationPreference.toString());
        }

        return result;
    }

    /**
     * Parsing APIs
     */

    public static RtpParameters updateRtpParameters(ReadableMap updateParams, RtpParameters rtpParams) {
        // Preparing encodings
        ReadableArray encodingsArray = updateParams.getArray("encodings");
        List<RtpParameters.Encoding> encodings = rtpParams.encodings;
        if (encodingsArray.size() != encodings.size()) {
            return null;
        }

        for (int i = 0; i < encodingsArray.size(); i++) {
            ReadableMap encodingUpdate = encodingsArray.getMap(i);
            RtpParameters.Encoding encoding = encodings.get(i);
            // Dealing with nullable Integers
            Integer maxBitrate = encodingUpdate.hasKey("maxBitrate") ? encodingUpdate.getInt("maxBitrate") : null;
            Integer maxFramerate = encodingUpdate.hasKey("maxFramerate") ? encodingUpdate.getInt("maxFramerate") : null;
            Double scaleResolutionDownBy = encodingUpdate.hasKey("scaleResolutionDownBy")
                    ? encodingUpdate.getDouble("scaleResolutionDownBy")
                    : null;

            encoding.active = encodingUpdate.getBoolean("active");
            encoding.rid = encodingUpdate.getString("rid");
            encoding.maxBitrateBps = maxBitrate;
            encoding.maxFramerate = maxFramerate;
            encoding.scaleResolutionDownBy = scaleResolutionDownBy;
        }

        if (updateParams.hasKey("degradationPreference")) {
            rtpParams.degradationPreference =
                    RtpParameters.DegradationPreference.valueOf(updateParams.getString("degradationPreference"));
        }

        return rtpParams;
    }

    public static MediaStreamTrack.MediaType parseMediaType(String type) {
        switch (type) {
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
                // Here we ignore the "stopped" direction because user code should
                // never set it.
        }
        throw new Error("Invalid direction");
    }

    private static RtpParameters.Encoding parseEncoding(ReadableMap params) {
        RtpParameters.Encoding encoding = new RtpParameters.Encoding(params.getString("rid"), true, 1.0);

        if (params.hasKey("active")) {
            encoding.active = params.getBoolean("active");
        }
        if (params.hasKey("maxBitrate")) {
            encoding.maxBitrateBps = params.getInt("maxBitrate");
        }
        if (params.hasKey("maxFramerate")) {
            encoding.maxFramerate = params.getInt("maxFramerate");
        }
        if (params.hasKey("scaleResolutionDownBy")) {
            encoding.scaleResolutionDownBy = params.getDouble("scaleResolutionDownBy");
        }

        return encoding;
    }

    public static RtpTransceiver.RtpTransceiverInit parseTransceiverOptions(ReadableMap map) {
        if (map == null) {
            return null;
        }

        RtpTransceiver.RtpTransceiverDirection direction = RtpTransceiver.RtpTransceiverDirection.SEND_RECV;
        List<String> streamIds = new ArrayList<>();
        List<RtpParameters.Encoding> sendEncodings = new ArrayList<>();

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
        if (map.hasKey("sendEncodings")) {
            ReadableArray encodingsArray = map.getArray("sendEncodings");
            if (encodingsArray != null) {
                for (int i = 0; i < encodingsArray.size(); i++) {
                    ReadableMap encoding = encodingsArray.getMap(i);
                    sendEncodings.add(SerializeUtils.parseEncoding(encoding));
                }
            }
        }

        return new RtpTransceiver.RtpTransceiverInit(direction, streamIds, sendEncodings);
    }
}
