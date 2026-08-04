# Transcoding
[![CI](https://github.com/finnvoor/Transcoding/actions/workflows/CI.yml/badge.svg)](https://github.com/finnvoor/Transcoding/actions/workflows/CI.yml) [![](https://img.shields.io/endpoint?url=https%3A%2F%2Fswiftpackageindex.com%2Fapi%2Fpackages%2Ffinnvoor%2FTranscoding%2Fbadge%3Ftype%3Dplatforms)](https://swiftpackageindex.com/finnvoor/Transcoding) [![](https://img.shields.io/endpoint?url=https%3A%2F%2Fswiftpackageindex.com%2Fapi%2Fpackages%2Ffinnvoor%2FTranscoding%2Fbadge%3Ftype%3Dswift-versions)](https://swiftpackageindex.com/finnvoor/Transcoding)

A simple Swift package for video encoding and decoding with Annex-B adaptors optimized for transferring video over a network.

`Transcoding` is used for video encoding and decoding in [Castaway](https://finnvoorhees.com/castaway), an app that streams HDMI capture devices from an iPad or Mac to a nearby Vision Pro.

<img width="2802" alt="Example data flow" src="https://github.com/finnvoor/Transcoding/assets/8284016/3c0cb0dc-ebaf-4419-b43c-d47553ca9f06">

## Usage
### VideoEncoder
`VideoEncoder` is an object that takes `CMSampleBuffer`s containing `CVPixelBuffer`s and outputs a stream of `CMSampleBuffer`s containing `CMBlockBuffer`s containing compressed H264/HEVC data. `VideoEncoder` is initialized with a `Config`, with presets for live capture, active transcoding, background transcoding, and ultra-low latency following Apple's recommendations.

#### Usage
```swift
let videoEncoder = VideoEncoder(config: .ultraLowLatency)
encoderStreamTask = Task {
    for await encodedSampleBuffer in videoEncoder.encodedSampleBuffers {
        // encodedSampleBuffer: CMSampleBuffer > CMBlockBuffer
    }
}
videoEncoder.encode(sampleBuffer)
```

### VideoDecoder
`VideoDecoder` is an object that takes `CMSampleBuffer`s containing `CMBlockBuffers`s containing compressed H264/HEVC data and outputs a stream of `CMSampleBuffer`s containing `CVPixelBuffer`s. `VideoDecoder` is initialized with a `Config` containing various optional decompression settings.

#### Usage
```swift
let videoDecoder = VideoDecoder(config: .init(realTime: true))
decoderStreamTask = Task {
    for await decodedSampleBuffer in videoDecoder.decodedSampleBuffers {
        // decodedSampleBuffer: CMSampleBuffer > CVPixelBuffer
    }
}
videoDecoder.decode(sampleBuffer)
```

### Annex B
`VideoEncoderAnnexBAdaptor` and `VideoDecoderAnnexBAdaptor` can be used to convert compressed `CMSampleBuffer`s to and from an Annex B (ITU-T-REC-H.265) byte stream. This is ideal for sending compressed video data over a network.

### Example Pipeline
In this example, video frames from a capture device are encoded and decoded as an Annex-B data stream optimized for low latency.
```swift
let videoEncoder = VideoEncoder(config: .ultraLowLatency)
let videoEncoderAnnexBAdaptor = VideoEncoderAnnexBAdaptor(
    videoEncoder: videoEncoder
)
let videoDecoder = VideoDecoder(config: .init(realTime: true))
let videoDecoderAnnexBAdaptor = VideoDecoderAnnexBAdaptor(
    videoDecoder: videoDecoder,
    codec: .hevc
)

videoEncoderTask = Task {
    for await data in videoEncoderAnnexBAdaptor.annexBData {
        // send data over network or whatever
    }
}

videoDecoderTask = Task {
    for await decodedSampleBuffer in videoDecoder.decodedSampleBuffers {
        // here you have a received decoded sample buffer with image buffer
    }
}

receivedMessageTask = Task {
    // Replace `realtimeStreaming.receivedMessages` with however you receive encoded data packets 
    for await (data, _) in realtimeStreaming.receivedMessages {
        videoDecoderAnnexBAdaptor.decode(data)
    }
}

captureSessionTask = Task {
    // Replace `captureSession.pixelBuffers` with your video data source
    for await pixelBuffer in captureSession.pixelBuffers {
        videoEncoder.encode(pixelBuffer)
    }
}
```

### Important Notes
- Currently `VideoDecoderAnnexBAdaptor` only supports decoding full NALU's, meaning if you are passing data over the network or some stream you must ensure you receive full video frame packets, not just an arbitrarily sized data stream. When using Network.framework, for example, you would use a custom `NWProtocolFramerImplementation` to receive individual messages.
- There are a number of instances where either the encoder or decoder needs to be reset during an application lifecycle. The encoder and decoder automatically handle resetting after an iOS app has been backgrounded, but you may need to handle other cases by calling `encoder/decoder.invalidate()`. For example, if you maintain one encoder and disconnect then reconnect from a peer/decoder, the encoder will need to be invalidated to ensure it sends over the H264/HEVC parameter sets again. Otherwise the decoder will not be able to decode frames, as the encoder is optimized to only send SPS/PPS/VPS when necessary.
