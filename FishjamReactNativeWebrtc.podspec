require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name                = 'FishjamReactNativeWebrtc'
  s.version             = package['version']
  s.summary             = package['description']
  s.homepage            = 'https://github.com/fishjam-cloud/fishjam-react-native-webrtc'
  s.license             = package['license']
  s.author              = { 'Fishjam Cloud' => 'https://github.com/fishjam-cloud' }
  s.source              = { :git => 'https://github.com/fishjam-cloud/fishjam-react-native-webrtc.git', :tag => s.version.to_s }
  s.requires_arc        = true

  s.platforms           = { :ios => '13.4', :osx => '10.13', :tvos => '16.0' }

  s.preserve_paths      = 'ios/**/*'
  s.source_files        = 'ios/**/*.{h,m,swift}'
  s.public_header_files = 'ios/**/*.h'
  s.swift_version       = '5.0'
  s.libraries           = 'c', 'sqlite3', 'stdc++'
  s.framework           = 'AudioToolbox','AVFoundation', 'CoreAudio', 'CoreGraphics', 'CoreVideo', 'GLKit', 'VideoToolbox'
  s.dependency          'React-Core'
  s.dependency          'FishjamWebRTC', '~> 124.0.2.0'
end
