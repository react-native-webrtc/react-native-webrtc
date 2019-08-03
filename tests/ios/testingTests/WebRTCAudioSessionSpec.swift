//
//  AudioSessionSpec.swift
//  testingTests
//
//  Created by Robert Barclay on 7/31/19.
//

import Quick
import Nimble
import OCMock

@testable import react_native_webrtc

class WebRTCAudioSessionSpec: QuickSpec {
    
    override func spec() {
        
        var rtcAudioSession: MockRTCAudioSession!
        var audioSession: WebRTCAudioSession!
        
        beforeEach {
            rtcAudioSession = MockRTCAudioSession()
            audioSession = WebRTCAudioSession()
            audioSession.rtcAudioSession = rtcAudioSession
        }
        
        it("should launch WebAudioSession from bridge") {
            let bridge = RCTBridge(delegate: BridgeDelegate(), launchOptions: [:])!
            let rtcAudioSession = bridge.module(forName: "WebRTCAudioSession") as? WebRTCAudioSession
            expect(rtcAudioSession).toNot(beNil())
        }
        
        it("should call lock configuration on rtc audio session") {
            audioSession.lockForConfiguration()
            expect(rtcAudioSession.lockForConfigurationCalled) == true
        }
        
        it("should call unlock configuration on rtc audio session") {
            audioSession.unlockForConfiguration()
            expect(rtcAudioSession.unlockForConfigurationCalled) == true
        }
        
        it("should return audio enabled from rtc audio session") {
            rtcAudioSession.isAudioEnabled = true
            expect(audioSession.isAudioEnabled()) == true
        }
        
        it("should return manual audio from rtc audio session") {
            rtcAudioSession.useManualAudio = true
            expect(audioSession.isManualAudio()) == true
        }
        
        describe("start audio") {
            
            describe("promise") {
                
                it("should call reject with code if failed") {
                    audioSession.startAudio({ _ in
                        fail("should not call this")
                    }, rejecter: { code, _, _ in
                        expect(code) == AudioSessionErrorCode.startError.rawValue
                    })
                }
                
                it("should call reject with message if failed") {
                    audioSession.startAudio({ _ in
                        fail("should not call this")
                    }, rejecter: { _, message, _ in
                        expect(message) == "Unable to start audio"
                    })
                }
                
                it("should call reject with error if failed") {
                    audioSession.startAudio({ _ in
                        fail("should not call this")
                    }, rejecter: { _, _, error in
                        expect(error).to(matchError( AudioSessionError.manualModeNotSet ))
                    })
                }
                
                context("success") {
                    beforeEach {
                        rtcAudioSession.useManualAudio = true
                        rtcAudioSession.category = AVAudioSession.Category.playAndRecord.rawValue
                    }
                    
                    it("should resolve") {
                        audioSession.startAudio({ result in
                            expect(result as? String) == "success"
                        }, rejecter: { _, _, error in
                            fail("should not call this")
                        })
                    }
                }
            }
            
            describe("internal") {
                
                it("should throw error if manual audio is not enabled") {
                    expect{
                        try audioSession.startAudio()
                        }.to(throwError( AudioSessionError.manualModeNotSet ))
                }
                
                context("manual audio is enabled") {
                    
                    beforeEach {
                        rtcAudioSession.useManualAudio = true
                    }
                    
                    it("should throw error if audio category is not play and record") {
                        expect{
                            try audioSession.startAudio()
                            }.to(throwError( AudioSessionError.audioCategoryNotPlayRecord ))
                    }
                    
                    context("audio category is play and record") {
                        
                        beforeEach {
                            rtcAudioSession.category = AVAudioSession.Category.playAndRecord.rawValue
                        }
                        
                        it("should set is audio enabled to true") {
                            try? audioSession.startAudio()
                            expect(rtcAudioSession.isAudioEnabledCalled) == true
                        }
                        
                        it("should set is audio enabled once") {
                            try? audioSession.startAudio()
                            expect(rtcAudioSession.isAudioEnabledCalledCount) == 1
                        }
                        
                        it("should not call set active on audio session") {
                            try? audioSession.startAudio()
                            expect(rtcAudioSession.setActiveCalled) == false
                        }
                        
                        it("should not throw error") {
                            expect {
                                try audioSession.startAudio()
                                }.toNot(throwError())
                        }
                        
                        context("is audio enabled already true") {
                            
                            beforeEach {
                                rtcAudioSession.isAudioEnabled = true
                                rtcAudioSession.isAudioEnabledCalledCount = 0
                                rtcAudioSession.isAudioEnabledCalled = false
                            }
                            
                            it("should set is audio enabled once") {
                                try? audioSession.startAudio()
                                expect(rtcAudioSession.isAudioEnabledCalledCount) == 2
                            }
                        }
                        
                        context("audio session is not active") {
                            
                            beforeEach {
                                rtcAudioSession.isActive = false
                            }
                            
                            it("should call set active on audio session") {
                                try? audioSession.startAudio()
                                expect(rtcAudioSession.setActiveCalled) == true
                            }
                            
                            it("should throw exception if set audio failed") {
                                let error = NSError(domain: "test error", code: 0, userInfo: nil)
                                rtcAudioSession.onSetActive = { _ in throw error }
                                expect {
                                    try audioSession.startAudio()
                                    }.to(throwError(error))
                            }
                        }
                    }
                }
            }
        }
        
        describe("stop audio") {
            
            describe("promise") {
                
                it("should call reject with code if failed") {
                    audioSession.stopAudio({ _ in
                        fail("should not call this")
                    }, rejecter: { code, _, _ in
                        expect(code) == AudioSessionErrorCode.stopError.rawValue
                    })
                }
                
                it("should call reject with message if failed") {
                    audioSession.stopAudio({ _ in
                        fail("should not call this")
                    }, rejecter: { _, message, _ in
                        expect(message) == "Unable to stop audio"
                    })
                }
                
                it("should call reject with error if failed") {
                    audioSession.stopAudio({ _ in
                        fail("should not call this")
                    }, rejecter: { _, _, error in
                        expect(error).to(matchError( AudioSessionError.manualModeNotSet ))
                    })
                }
                
                context("success") {
                    beforeEach {
                        rtcAudioSession.useManualAudio = true
                    }
                    
                    it("should resolve") {
                        audioSession.stopAudio({ result in
                            expect(result as? String) == "success"
                        }, rejecter: { _, _, error in
                            fail("should not call this")
                        })
                    }
                }
            }
            
            describe("internal") {
                
                it("should throw error if manual audio is not enabled") {
                    expect{
                        try audioSession.stopAudio()
                    }.to(throwError( AudioSessionError.manualModeNotSet ))
                }
                
                context("manual audio is enabled") {
                    
                    beforeEach {
                        rtcAudioSession.useManualAudio = true
                    }
                    
                    it("should set audio enabled") {
                        try? audioSession.stopAudio()
                        expect(rtcAudioSession.isAudioEnabledCalled) == true
                    }
                }
            }
        }
        
        describe("set manual audio") {
            
            it("should set manual audio on the rctAudioSession") {
                audioSession.setManualAudio(true)
                expect(rtcAudioSession.useManualAudio) == true
            }
            
            it("should set is audio enabled to false") {
                audioSession.setManualAudio(true)
                expect(rtcAudioSession.isAudioEnabled) == false
                expect(rtcAudioSession.isAudioEnabledCalled) == true
            }
        }
    }
}

class BridgeDelegate: NSObject, RCTBridgeDelegate {
    func sourceURL(for bridge: RCTBridge!) -> URL! {
        return Bundle.main.bundleURL
    }
}

class MockRTCAudioSession: RTCAudioSessionProtocol {
    var useManualAudioCalled = false
    var useManualAudio: Bool = false {
        didSet {
            useManualAudioCalled = true
        }
    }
    
    var isAudioEnabledCalled = false
    var isAudioEnabledCalledCount = 0
    var isAudioEnabled: Bool = false {
        didSet {
            isAudioEnabledCalled = true
            isAudioEnabledCalledCount += 1
        }
    }
    var category: String = AVAudioSession.Category.ambient.rawValue
    var isActive: Bool = true
    
    var lockForConfigurationCalled = false
    func lockForConfiguration() {
        lockForConfigurationCalled = true
    }
    
    var unlockForConfigurationCalled = false
    func unlockForConfiguration() {
        unlockForConfigurationCalled = true
    }
    
    var setActiveCalled = false
    var onSetActive: (Bool) throws -> Void = { _ in }
    func setActive(_ active: Bool) throws {
        setActiveCalled = true
        try onSetActive(active)
    }
    
    
}
