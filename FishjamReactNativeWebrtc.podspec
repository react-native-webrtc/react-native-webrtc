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

  s.preserve_paths      = 'ios/**/*', 'common/cpp/**/*'
  s.source_files        = 'ios/**/*.{h,m,mm,c,cpp,hpp,swift}', 'common/cpp/**/*.{h,c,cpp}'
  s.public_header_files = 'ios/**/*.h'
  # Keep shared C++ headers (miniaudio + JSI core) out of the public umbrella.
  s.private_header_files = 'common/cpp/**/*.h'
  s.swift_version       = '5.0'
  s.libraries           = 'c', 'sqlite3', 'stdc++'
  s.framework           = 'AudioToolbox','AVFoundation', 'CoreAudio', 'CoreGraphics', 'CoreVideo', 'GLKit', 'VideoToolbox'
  s.dependency          'React-Core'
  s.dependency          'FishjamWebRTC', '~> 124.0.2.2'
  # JSI audio-sink channel deps. React-jsi is universal; React-callinvoker is
  # present in new-arch-capable RN builds (guarded by FJ_HAS_CALL_INVOKER).
  s.dependency          'React-jsi'
  s.dependency          'React-callinvoker'

  # miniaudio conversion-only build. MA_NO_* must be global across all TUs
  # (miniaudio is not ABI-compatible across differing configs).
  s.pod_target_xcconfig = {
    'CLANG_CXX_LANGUAGE_STANDARD' => 'c++20',
    'HEADER_SEARCH_PATHS' => '"$(PODS_TARGET_SRCROOT)/common/cpp/vendor" "$(PODS_TARGET_SRCROOT)/common/cpp/fishjam-audio" "$(PODS_TARGET_SRCROOT)/common/cpp/fishjam-video"',
    'GCC_PREPROCESSOR_DEFINITIONS' =>
      'MA_NO_DEVICE_IO=1 MA_NO_DECODING=1 MA_NO_ENCODING=1 MA_NO_GENERATION=1 ' \
      'MA_NO_RESOURCE_MANAGER=1 MA_NO_NODE_GRAPH=1 $(inherited)',
  }
end
