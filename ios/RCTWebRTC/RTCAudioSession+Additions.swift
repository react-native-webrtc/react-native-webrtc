//
//  RCTAudioSession+Additions.swift
//  testing
//
//  Created by Robert Barclay on 8/16/19.
//

import Foundation

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
  
  func setCategory(_ category: String, with options: AVAudioSession.CategoryOptions) throws
  func setMode(_ mode: String) throws /* set session mode */
  func setPreferredInput(_ inPort: AVAudioSessionPortDescription) throws
  func setPreferredSampleRate(_ sampleRate: Double) throws
  func setPreferredIOBufferDuration(_ duration: TimeInterval) throws
  func setPreferredInputNumberOfChannels(_ count: Int) throws
  func setPreferredOutputNumberOfChannels(_ count: Int) throws
}

extension RTCAudioSessionProtocol {
  
  /**
   Determine if our audio session is setup to be VOIP from its properties. if we have video or voice chat for out
   mode and we have Play and Record enabled, then we are currently VOIP enabled and active.
   */
  var isVoIPActive: Bool {
    return category == AVAudioSession.Category.playAndRecord.rawValue && (mode == AVAudioSession.Mode.voiceChat.rawValue || mode == AVAudioSession.Mode.videoChat.rawValue)
  }
  
  func setCategory(_ category: String, mode: String, options: AVAudioSession.CategoryOptions) throws {
    try setCategory(category, with: options)
    try setMode(mode)
  }
  
  func applyConfigIfNecessary(_ config: WebRTCAudioSessionConfiguration) throws -> Bool {
    
    var madeChanges = false
    
    let category = self.category
    let options = self.categoryOptions
    let mode = self.mode
    if category != config.category.rawValue || options != config.categoryOptions || mode != config.mode.rawValue {
      try setCategory(config.category.rawValue, mode: config.mode.rawValue, options: config.categoryOptions)
      madeChanges = true
    }
    
    if preferredSampleRate != config.sampleRate {
      try setPreferredSampleRate(config.sampleRate)
      madeChanges = true
    }
    
    let preferredIOBufferDuration = TimeInterval(round(1000 * self.preferredIOBufferDuration)/1000)
    if preferredIOBufferDuration != config.ioBufferDuration {
      try setPreferredIOBufferDuration(config.ioBufferDuration)
      madeChanges = true
    }
    
    guard isVoIPActive else {
      return madeChanges
    }
    
    // Try to set the preferred number of hardware audio channels. These calls must be done after setting the audio
    // session’s category and mode and activating the session.
    if inputNumberOfChannels != config.inputNumberOfChannels {
      try? setPreferredInputNumberOfChannels(config.inputNumberOfChannels)
      madeChanges = true
    }
    
    if outputNumberOfChannels != config.outputNumberOfChannels {
      try? setPreferredOutputNumberOfChannels(config.outputNumberOfChannels)
      madeChanges = true
    }
    
    return madeChanges
  }
}

// We add our protocol as an extension to the object from the framework so we the refer to is as the protocol rather
// than as the class.
extension RTCAudioSession: RTCAudioSessionProtocol {}
