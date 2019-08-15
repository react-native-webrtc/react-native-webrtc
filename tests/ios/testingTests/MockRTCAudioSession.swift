//
//  MockRTCAudioSession.swift
//  testingTests
//
//  Created by Robert Barclay on 8/14/19.
//

@testable import react_native_webrtc

class MockRTCAudioSession: RTCAudioSessionProtocol {
    
    var getUseManualAudioCalled = false
    var setUseManualAudioCalled = false
    var onGetUseManualAudio: (() -> Bool) = { return false }
    var onSetUseManualAudio: ((Bool) -> Void) = { _ in  }
    var useManualAudio: Bool {
        get {
            getUseManualAudioCalled = true
            return onGetUseManualAudio()
        }
        set {
            setUseManualAudioCalled = true
            onSetUseManualAudio(newValue)
        }
    }
    
    var getIsAudioEnabledCalled = false
    var getIsAudioEnabledCalledCount = 0
    var onGetIsAudioEnabled: (() -> Bool) = { return false }
    var setIsAudioEnabledCalled = false
    var setIsAudioEnabledCalledCount = 0
    var onSetIsAudioEnabled: ((Bool) -> Void) = { _ in  }
    var isAudioEnabled: Bool {
        get {
            getIsAudioEnabledCalled = true
            getIsAudioEnabledCalledCount += 1
            return onGetIsAudioEnabled()
        }
        set {
            setIsAudioEnabledCalled = true
            setIsAudioEnabledCalledCount += 1
            onSetIsAudioEnabled(newValue)
        }
    }
    
    var lockForConfigurationCalled = false
    func lockForConfiguration() {
        lockForConfigurationCalled = true
    }
    
    var unlockForConfigurationCalled = false
    func unlockForConfiguration() {
        unlockForConfigurationCalled = true
    }
    
    var inActiveCalled = false
    var onIsActive: (() -> Bool) = { return true }
    var isActive: Bool {
        inActiveCalled = true
        return onIsActive()
    }
    
    var setActiveCalled = false
    var onSetActive: (Bool) throws -> Void = { _ in }
    func setActive(_ active: Bool) throws {
        setActiveCalled = true
        try onSetActive(active)
    }
    
    var categoryCalled = false
    var onCategory: (() -> AVAudioSession.Category) = { return .ambient }
    var category: String {
        categoryCalled = true
        return onCategory().rawValue
    }
    
    var categoryOptionsCalled = false
    var onCategoryOptions: (() -> (AVAudioSession.CategoryOptions)) = { return .allowBluetooth }
    var categoryOptions: AVAudioSession.CategoryOptions {
        categoryOptionsCalled = true
        return onCategoryOptions()
    }
    
    var modeCalled = false
    var onMode: (() -> AVAudioSession.Mode) = { return .default }
    var mode: String {
        modeCalled = true
        return onMode().rawValue
    }
    
    var sampleRateCalled = false
    var onSampleRate: (() -> Double) = { return 0 }
    var sampleRate: Double {
        sampleRateCalled = true
        return onSampleRate()
    }
    
    var inputNumberOfChannelsCalled = false
    var onInputNumberOfChannels: (() -> Int) = { return 0 }
    var inputNumberOfChannels: Int {
        inputNumberOfChannelsCalled = true
        return onInputNumberOfChannels()
    }
    
    var outputNumberOfChannelsCalled = false
    var onOutputNumberOfChannels: (() -> Int ) = { return 0 }
    var outputNumberOfChannels: Int {
        outputNumberOfChannelsCalled = true
        return onOutputNumberOfChannels()
    }
    
    var outputVolumeCalled = false
    var onOutputVolume: (() -> Float) = { return 0.0 }
    var outputVolume: Float {
        outputVolumeCalled = true
        return onOutputVolume()
    }
    
    var inputLatencyCalled = false
    var onInputLatency: (() -> TimeInterval) = { return 1.0 }
    var inputLatency: TimeInterval {
        inputLatencyCalled = true
        return onInputLatency()
    }
    
    var outputLatencyCalled = false
    var onOutputLatency: (() -> TimeInterval) = { return 1.0 }
    var outputLatency: TimeInterval {
        outputLatencyCalled = true
        return onOutputLatency()
    }
    
    var ioBufferDurationCalled = false
    var onIoBufferDuration: (() -> TimeInterval) = { return 1.0 }
    var ioBufferDuration: TimeInterval {
        ioBufferDurationCalled = true
        return onIoBufferDuration()
    }
    
    var preferredSampleRateCalled = false
    var onPreferredSampleRate: (() -> Double) = { return 1.0 }
    var preferredSampleRate: Double {
        preferredSampleRateCalled = true
        return onPreferredSampleRate()
    }
    
    var preferredIOBufferDurationCalled = false
    var onPreferredIOBufferDuration: (() -> TimeInterval) = { return 1.0 }
    var preferredIOBufferDuration: TimeInterval {
        preferredIOBufferDurationCalled = true
        return onPreferredIOBufferDuration()
    }
    
    var setCategoryWithOptionsCalled = false
    var onSetCategory: ((String, AVAudioSession.CategoryOptions) throws -> Void) = { _, _ in }
    func setCategory(_ category: String, with options: AVAudioSession.CategoryOptions) throws {
        setCategoryWithOptionsCalled = true
        try onSetCategory(category, options)
    }
    
    var setModeCalled = false
    var onSetMode: ((String) throws -> Void) = { _ in }
    func setMode(_ mode: String) throws {
        setModeCalled = true
        try onSetMode(mode)
    }
    
    var setPreferredInputCalled = false
    var onSetPreferredInput: ((AVAudioSessionPortDescription) throws -> Void) = { _ in }
    func setPreferredInput(_ inPort: AVAudioSessionPortDescription) throws {
        setPreferredInputCalled = true
        try onSetPreferredInput(inPort)
    }
    
    var setPreferredSampleRateCalled = false
    var onSetPreferredSampleRate: ((Double) throws -> Void) = { _ in }
    func setPreferredSampleRate(_ sampleRate: Double) throws {
        setPreferredSampleRateCalled = true
        try onSetPreferredSampleRate(sampleRate)
    }
    
    var setPreferredIOBufferDurationCalled = false
    var onSetPreferredIOBufferDuration: ((TimeInterval) throws -> Void) = { _ in }
    func setPreferredIOBufferDuration(_ duration: TimeInterval) throws {
        setPreferredIOBufferDurationCalled = true
        try onSetPreferredIOBufferDuration(duration)
    }
    
    var setPreferredInputNumberOfChannelsCalled = false
    var onSetPreferredInputNumberOfChannels: ((Int) throws -> Void) = { _ in }
    func setPreferredInputNumberOfChannels(_ count: Int) throws {
        setPreferredInputNumberOfChannelsCalled = true
        try onSetPreferredInputNumberOfChannels(count)
    }
    
    var setPreferredOutputNumberOfChannelsCalled = false
    var onSetPreferredOutputNumberOfChannels: ((Int) throws -> Void) = { _ in }
    func setPreferredOutputNumberOfChannels(_ count: Int) throws {
        setPreferredOutputNumberOfChannelsCalled = true
        try onSetPreferredOutputNumberOfChannels(count)
    }
}
