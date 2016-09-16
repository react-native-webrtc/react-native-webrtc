require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name                = "RCTWebRTC"
  s.version             = package['version']
  s.summary             = package['description']
  s.description         = <<-DESC
                            WebRTC for react native.
                         DESC
  s.homepage            = "https://github.com/oney/react-native-webrtc"
  s.license             = package['license']
  s.author              = "https://github.com/oney/react-native-webrtc/graphs/contributors"
  s.source              = { :git => "git@github.com:oney/react-native-webrtc.git", :tag => "release #{s.version}" }
  s.default_subspec     = 'Core'
  s.requires_arc        = true
  s.platform            = :ios, "9.2"
  s.preserve_paths      = "ios/libjingle_peerconnection/**/*"
  s.framework           = 'VideoToolbox','CoreVideo','CoreAudio','GLKit','CoreGraphics','AudioToolbox','AVFoundation'
  s.subspec 'Core' do |ss|
    ss.dependency       'RCTWebRTC/libWebRTC'

    ss.source_files        = "ios/**/*.{h,m,a}"
    ss.libraries           = "stdc++","sqlite3","c"
  end
  s.subspec 'libWebRTC' do |ss|
    ss.vendored_libraries = 'ios/libjingle_peerconnection/*.{a}'
    ss.source_files        = "ios/libjingle_peerconnection/*.{h}"
  end
end
