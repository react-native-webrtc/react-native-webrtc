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
public class WebRTCAudioSession: RCTEventEmitter {
  
  lazy var dispatchQueue = DispatchQueue(label: "react-native-webrtc.audiosession")
  lazy var rtcAudioSession: RTCAudioSessionProtocol = RTCAudioSession.sharedInstance()
  lazy var center = NotificationCenter.default
  
  var hasListeners = false
  var registeredListeners = [String]()
  
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
      try engageAudioSession(WebRTCAudioSessionConfiguration.Defaults.voip)
      resolve("success")
    } catch {
      reject(AudioSessionErrorCode.engageAudioError.rawValue, "Unable to engage voip session", error)
    }
  }
  
  @objc
  public func engageVideoAudioSession(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    do {
      try engageAudioSession(WebRTCAudioSessionConfiguration.Defaults.video)
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
      reject(AudioSessionErrorCode.disengageAudioError.rawValue, "Unable to disengage audio", error)
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
  public func setManualAudio(_ manualAudio: Bool) {
    rtcAudioSession.useManualAudio = manualAudio
    rtcAudioSession.isAudioEnabled = false
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
  
  // MARK: React Native Event Interface
  
  public override func startObserving() {
    super.startObserving()
    hasListeners = true
  }
  
  public override func stopObserving() {
    super.stopObserving()
    hasListeners = false
  }
  
  public override func addListener(_ eventName: String!) {
    super.addListener(eventName)
    registeredListeners.append(eventName)
  }
  
  public override func supportedEvents() -> [String]! {
    return Event.allCases.compactMap({ $0.rawValue })
  }
  
  @objc
  public override static func requiresMainQueueSetup() -> Bool {
    return true
  }
  
  // MARK: - Internal -
  
  func engageAudioSession(_ config: WebRTCAudioSessionConfiguration ) throws {
    
    guard !rtcAudioSession.isVoIPActive else {
      return
    }
    
    NSLog("[AudioSession]: Engage Audio Session")
    
    rtcAudioSession.lockForConfiguration()
    try setConfiguration(config)
    rtcAudioSession.unlockForConfiguration()
  }
  
  func disengageAudioSession() throws {
    
    if rtcAudioSession.isAudioEnabled {
      try stopAudio()
    }
    
    guard rtcAudioSession.isVoIPActive else {
      return
    }
    
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
        send(event: .audioDidUpdate)
      }
    }
    
    madeChanges = try rtcAudioSession.applyConfigIfNecessary(config)
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
  
  internal func canSendEvent(_ event: Event) -> Bool {
    return hasListeners && registeredListeners.contains(event.rawValue)
  }
  
  func send(event: Event, body: [String: String]? = nil) {
    
    guard canSendEvent(event) else {
      return
    }
    
    DispatchQueue.main.async { [weak self] in
      let name = Notification.Name(rawValue: event.rawValue)
      self?.center.post(name: name, object: self, userInfo: body)
      self?.sendEvent(withName: event.rawValue, body: body)
    }
  }
  
  enum Event: String, CaseIterable {
    case audioDidUpdate = "audioDidUpdate" // Dispatched if we changed the audio session configuration
  }
}

// MARK: - Mocking interfaces -



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
