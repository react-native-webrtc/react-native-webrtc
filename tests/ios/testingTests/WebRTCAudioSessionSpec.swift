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
        
        it("should return audio enabled from rtc audio session") {
            rtcAudioSession.onGetIsAudioEnabled = { return true }
            expect(audioSession.isAudioEnabled()) == true
        }
        
        it("should return manual audio from rtc audio session") {
            rtcAudioSession.onGetUseManualAudio = { return true }
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
                        rtcAudioSession.onGetUseManualAudio = { return true }
                        rtcAudioSession.onCategory = { return .playAndRecord }
                        rtcAudioSession.onMode = { return .voiceChat }
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
                    rtcAudioSession.onGetUseManualAudio = { return false }
                    expect{
                        try audioSession.startAudio()
                    }.to(throwError( AudioSessionError.manualModeNotSet ))
                }
                
                context("manual audio is enabled") {
                    
                    beforeEach {
                        rtcAudioSession.onGetUseManualAudio = { return true }
                    }
                    
                    context("voip inactive") {
                        
                        beforeEach {
                            rtcAudioSession.onCategory = { return .ambient }
                            rtcAudioSession.onMode = { return .default }
                        }
                        
                    }
                    
                    context("voip active") {
                        
                        beforeEach {
                            rtcAudioSession.onCategory = { return .playAndRecord }
                            rtcAudioSession.onMode = { return .voiceChat }
                        }
                        
                        context("audio category is play and record") {
                            
                            beforeEach {
                                rtcAudioSession.onCategory = { return .playAndRecord }
                            }
                            
                            it("should set is audio enabled to true") {
                                try? audioSession.startAudio()
                                expect(rtcAudioSession.setIsAudioEnabledCalled) == true
                            }
                            
                            it("should set is audio enabled once") {
                                try? audioSession.startAudio()
                                expect(rtcAudioSession.setIsAudioEnabledCalledCount) == 1
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
                                    rtcAudioSession.onGetIsAudioEnabled = { return true }
                                }
                                
                                it("should set is audio enabled twice") {
                                    try? audioSession.startAudio()
                                    expect(rtcAudioSession.setIsAudioEnabledCalledCount) == 2
                                }
                            }
                            
                            context("audio session is not active") {
                                
                                beforeEach {
                                    rtcAudioSession.onIsActive = { return false }
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
                    
                    //TODO: ask if voip is active and it went into engaging the voip or not.
                    
                    xit("should throw error if audio category is not play and record") {
                        expect{
                            try audioSession.startAudio()
                        }.to(throwError( AudioSessionError.audioCategoryNotPlayRecord ))
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
                        rtcAudioSession.onGetUseManualAudio = { return true }
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
                        rtcAudioSession.onSetIsAudioEnabled = { result in
                            expect(result) == false
                        }
                        try? audioSession.stopAudio()
                    }
                }
            }
        }
        
        describe("set manual audio") {
            
            it("should set manual audio on the rctAudioSession") {
                rtcAudioSession.onSetUseManualAudio = { result in
                    expect(result) == true
                }
                audioSession.setManualAudio(true)
            }
            
            it("should set is audio enabled to false") {
                rtcAudioSession.onSetIsAudioEnabled = { result in
                    expect(result) == false
                }
                audioSession.setManualAudio(true)
                expect(rtcAudioSession.setIsAudioEnabledCalled) == true
            }
        }
    }
}

class BridgeDelegate: NSObject, RCTBridgeDelegate {
    func sourceURL(for bridge: RCTBridge!) -> URL! {
        return Bundle.main.bundleURL
    }
}


