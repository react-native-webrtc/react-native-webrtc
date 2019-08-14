//
//  AudioSession.swift
//  testing
//
//  Created by Robert Barclay on 7/30/19.
//

import WebRTC
import React
import AVFoundation

// Define the react-native exposed class with the interface that is mapped in the bridging to be expose to react native.
@objc(WebRTCAudioSession)
public class WebRTCAudioSession: NSObject {
  
  lazy var dispatchQueue = DispatchQueue(label: "react-native-webrtc.audiosession")
  lazy var rtcAudioSession: RTCAudioSessionProtocol = RTCAudioSession.sharedInstance()
  lazy var center = NotificationCenter.default
  
  // MARK: - React Native interface -
  
  // MARK: Audio session configuration
  
  // In this sub section we provide some Promise methods that configure the audio session to be ready for WebRTC audio,
  // or to return the audio back to an ambiant state. When engaging VoIP mode, it enabled play and record, bluetooth and
  // external audio, and changed the move dot be in voice, so the audio hardware is optimized for Voice. When video mode
  // is engaged it will set the mode for Video, while enableing for play record and bluthooth and extenal hardware. The
  // Disengage will return the audio session to be in a default state.
  
  @objc
  public func isEngaged() -> Bool {
    return rtcAudioSession.isVoIPActive
  }
  
  @objc
  public func engageVoipAudioSession(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    do {
      try engageAudioSession( WebRTCAudioSessionConfiguration.Defaults.voip )
      resolve("success")
    } catch {
      reject(AudioSessionErrorCode.engageAudioError.rawValue, "Unable to engage voip session", error)
    }
  }
  
  @objc
  public func engageVideoAudioSession(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    do {
      try engageAudioSession( WebRTCAudioSessionConfiguration.Defaults.video )
      resolve("success")
    } catch {
      reject(AudioSessionErrorCode.engageAudioError.rawValue, "Unable to engage video session", error)
    }
  }
  
  @objc
  public func disengageAudioSession(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    do {
      try disengageAudioSession()
      resolve("success")
    } catch {
      reject(AudioSessionErrorCode.disengageAudioError.rawValue, "Unable to start audio", error)
    }
  }
  
  // MARK: Manual Audio
  
  // In this subsection we provide Promise methods for startting and stopping audio manually in a manaual audio mode.
  // The startAudio function will automatically engage the VoIP audio session if we are not already engaged. it also
  // validates if we are in manual mode and if we are able to start the audio. if the audio session is not active it
  // will activate it. Also the stop audio will clear it out.
  
  @objc
  public func isManualAudio() -> Bool {
    return rtcAudioSession.useManualAudio
  }
  
  @objc
  public func isAudioEnabled() -> Bool {
    return rtcAudioSession.isAudioEnabled
  }
  
  @objc
  public func startAudio(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    do {
      try startAudio()
      resolve("success")
    } catch {
      reject(AudioSessionErrorCode.startError.rawValue, "Unable to start audio", error)
    }
  }
  
  @objc
  public func stopAudio(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    do {
      try stopAudio()
      resolve("success")
    } catch {
      reject(AudioSessionErrorCode.stopError.rawValue, "Unable to stop audio", error)
    }
  }
  
  @objc
  public func setManualAudio(_ manualAudio: Bool) {
    rtcAudioSession.useManualAudio = manualAudio
    rtcAudioSession.isAudioEnabled = false
  }
  
  @objc
  public static func requiresMainQueueSetup() -> Bool {
    return true
  }
  
  // MARK: - Internal -
  
  func engageAudioSession(_ config: WebRTCAudioSessionConfiguration ) throws {
    
    guard rtcAudioSession.isVoIPActive == false else {
      return
    }
    
    NSLog("[AudioSession]: Engage Audio Session")
    
    rtcAudioSession.lockForConfiguration()
    try setConfiguration(config)
    rtcAudioSession.unlockForConfiguration()
  }
  
  func disengageAudioSession() throws {
    
    NSLog("[AudioSession]: Disengage Audio Session")
    
    let config = WebRTCAudioSessionConfiguration.Defaults.playback
    rtcAudioSession.lockForConfiguration()
    try setConfiguration(config)
    rtcAudioSession.unlockForConfiguration()
  }
  
  func setConfiguration(_ config: WebRTCAudioSessionConfiguration) throws {
    var madeChanges = false
    defer {
      if madeChanges {
        center.post(name: .audioSessionDidChange, object: self)
      }
    }
    
    if rtcAudioSession.category != config.category.rawValue || rtcAudioSession.categoryOptions != config.categoryOptions || rtcAudioSession.mode != config.mode.rawValue {
      try rtcAudioSession.setCategory(config.category.rawValue, mode: config.mode.rawValue, options: config.categoryOptions)
      madeChanges = true
    }
    
    if rtcAudioSession.preferredSampleRate != config.sampleRate {
      try rtcAudioSession.setPreferredSampleRate(config.sampleRate)
      madeChanges = true
    }
    
    let preferredIOBufferDuration = TimeInterval(round(1000 * rtcAudioSession.preferredIOBufferDuration)/1000)
    if preferredIOBufferDuration != config.ioBufferDuration {
      try rtcAudioSession.setPreferredIOBufferDuration(config.ioBufferDuration)
      madeChanges = true
    }
    
    guard rtcAudioSession.isVoIPActive else {
      return
    }
    
    // Try to set the preferred number of hardware audio channels. These calls must be done after setting the audio
    // session’s category and mode and activating the session.
    if rtcAudioSession.inputNumberOfChannels != config.inputNumberOfChannels {
      try? rtcAudioSession.setPreferredInputNumberOfChannels(config.inputNumberOfChannels)
      madeChanges = true
    }
    
    if rtcAudioSession.outputNumberOfChannels != config.outputNumberOfChannels {
      try? rtcAudioSession.setPreferredOutputNumberOfChannels(config.outputNumberOfChannels)
      madeChanges = true
    }
  }
  
  /**
   Starts the web rtc audio units to start gathering data from the the audio session under manual mode. IF we are in
   manual mode we throw an error. We should also have the category of play and record so we have access to the
   microphone. if we are not set to play and record we throw an error. if we currently have is enable turned on, we
   first stop and then restart the audio. before we start the audio we make sure the audio session is set to active. */
  func startAudio() throws {
    
    guard isManualAudio() else {
      throw AudioSessionError.manualModeNotSet
    }
    
    // Check to see if we are in voip mode. if not, try to configure the audio session to be VOIP. If it is configured
    // for video, this should already be enabled.
    if rtcAudioSession.isVoIPActive == false {
      try engageAudioSession( WebRTCAudioSessionConfiguration.Defaults.voip )
    }
    
    guard rtcAudioSession.isVoIPActive else {
      throw AudioSessionError.audioCategoryNotPlayRecord
    }
    
    if isAudioEnabled() == true {
      rtcAudioSession.isAudioEnabled = false
    }
    
    // We post this notification to "Kick start the audio session back into gear". This should clear up anything that
    // might be caused by an interuption having triggered the audio interuptions mode in webRTC, if there is anything
    // lingering there. This is a noted workaround for the interuption causing it to become broken.
    let info = [AVAudioSessionInterruptionTypeKey: AVAudioSession.InterruptionType.ended.rawValue as UInt]
    center.post(name: AVAudioSession.interruptionNotification, object: nil, userInfo: info)
    
    // Ensure the that audio session is active before we say is audio enabled. We need an active audio session. we
    // should normally be active, this is mostly just a sanity check. due to the locking nature of the RTCAudioSession,
    // we manually engage and disengage the lock. if we do not it will throw an error.
    if rtcAudioSession.isActive == false {
      rtcAudioSession.lockForConfiguration()
      try rtcAudioSession.setActive(true)
      rtcAudioSession.unlockForConfiguration()
    }
    
    rtcAudioSession.isAudioEnabled = true
  }
  
  /** Stops the audio units from recording. */
  public func stopAudio() throws {
    
    guard isManualAudio() else {
      throw AudioSessionError.manualModeNotSet
    }
    
    rtcAudioSession.isAudioEnabled = false
  }
}

// MARK: - Mocking interfaces -

/**
 We define a protocol of what we use on the RTCAudioSession in the WebRTC.framework. This protocol allows us to inject a
 mock object conforming to that protocol to test the implementation of our methods to its interactions with the
 RTCAudioSession. */
protocol RTCAudioSessionProtocol {
  var useManualAudio: Bool { get set }
  var isAudioEnabled: Bool { get set }
  var isActive: Bool { get }
  var isVoIPActive: Bool { get }
  
  func lockForConfiguration()
  func unlockForConfiguration()
  func setActive(_ active: Bool) throws
  
  var category: String { get }
  var categoryOptions: AVAudioSession.CategoryOptions  { get }
  var mode: String  { get }
  var sampleRate: Double  { get }
  var inputNumberOfChannels: Int  { get }
  var outputNumberOfChannels: Int  { get }
  var outputVolume: Float  { get }
  var inputLatency: TimeInterval  { get }
  var outputLatency: TimeInterval  { get }
  var ioBufferDuration: TimeInterval  { get }
  var preferredSampleRate: Double { get }
  var preferredIOBufferDuration: TimeInterval { get }
  
  func setCategory(_ category: String, mode: String, options: AVAudioSession.CategoryOptions) throws
  func setMode(_ mode: String) throws /* set session mode */
  func setPreferredInput(_ inPort: AVAudioSessionPortDescription) throws
  func setPreferredSampleRate(_ sampleRate: Double) throws
  func setPreferredIOBufferDuration(_ duration: TimeInterval) throws
  func setPreferredInputNumberOfChannels(_ count: Int) throws
  func setPreferredOutputNumberOfChannels(_ count: Int) throws
}

// We add our protocol as an extension to the object from the framework so we the refer to is as the protocol rather
// than as the class.
extension RTCAudioSession: RTCAudioSessionProtocol {
  
  /**
   Determine if our audio session is setup to be VOIP from its properties. if we have video or voice chat for out
   mode and we have Play and Record enabled, then we are currently VOIP enabled and active.
   */
  public var isVoIPActive: Bool {
    return category == AVAudioSession.Category.playAndRecord.rawValue && (mode == AVAudioSession.Mode.voiceChat.rawValue || mode == AVAudioSession.Mode.videoChat.rawValue)
  }
  
  func setCategory(_ category: String, mode: String, options: AVAudioSession.CategoryOptions) throws {
    try setCategory(category, with: options)
    try setMode(mode)
  }
  
  open override var debugDescription: String {
    return "WebRTCAudioSession: {\n" +
      "  category: \(category)\n" +
      "  categoryOptions: \(categoryOptions.rawValue)\n" +
      "  mode: \(mode)\n" +
      "  isVoipActive: \(isVoIPActive)\n" +
      "  sampleRate: \(sampleRate)\n" +
      "  IOBufferDuration: \(ioBufferDuration)\n" +
      "  outputNumberOfChannels: \(outputNumberOfChannels)\n" +
      "  inputNumberOfChannels: \(inputNumberOfChannels)\n" +
      "  outputLatency: \(outputLatency)\n" +
      "  inputLatency: \(inputLatency)\n" +
      "  outputVolume: \(outputVolume)\n" +
    "}"
  }
}

// MARK: - Errors -

enum AudioSessionError: Error {
  case manualModeNotSet
  case audioCategoryNotPlayRecord
}

enum AudioSessionErrorCode: String {
  case startError = "RTCAudioSessionStartError"
  case stopError = "RTCAudioSessionStopError"
  case engageAudioError = "ENGAGE_AUDIO_SESSION_ERROR"
  case disengageAudioError = "DISENGAGE_AUDIO_SESSION_ERROR"
}

extension Notification.Name {
  static let audioSessionDidChange = Notification.Name(rawValue: "audioSessionDidChange")
}
