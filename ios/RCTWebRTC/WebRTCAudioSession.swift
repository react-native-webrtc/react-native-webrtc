//
//  AudioSession.swift
//  testing
//
//  Created by Robert Barclay on 7/30/19.
//

import WebRTC
import React
import AVFoundation

protocol RTCAudioSessionProtocol {
  var useManualAudio: Bool { get set }
  var isAudioEnabled: Bool { get set }
  var category: String { get }
  var isActive: Bool { get }
  
  func lockForConfiguration()
  func unlockForConfiguration()
  func setActive(_ active: Bool) throws
}

extension RTCAudioSession: RTCAudioSessionProtocol {}

@objc(WebRTCAudioSession)
public class WebRTCAudioSession: NSObject {
  
  lazy var rtcAudioSession: RTCAudioSessionProtocol = RTCAudioSession.sharedInstance()
  
  @objc
  public func isManualAudio() -> Bool {
    return rtcAudioSession.useManualAudio
  }
  
  @objc
  public func isAudioEnabled() -> Bool {
    return rtcAudioSession.isAudioEnabled
  }
  
  @objc
  public func lockForConfiguration() {
    rtcAudioSession.lockForConfiguration()
  }
  
  @objc
  public func unlockForConfiguration() {
    rtcAudioSession.unlockForConfiguration()
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
  
  /**
   Starts the web rtc audio units to start gathering data from the the audio session under manual mode. IF we are in
   manual mode we throw an error. We should also have the category of play and record so we have access to the
   microphone. if we are not set to play and record we throw an error. if we currently have is enable turned on, we
   first stop and then restart the audio. before we start the audio we make sure the audio session is set to active. */
  func startAudio() throws {
    guard isManualAudio() else {
      throw AudioSessionError.manualModeNotSet
    }
    
    guard rtcAudioSession.category == AVAudioSession.Category.playAndRecord.rawValue else {
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

enum AudioSessionError: Error {
  case manualModeNotSet
  case audioCategoryNotPlayRecord
}

enum AudioSessionErrorCode: String {
  case startError = "RTCAudioSessionStartError"
  case stopError = "RTCAudioSessionStopError"
  case restartError = "RTCAudioSessionRestartError"
}
