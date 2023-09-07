# tvOS Installation

In order to use React Native on tvOS, you should use [react-native-tvos](https://www.npmjs.com/package/react-native-tvos) due to at the moment, tvOS support is deprecated on the lastes versions of React Native. We strongly recommend using React version 18.0.0. and React Native 0.69.8-2, adding this dependency to your `package.json` as follows: 
``` 
"react-native": "npm:react-native-tvos@0.69.8-2" 
```

## Adjusting the supported platform version

**IMPORTANT:** Make sure you are using CocoaPods 1.10 or higher.  
You may have to change the `platform` field in your podfile.  
`react-native-webrtc` doesn't support tvOS < 16. Set it to '16.0' or above.
Older versions of tvOS doesn't support WebRTC.

```
platform :tvos, '16.0'
```