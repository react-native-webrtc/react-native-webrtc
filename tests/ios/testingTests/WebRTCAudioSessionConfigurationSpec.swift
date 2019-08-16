//
//  AudioSessionConfigurationSpec.swift
//  testing
//
//  Created by Robert Barclay on 7/1/19.
//  Copyright © 2019 LogMeIn. All rights reserved.
//

@testable import react_native_webrtc
import Quick
import Nimble
import AVFoundation

class WEBRTCAudioSessionConfigurationSpec: QuickSpec {
  override func spec() {
    
    describe("defaults") {
      
      describe("voip") {
        
        let configuration = WebRTCAudioSessionConfiguration.Defaults.voip
        
        it("should have playAndRecord category") {
          expect(configuration.category) == AVAudioSession.Category.playAndRecord
        }
        
        it("should have allowBluetooth CategoryOptions") {
          expect(configuration.categoryOptions) == AVAudioSession.CategoryOptions.allowBluetooth
        }
        
        it("should have allowBluetooth CategoryOptions") {
          expect(configuration.mode) == AVAudioSession.Mode.voiceChat
        }
        
        it("should have sample rate for the device") {
          expect(configuration.sampleRate) == WebRTCAudioSessionConfiguration.Defaults.deviceInfo.sampleRate
        }
        
        it("should have ioBufferDuration for the device") {
          expect(configuration.ioBufferDuration) == WebRTCAudioSessionConfiguration.Defaults.deviceInfo.iOBufferDuration
        }
        
        it("should have inputNumberOfChannels for the device") {
          expect(configuration.inputNumberOfChannels) == WebRTCAudioSessionConfiguration.Defaults.PreferredNumberOfChannels
        }
        
        it("should have inputNumberOfChannels for the device") {
          expect(configuration.outputNumberOfChannels) == WebRTCAudioSessionConfiguration.Defaults.PreferredNumberOfChannels
        }
      }
      
      describe("video") {
        let configuration = WebRTCAudioSessionConfiguration.Defaults.video
        
        it("should have playAndRecord category") {
          expect(configuration.category) == AVAudioSession.Category.playAndRecord
        }
        
        it("should have allowBluetooth CategoryOptions") {
          expect(configuration.categoryOptions) == AVAudioSession.CategoryOptions.allowBluetooth
        }
        
        it("should have allowBluetooth CategoryOptions") {
          expect(configuration.mode) == AVAudioSession.Mode.videoChat
        }
        
        it("should have sample rate for the device") {
          expect(configuration.sampleRate) == WebRTCAudioSessionConfiguration.Defaults.deviceInfo.sampleRate
        }
        
        it("should have ioBufferDuration for the device") {
          expect(configuration.ioBufferDuration) == WebRTCAudioSessionConfiguration.Defaults.deviceInfo.iOBufferDuration
        }
        
        it("should have inputNumberOfChannels for the device") {
          expect(configuration.inputNumberOfChannels) == WebRTCAudioSessionConfiguration.Defaults.PreferredNumberOfChannels
        }
        
        it("should have inputNumberOfChannels for the device") {
          expect(configuration.outputNumberOfChannels) == WebRTCAudioSessionConfiguration.Defaults.PreferredNumberOfChannels
        }
      }
      
      describe("playback") {
        let configuration = WebRTCAudioSessionConfiguration.Defaults.playback
        
        it("should have playAndRecord category") {
          expect(configuration.category) == AVAudioSession.Category.ambient
        }
        
        it("should have allowBluetooth CategoryOptions") {
          expect(configuration.categoryOptions) == AVAudioSession.CategoryOptions.duckOthers
        }
        
        it("should have allowBluetooth CategoryOptions") {
          expect(configuration.mode) == AVAudioSession.Mode.default
        }
        
        it("should have sample rate for the device") {
          expect(configuration.sampleRate) == WebRTCAudioSessionConfiguration.Defaults.deviceInfo.sampleRate
        }
        
        it("should have ioBufferDuration for the device") {
          expect(configuration.ioBufferDuration) == WebRTCAudioSessionConfiguration.Defaults.deviceInfo.iOBufferDuration
        }
        
        it("should have inputNumberOfChannels for the device") {
          expect(configuration.inputNumberOfChannels) == WebRTCAudioSessionConfiguration.Defaults.PreferredNumberOfChannels
        }
        
        it("should have inputNumberOfChannels for the device") {
          expect(configuration.outputNumberOfChannels) == WebRTCAudioSessionConfiguration.Defaults.PreferredNumberOfChannels
        }
      }
    }
    
    describe("voip configuration") {
      
      it("should by default be default voip configuration") {
        let voip = WebRTCAudioSessionConfiguration.voipAudioConfiguration()
        expect(voip) == WebRTCAudioSessionConfiguration.Defaults.voip
      }
      
      it("should be configurable") {
        WebRTCAudioSessionConfiguration.setVoip(configuration: WebRTCAudioSessionConfiguration(
          category: AVAudioSession.Category.playAndRecord,
          categoryOptions: AVAudioSession.CategoryOptions.allowAirPlay,
          mode: AVAudioSession.Mode.voiceChat,
          sampleRate: WebRTCAudioSessionConfiguration.Defaults.deviceInfo.sampleRate,
          ioBufferDuration: WebRTCAudioSessionConfiguration.Defaults.deviceInfo.iOBufferDuration,
          inputNumberOfChannels: WebRTCAudioSessionConfiguration.Defaults.PreferredNumberOfChannels,
          outputNumberOfChannels: WebRTCAudioSessionConfiguration.Defaults.PreferredNumberOfChannels))
        
        let voip = WebRTCAudioSessionConfiguration.voipAudioConfiguration()
        expect(voip) != WebRTCAudioSessionConfiguration.Defaults.voip
      }
    }
    
    describe("video configuration") {
      
      it("should by default be default video configuration") {
        let video = WebRTCAudioSessionConfiguration.videoAudioConfiguration()
        expect(video) == WebRTCAudioSessionConfiguration.Defaults.video
      }
      
      it("should be configutable") {
        WebRTCAudioSessionConfiguration.setVideo(configuration: WebRTCAudioSessionConfiguration(
          category: AVAudioSession.Category.playAndRecord,
          categoryOptions: AVAudioSession.CategoryOptions.allowAirPlay,
          mode: AVAudioSession.Mode.videoChat,
          sampleRate: WebRTCAudioSessionConfiguration.Defaults.deviceInfo.sampleRate,
          ioBufferDuration: WebRTCAudioSessionConfiguration.Defaults.deviceInfo.iOBufferDuration,
          inputNumberOfChannels: WebRTCAudioSessionConfiguration.Defaults.PreferredNumberOfChannels,
          outputNumberOfChannels: WebRTCAudioSessionConfiguration.Defaults.PreferredNumberOfChannels))
        
        let video = WebRTCAudioSessionConfiguration.videoAudioConfiguration()
        expect(video) != WebRTCAudioSessionConfiguration.Defaults.video
      }
    }
    
    describe("playback configuration") {
      
      it("should by default be default playback configuration") {
        let playback = WebRTCAudioSessionConfiguration.playbackAudioConfiguration()
        expect(playback) == WebRTCAudioSessionConfiguration.Defaults.playback
      }
      
      it("should be configurable") {
        WebRTCAudioSessionConfiguration.setPlayback(configuration: WebRTCAudioSessionConfiguration(
          category: AVAudioSession.Category.ambient,
          categoryOptions: AVAudioSession.CategoryOptions.allowAirPlay,
          mode: AVAudioSession.Mode.default,
          sampleRate: WebRTCAudioSessionConfiguration.Defaults.deviceInfo.sampleRate,
          ioBufferDuration: WebRTCAudioSessionConfiguration.Defaults.deviceInfo.iOBufferDuration,
          inputNumberOfChannels: WebRTCAudioSessionConfiguration.Defaults.PreferredNumberOfChannels,
          outputNumberOfChannels: WebRTCAudioSessionConfiguration.Defaults.PreferredNumberOfChannels))
        
        let video = WebRTCAudioSessionConfiguration.playbackAudioConfiguration()
        expect(video) != WebRTCAudioSessionConfiguration.Defaults.playback
      }
    }
  }
}
