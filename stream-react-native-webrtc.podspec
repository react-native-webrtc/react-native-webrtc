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

  s.platform            = :ios, '13.0'

  s.preserve_paths      = 'ios/**/*'
  s.source_files        = 'ios/**/*.{h,m}'
  s.libraries           = 'c', 'sqlite3', 'stdc++'
  s.framework           = 'AudioToolbox','AVFoundation', 'CoreAudio', 'CoreGraphics', 'CoreVideo', 'GLKit', 'VideoToolbox'
  s.dependency          'React-Core'
  s.dependency          'StreamWebRTC', '~>125.6422.070'
  # Swift/Objective-C compatibility #https://blog.cocoapods.org/CocoaPods-1.5.0/
  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES'
  }
end
