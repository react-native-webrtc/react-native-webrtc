//
//  WebRTCAudioSessionConfiguration.swift
//  JiveWebRTCModule
//
//  Created by Robert Barclay on 10/26/16.
//
//

import AVFoundation

private var voipConfiguration      = WebRTCAudioSessionConfiguration.Defaults.voip
private var videoConfiguration     = WebRTCAudioSessionConfiguration.Defaults.video
private var playbackConfiguration  = WebRTCAudioSessionConfiguration.Defaults.playback

public struct WebRTCAudioSessionConfiguration: Equatable {
  
  public let category: AVAudioSession.Category
  public let categoryOptions: AVAudioSession.CategoryOptions
  public let mode: AVAudioSession.Mode
  public let sampleRate: Double
  public let ioBufferDuration: TimeInterval
  public let inputNumberOfChannels: Int
  public let outputNumberOfChannels: Int
  
  public static func setVoip(configuration: WebRTCAudioSessionConfiguration) {
    voipConfiguration = configuration
  }
  
  public static func voipAudioConfiguration() -> WebRTCAudioSessionConfiguration {
    return voipConfiguration
  }
  
  public static func setVideo(configuration: WebRTCAudioSessionConfiguration) {
    videoConfiguration = configuration
  }
  
  public static func videoAudioConfiguration() -> WebRTCAudioSessionConfiguration {
    return videoConfiguration
  }
  
  public static func setPlayback(configuration: WebRTCAudioSessionConfiguration) {
    playbackConfiguration = configuration
  }
  
  public static func playbackAudioConfiguration() -> WebRTCAudioSessionConfiguration {
    return playbackConfiguration
  }
  
  public static func == (lhs: WebRTCAudioSessionConfiguration, rhs: WebRTCAudioSessionConfiguration) -> Bool {
    return lhs.category == rhs.category &&
           lhs.categoryOptions == rhs.categoryOptions &&
           lhs.mode == rhs.mode &&
           lhs.sampleRate == rhs.sampleRate &&
           lhs.ioBufferDuration == rhs.ioBufferDuration &&
           lhs.inputNumberOfChannels == rhs.inputNumberOfChannels &&
           lhs.outputNumberOfChannels == rhs.outputNumberOfChannels
  }
  
  public struct Defaults {
    public static var voip: WebRTCAudioSessionConfiguration {
      let device = deviceInfo
      return WebRTCAudioSessionConfiguration(
        category: PreferredCategory,
        categoryOptions: PreferredVOIPCategoryMode,
        mode: PreferredVOIPMode,
        sampleRate: device.sampleRate,
        ioBufferDuration: device.iOBufferDuration,
        inputNumberOfChannels: PreferredNumberOfChannels,
        outputNumberOfChannels: PreferredNumberOfChannels)
    }
    
    public static var video: WebRTCAudioSessionConfiguration {
      let device = deviceInfo
      return WebRTCAudioSessionConfiguration(
        category: PreferredCategory,
        categoryOptions: PreferredVideoCategoryMode,
        mode: PreferredVideoMode,
        sampleRate: device.sampleRate,
        ioBufferDuration: device.iOBufferDuration,
        inputNumberOfChannels: PreferredNumberOfChannels,
        outputNumberOfChannels: PreferredNumberOfChannels)
    }
    
    public static var playback: WebRTCAudioSessionConfiguration {
      let device = deviceInfo
      return WebRTCAudioSessionConfiguration(
        category: .ambient,
        categoryOptions: .duckOthers,
        mode: .default,
        sampleRate: device.sampleRate,
        ioBufferDuration: device.iOBufferDuration,
        inputNumberOfChannels: PreferredNumberOfChannels,
        outputNumberOfChannels: PreferredNumberOfChannels)
    }
    
    static var deviceInfo: (sampleRate: Double, iOBufferDuration: TimeInterval) {
      let processorCount = ProcessInfo.processInfo.processorCount
      if processorCount > 1 {
        return (HighPerformanceSampleRate, HighPerformanceIOBufferDuration)
      }
      return (LowComplexitySampleRate, LowComplexityIOBufferDuration)
    }
    
    // Use a category which supports simultaneous recording and playback. By default, using this category implies that our
    // app’s audio is nonmixable, hence activating the session will interrupt any other audio sessions which are also
    // nonmixable.
    static let PreferredCategory = AVAudioSession.Category.playAndRecord
    
    // Specify a category option that allows us to use bluetooth.
    static let PreferredVOIPCategoryMode: AVAudioSession.CategoryOptions = [.allowBluetooth,      // Determines whether Bluetooth handsfree devices appear as available input routes.
                                                                            .allowBluetoothA2DP,  // Determines whether audio from this session can be streamed to Bluetooth devices that support the Advanced Audio Distribution Profile (A2DP).
                                                                            .allowAirPlay]        // Determines whether audio from this session can be streamed to AirPlay devices.
    
    static let PreferredVideoCategoryMode: AVAudioSession.CategoryOptions = [.allowBluetooth,     // Determines whether Bluetooth handsfree devices appear as available input routes.
                                                                             .allowBluetoothA2DP, // Determines whether audio from this session can be streamed to Bluetooth devices that support the Advanced Audio Distribution Profile (A2DP).
                                                                             .allowAirPlay,       // Determines whether audio from this session can be streamed to AirPlay devices.
                                                                             .defaultToSpeaker]   // Determines whether audio from the session defaults to the built-in speaker instead of the receiver.
    
    // Specify mode for two-way voice communication (e.g. VoIP).
    // This mode is intended for Voice over IP (VoIP) apps and can only be used
    // with the playAndRecord category. When this mode is used, the device’s tonal
    // equalization is optimized for voice and the set of allowable audio routes is
    // reduced to only those appropriate for voice chat. Using this mode has the
    // side effect of enabling the allowBluetooth category option.
    static let PreferredVOIPMode = AVAudioSession.Mode.voiceChat
    
    // Specify this mode if your app is engaging in online video conferencing.
    // This mode is intended for video chat apps and can only be used with the
    // playAndRecord or record categories. When this mode is used, the device’s
    // tonal equalization is optimized for voice and the set of allowable audio
    // routes is reduced to only those appropriate for video chat. Using this mode
    // has the side effect of enabling the allowBluetooth category option.
    static let PreferredVideoMode = AVAudioSession.Mode.videoChat
    
    // Try to use mono to save resources. Also avoids channel format conversion
    // in the I/O audio unit. Initial tests have shown that it is possible to use
    // mono natively for built-in microphones and for BT headsets but not for
    // wired headsets. Wired headsets only support stereo as native channel format
    // but it is a low cost operation to do a format conversion to mono in the
    // audio unit. Hence, we will not hit a RTC_CHECK in
    // VerifyAudioParametersForActiveAudioSession() for a mismatch between the
    // preferred number of channels and the actual number of channels.
    static let PreferredNumberOfChannels = 1
    
    // Preferred hardware sample rate (unit is in Hertz). The client sample rate
    // will be set to this value as well to avoid resampling the the audio unit's
    // format converter. Note that, some devices, e.g. BT headsets, only supports
    // 8000Hz as native sample rate.
    static let HighPerformanceSampleRate = 48000.0
    
    // A lower sample rate will be used for devices with only one core
    // (e.g. iPhone 4). The goal is to reduce the CPU load of the application.
    static let LowComplexitySampleRate = 16000.0
    
    // Use a hardware I/O buffer size (unit is in seconds) that matches the 10ms
    // size used by WebRTC. The exact actual size will differ between devices.
    // Example: using 48kHz on iPhone 6 results in a native buffer size of
    // ~10.6667ms or 512 audio frames per buffer. The FineAudioBuffer instance will
    // take care of any buffering required to convert between native buffers and
    // buffers used by WebRTC. It is beneficial for the performance if the native
    // size is as close to 10ms as possible since it results in "clean" callback
    // sequence without bursts of callbacks back to back.
    static let HighPerformanceIOBufferDuration = 0.01
    
    // Use a larger buffer size on devices with only one core (e.g. iPhone 4).
    // It will result in a lower CPU consumption at the cost of a larger latency.
    // The size of 60ms is based on instrumentation that shows a significant
    // reduction in CPU load compared with 10ms on low-end devices.
    // TODO(henrika): monitor this size and determine if it should be modified.
    static let LowComplexityIOBufferDuration = 0.06
  }
}

