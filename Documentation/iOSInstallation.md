# iOS Installation

Starting with React Native 0.60 due to a new auto linking feature, you won't need to follow the manual linking steps below but you
will need to follow the other steps if you plan on releasing an app to production.  
See a sample app in the `examples/GumTestApp` directory.  

**IMPORTANT:** Make sure you are using CocoaPods 1.10 or higher.  

## Manual Linking (using CocoaPods)

This is not needed with React Native >= 0.60.

<details>
<summary>Show instructions</summary>

You can use the included podspec in your Podfile to take care of all dependencies.  
Include in the Podfile in your react-native ios directory:

```
pod 'react-native-webrtc', :path => '../node_modules/react-native-webrtc'
```

</details>

## Adjusting the supported platform version

You may have to change the `platform` field in your Podfile, as `react-native-webrtc` doesn't support iOS < 11 - set it to '11.0' or above (otherwise you get an error when doing `pod install`):

```
platform :ios, '11.0'
```

## Declaring Permissions

Navigate to `<ProjectFolder>/ios/<ProjectName>/` and edit `Info.plist` adding the following lines:

```xml
<key>NSCameraUsageDescription</key>
<string>Camera permission description</string>
<key>NSMicrophoneUsageDescription</key>
<string>Microphone permission description</string>
```

## FAQ

### Library not loaded/Code signature invalid

This is an issue with iOS 13.3.1. All dynamic frameworks being compiled to the newest release of iOS 13.3.1 are experiencing this issue when run on a personal provisioning profile/developer account. Use a non-Personal Team provisioning profile (paid developer account).  
Source (https://stackoverflow.com/a/60090629/8691951)