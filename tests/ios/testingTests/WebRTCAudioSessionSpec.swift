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
    var bridge: RCTBridge!
    
    beforeEach {
      bridge = RCTBridge(delegate: BridgeDelegate(), launchOptions: [:])!
      audioSession = bridge.module(forName: "WebRTCAudioSession") as? WebRTCAudioSession
      rtcAudioSession = MockRTCAudioSession()
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
    
    describe("is enabled") {
      
      context("voip") {
        
        beforeEach {
          rtcAudioSession.onCategory = { return .playAndRecord }
          rtcAudioSession.onMode = { return .voiceChat }
        }
        
        it("should return is engaged as true") {
          expect(audioSession.isEngaged()) == true
        }
        
      }
      
      context("video") {
        
        beforeEach {
          rtcAudioSession.onCategory = { return .playAndRecord }
          rtcAudioSession.onMode = { return .videoChat }
        }
        
        it("should return is engaged as true") {
          expect(audioSession.isEngaged()) == true
        }
        
      }
      
      context("playback") {
        
        beforeEach {
          rtcAudioSession.onCategory = { return .ambient }
          rtcAudioSession.onMode = { return .default }
        }
        
        it("should return is engaged as true") {
          expect(audioSession.isEngaged()) == false
        }
      }
    }
    
    describe("engageAudioSession") {
      
      describe("promise") {
        
        context("voip") {
          
          beforeEach {
            rtcAudioSession.onCategory = { return .ambient }
            rtcAudioSession.onMode = { return .default }
          }
          
          context("failed") {
            
            var error: NSError!
            
            beforeEach {
              error = NSError(domain: "error", code: 0, userInfo: nil)
              rtcAudioSession.onSetCategory = { _, _ in
                throw error
              }
            }
            
            it("should call reject with code if failed") {
              audioSession.engageVoipAudioSession({ _ in
                fail("should not call this")
              }, rejecter: { code, _, _ in
                expect(code) == AudioSessionErrorCode.engageAudioError.rawValue
              })
            }
            
            it("should call reject with message if failed") {
              audioSession.engageVoipAudioSession({ _ in
                fail("should not call this")
              }, rejecter: { _, message, _ in
                expect(message) == "Unable to engage voip session"
              })
            }
            
            it("should call reject with error if failed") {
              audioSession.engageVoipAudioSession({ _ in
                fail("should not call this")
              }, rejecter: { _, _, testError in
                expect(testError).to(matchError( error ))
              })
            }
          }
          
          context("success") {
            
            it("should resolve") {
              audioSession.engageVoipAudioSession({ result in
                expect(result as? String) == "success"
              }, rejecter: { _, _, error in
                fail("should not call this")
              })
            }
          }
        }
        
        context("video") {
          
          beforeEach {
            rtcAudioSession.onCategory = { return .ambient }
            rtcAudioSession.onMode = { return .default }
          }
          
          context("failed") {
            
            var error: NSError!
            
            beforeEach {
              error = NSError(domain: "error", code: 0, userInfo: nil)
              rtcAudioSession.onSetCategory = { _, _ in
                throw error
              }
            }
            
            it("should call reject with code if failed") {
              audioSession.engageVideoAudioSession({ _ in
                fail("should not call this")
              }, rejecter: { code, _, _ in
                expect(code) == AudioSessionErrorCode.engageAudioError.rawValue
              })
            }
            
            it("should call reject with message if failed") {
              audioSession.engageVideoAudioSession({ _ in
                fail("should not call this")
              }, rejecter: { _, message, _ in
                expect(message) == "Unable to engage video session"
              })
            }
            
            it("should call reject with error if failed") {
              audioSession.engageVideoAudioSession({ _ in
                fail("should not call this")
              }, rejecter: { _, _, testError in
                expect(testError).to(matchError( error ))
              })
            }
          }
          
          context("success") {
            
            it("should resolve") {
              audioSession.engageVideoAudioSession({ result in
                expect(result as? String) == "success"
              }, rejecter: { _, _, error in
                fail("should not call this")
              })
            }
          }
        }
      }
      
      describe("internal") {
        
        let configuration = WebRTCAudioSessionConfiguration.Defaults.voip
        
        context("voip is active") {
          
          beforeEach {
            rtcAudioSession.onCategory = { return .playAndRecord }
            rtcAudioSession.onMode = { return .voiceChat }
          }
          
          it("should not call lock") {
            try! audioSession.engageAudioSession(configuration)
            expect(rtcAudioSession.lockForConfigurationCalled) == false
          }
          
          it("should not set the category with options") {
            try! audioSession.engageAudioSession(configuration)
            expect(rtcAudioSession.setCategoryWithOptionsCalled) == false
          }
          
          it("should not set the mode") {
            try! audioSession.engageAudioSession(configuration)
            expect(rtcAudioSession.setModeCalled) == false
          }
          
          it("should not call unlock") {
            try! audioSession.engageAudioSession(configuration)
            expect(rtcAudioSession.unlockForConfigurationCalled) == false
          }
        }
        
        context("voip is inactive") {
          
          beforeEach {
            rtcAudioSession.onCategory = { return .ambient }
            rtcAudioSession.onMode = { return .default }
          }
          
          it("should call lock") {
            try! audioSession.engageAudioSession(configuration)
            expect(rtcAudioSession.lockForConfigurationCalled) == true
          }
          
          it("should set the category with options") {
            try! audioSession.engageAudioSession(configuration)
            expect(rtcAudioSession.setCategoryWithOptionsCalled) == true
          }
          
          it("should set the mode") {
            try! audioSession.engageAudioSession(configuration)
            expect(rtcAudioSession.setModeCalled) == true
          }
          
          it("should call unlock") {
            try! audioSession.engageAudioSession(configuration)
            expect(rtcAudioSession.unlockForConfigurationCalled) == true
          }
          
          it("should anounce configuration changes if different") {
            
            audioSession.startObserving()
            audioSession.addListener(WebRTCAudioSession.Event.audioDidUpdate.rawValue)
            
            let name = Notification.Name(rawValue:  WebRTCAudioSession.Event.audioDidUpdate.rawValue)
            let testNotification = Notification(name: name, object: audioSession, userInfo: nil )
            
            expect {
              try! audioSession.engageAudioSession(configuration)
            }.toEventually(postNotifications(contain([testNotification])))
          }
        }
      }
    }
    
    describe("disengageAudioSession") {
      
      describe("promise") {
        
        beforeEach {
          rtcAudioSession.onCategory = { return .playAndRecord }
          rtcAudioSession.onMode = { return .voiceChat }
        }
        
        context("failed") {
          
          var error: NSError!
          
          beforeEach {
            error = NSError(domain: "error", code: 0, userInfo: nil)
            rtcAudioSession.onSetCategory = { _, _ in
              throw error
            }
          }
          
          it("should call reject with code if failed") {
            audioSession.disengageAudioSession({ _ in
              fail("should not call this")
            }, rejecter: { code, _, _ in
              expect(code) == AudioSessionErrorCode.disengageAudioError.rawValue
            })
          }
          
          it("should call reject with message if failed") {
            audioSession.disengageAudioSession({ _ in
              fail("should not call this")
            }, rejecter: { _, message, _ in
              expect(message) == "Unable to disengage audio"
            })
          }
          
          it("should call reject with error if failed") {
            audioSession.disengageAudioSession({ _ in
              fail("should not call this")
            }, rejecter: { _, _, testError in
              expect(testError).to(matchError( error ))
            })
          }
        }
        
        context("success") {
          
          it("should resolve") {
            audioSession.disengageAudioSession({ result in
              expect(result as? String) == "success"
            }, rejecter: { _, _, error in
              fail("should not call this")
            })
          }
        }
      }
      
      describe("internal") {
        
        beforeEach {
          rtcAudioSession.onGetUseManualAudio = { return true }
        }
        
        it("should stop audio if enabled") {
          
          rtcAudioSession.onGetIsAudioEnabled = { return true }
          rtcAudioSession.onSetIsAudioEnabled = { result in
            expect(result) == false
          }
          try! audioSession.disengageAudioSession()
        }
        
        context("voip is active") {
          
          beforeEach {
            rtcAudioSession.onCategory = { return .playAndRecord }
            rtcAudioSession.onMode = { return .voiceChat }
          }
          
          it("should call lock") {
            try! audioSession.disengageAudioSession()
            expect(rtcAudioSession.lockForConfigurationCalled) == true
          }
          
          it("should set the category with options") {
            try! audioSession.disengageAudioSession()
            expect(rtcAudioSession.setCategoryWithOptionsCalled) == true
          }
          
          it("should set the mode") {
            try! audioSession.disengageAudioSession()
            expect(rtcAudioSession.setModeCalled) == true
          }
          
          it("should call unlock") {
            try! audioSession.disengageAudioSession()
            expect(rtcAudioSession.unlockForConfigurationCalled) == true
          }
        }
        
        context("voip is inactive") {
          
          beforeEach {
            rtcAudioSession.onCategory = { return .ambient }
            rtcAudioSession.onMode = { return .default }
          }
          
          it("should not call lock") {
            try! audioSession.disengageAudioSession()
            expect(rtcAudioSession.lockForConfigurationCalled) == false
          }
          
          it("should not set the category with options") {
            try! audioSession.disengageAudioSession()
            expect(rtcAudioSession.setCategoryWithOptionsCalled) == false
          }
          
          it("should not set the mode") {
            try! audioSession.disengageAudioSession()
            expect(rtcAudioSession.setModeCalled) == false
          }
          
          it("should not call unlock") {
            try! audioSession.disengageAudioSession()
            expect(rtcAudioSession.unlockForConfigurationCalled) == false
          }
        }
      }
    }
    
    describe("setConfiguration") {
      
      let configuration = WebRTCAudioSessionConfiguration.Defaults.voip
      
      describe("category mode and options") {
        
        it("should request currect category") {
          try! audioSession.setConfiguration(WebRTCAudioSessionConfiguration.Defaults.playback)
          expect(rtcAudioSession.categoryCalled) == true
        }
        
        it("should request current category options") {
          try! audioSession.setConfiguration(WebRTCAudioSessionConfiguration.Defaults.playback)
          expect(rtcAudioSession.categoryOptionsCalled) == true
        }
        
        it("should request current mode") {
          try! audioSession.setConfiguration(WebRTCAudioSessionConfiguration.Defaults.playback)
          expect(rtcAudioSession.modeCalled) == true
        }
        
        it("should set the category if it does not match the configuration") {
          rtcAudioSession.onCategory = { return .ambient }
          rtcAudioSession.onSetCategory = { category, _ in
            expect(category) == configuration.category.rawValue
          }
          try! audioSession.setConfiguration(configuration)
        }
        
        it("should set the category options if it does not match the configuration") {
          rtcAudioSession.onCategoryOptions = { return .duckOthers }
          rtcAudioSession.onSetCategory = { _, options in
            expect(options) == configuration.categoryOptions
          }
          try! audioSession.setConfiguration(configuration)
        }
        
        it("should set the category options if it does not match the configuration") {
          rtcAudioSession.onMode = { return .default }
          rtcAudioSession.onSetMode = { mode in
            expect(mode) == configuration.mode.rawValue
          }
          try! audioSession.setConfiguration(configuration)
        }
      }
      
      it("should request current prefered sample rate") {
        try! audioSession.setConfiguration(WebRTCAudioSessionConfiguration.Defaults.playback)
        expect(rtcAudioSession.preferredSampleRateCalled) == true
      }
      
      it("should set the preferred sample rate if it does not match the configuration") {
        rtcAudioSession.onPreferredSampleRate = { return 1 }
        rtcAudioSession.onSetPreferredSampleRate = { result in
          expect(result) == configuration.sampleRate
        }
        try! audioSession.setConfiguration(configuration)
      }
      
      it("should request preferred io buffer duration") {
        try! audioSession.setConfiguration(WebRTCAudioSessionConfiguration.Defaults.playback)
        expect(rtcAudioSession.preferredIOBufferDurationCalled) == true
      }
      
      it("should set the preferred io buffer duration if it does not match the configuration") {
        rtcAudioSession.onPreferredIOBufferDuration = { return 1 }
        rtcAudioSession.onSetPreferredIOBufferDuration = { result in
          expect(result) == configuration.ioBufferDuration
        }
        try! audioSession.setConfiguration(configuration)
      }
      
      context("is configured for voip"){
        
        let configuration = WebRTCAudioSessionConfiguration.Defaults.voip
        
        beforeEach {
          rtcAudioSession.onCategory = { return .playAndRecord }
          rtcAudioSession.onMode = { return .voiceChat }
        }
        
        it("should request input number of channels") {
          try! audioSession.setConfiguration(configuration)
          expect(rtcAudioSession.inputNumberOfChannelsCalled) == true
        }
        
        it("should set the input number of channels if it does not match the configuration") {
          rtcAudioSession.onInputNumberOfChannels = { return 2 }
          rtcAudioSession.onSetPreferredInputNumberOfChannels = { result in
            expect(result) == configuration.inputNumberOfChannels
          }
          try! audioSession.setConfiguration(configuration)
        }
        
        it("should request output number of channels") {
          try! audioSession.setConfiguration(configuration)
          expect(rtcAudioSession.outputNumberOfChannelsCalled) == true
        }
        
        it("should set the input number of channels if it does not match the configuration") {
          rtcAudioSession.onOutputNumberOfChannels = { return 2 }
          rtcAudioSession.onSetPreferredOutputNumberOfChannels = { result in
            expect(result) == WebRTCAudioSessionConfiguration.Defaults.playback.outputNumberOfChannels
          }
          try! audioSession.setConfiguration(WebRTCAudioSessionConfiguration.Defaults.voip)
        }
      }
      
      it("should anounce configuration changes if different") {
        
        audioSession.startObserving()
        audioSession.addListener(WebRTCAudioSession.Event.audioDidUpdate.rawValue)
        
        rtcAudioSession.onCategory = { return .ambient }
        
        let name = Notification.Name(rawValue:  WebRTCAudioSession.Event.audioDidUpdate.rawValue)
        let testNotification = Notification(name: name, object: audioSession, userInfo: nil )
        
        expect {
          try! audioSession.setConfiguration( WebRTCAudioSessionConfiguration.Defaults.voip)
        }.toEventually(postNotifications(contain([testNotification])))
        
      }
      
      it("should not anounce configuration changes if the same") {
        
        audioSession.startObserving()
        audioSession.addListener(WebRTCAudioSession.Event.audioDidUpdate.rawValue)
        
        let configuration = WebRTCAudioSessionConfiguration.Defaults.voip
        
        rtcAudioSession.onCategory = { return configuration.category }
        rtcAudioSession.onCategoryOptions = { return configuration.categoryOptions }
        rtcAudioSession.onMode = { return configuration.mode }
        rtcAudioSession.onPreferredSampleRate = { return configuration.sampleRate }
        rtcAudioSession.onPreferredIOBufferDuration = { return configuration.ioBufferDuration }
        rtcAudioSession.onInputNumberOfChannels = { return configuration.inputNumberOfChannels }
        rtcAudioSession.onOutputNumberOfChannels = { return configuration.outputNumberOfChannels }
        
        let name = Notification.Name(rawValue:  WebRTCAudioSession.Event.audioDidUpdate.rawValue)
        let testNotification = Notification(name: name, object: audioSession, userInfo: nil )
        
        expect {
          try! audioSession.setConfiguration( WebRTCAudioSessionConfiguration.Defaults.voip)
        }.toEventuallyNot(postNotifications(contain([testNotification])))
      }
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
            
            it("should call set category") {
              try? audioSession.startAudio()
              expect(rtcAudioSession.setCategoryWithOptionsCalled) == true
            }
            
            it("should call set mode") {
              try? audioSession.startAudio()
              expect(rtcAudioSession.setModeCalled) == true
            }
            
            it("should call lock") {
              try? audioSession.startAudio()
              expect(rtcAudioSession.lockForConfigurationCalled) == true
            }
            
            it("should call unlock") {
              try? audioSession.startAudio()
              expect(rtcAudioSession.unlockForConfigurationCalled) == true
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



