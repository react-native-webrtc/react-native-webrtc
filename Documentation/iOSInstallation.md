# iOS Installation

Starting with React Native 0.60 due to a new auto linking feature you no longer need to follow manual linking steps but you will need to follow the other steps below if you plan on releasing your app to production.  

See a sample app in the `examples/GumTestApp` directory.  

## Adjusting the supported platform version

**IMPORTANT:** Make sure you are using CocoaPods 1.10 or higher.  
You may have to change the `platform` field in your podfile.  
`react-native-webrtc` doesn't support iOS < 12 
Set it to '12.0' or above or you'll get an error when running `pod install`.

```
platform :ios, '12.0'
```

## Declaring Permissions

Navigate to `<ProjectFolder>/ios/<ProjectName>/` and edit `Info.plist`, add the following lines.

```xml
<key>NSCameraUsageDescription</key>
<string>Camera permission description</string>
<key>NSMicrophoneUsageDescription</key>
<string>Microphone permission description</string>
```

## Library not loaded/Code signature invalid

This is an issue with iOS 13.3.1.  
All dynamic frameworks being compiled to the newest release of iOS 13.3.1 are experiencing this issue when run on a personal provisioning profile/developer account.  
Use a non-Personal Team provisioning profile (paid developer account) to resolve the issue.  
You can find more information [here](https://stackoverflow.com/a/60090629/8691951).  
