require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name                = 'stream-react-native-webrtc'
  s.version             = package['version']
  s.summary             = package['description']
  s.homepage            = 'https://github.com/GetStream/react-native-webrtc'
  s.license             = package['license']
  s.author              = 'https://github.com/GetStream/react-native-webrtc/graphs/contributors'
  s.source              = { :git => 'git@github.com:GetStream/react-native-webrtc.git', :tag => 'release #{s.version}' }
  s.requires_arc        = true

  s.platforms           = { :ios => '12.0', :osx => '10.13', :tvos => '16.0' }

  s.preserve_paths      = 'ios/**/*'
  s.source_files        = 'ios/**/*.{h,m}'
  s.libraries           = 'c', 'sqlite3', 'stdc++'
  s.framework           = 'AudioToolbox','AVFoundation', 'CoreAudio', 'CoreGraphics', 'CoreVideo', 'GLKit', 'VideoToolbox'
  s.dependency          'React-Core'
  s.dependency          'JitsiWebRTC', '~> 124.0.0'
  # Swift/Objective-C compatibility #https://blog.cocoapods.org/CocoaPods-1.5.0/
  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES'
  }
end
