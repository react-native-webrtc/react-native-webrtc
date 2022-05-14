[<img src="https://avatars.githubusercontent.com/u/42463376" alt="React Native WebRTC" style="height: 6em;" />](https://github.com/react-native-webrtc/react-native-webrtc)

# React-Native-WebRTC

[![npm version](https://img.shields.io/npm/v/react-native-webrtc)](https://www.npmjs.com/package/react-native-webrtc)
[![npm downloads](https://img.shields.io/npm/dm/react-native-webrtc)](https://www.npmjs.com/package/react-native-webrtc)
[![Discourse topics](https://img.shields.io/discourse/topics?server=https%3A%2F%2Freact-native-webrtc.discourse.group%2F)](https://react-native-webrtc.discourse.group/)

A WebRTC module for React Native.

## Feature Overview

|  | Android | iOS | macOS | Windows* | Web* | Expo* |
| :-: | :-------: | :---: | :-----: | :--------: | :----: | :-----: |
| Audio/Video | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | - | :heavy_check_mark: | :heavy_check_mark: |
| Data Channels | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | - | :heavy_check_mark: | :heavy_check_mark: |
| Screen Capture | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | - | :heavy_check_mark: | :heavy_check_mark: |
| Plan B | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | - | :heavy_check_mark: | :heavy_check_mark: |
| Unified Plan* | - | - | - | - | - | - |

> **Windows** - We don't currently support the [react-native-windows](https://github.com/microsoft/react-native-windows) platform at this time.  
Anyone interested in getting the ball rolling? We're open to contributions.

> **Web** - The [react-native-webrtc-web-shim](https://github.com/react-native-webrtc/react-native-webrtc-web-shim) project provides a shim for [react-native-web](https://github.com/necolas/react-native-web) support.  
Which will allow you to use [(almost)](https://github.com/react-native-webrtc/react-native-webrtc-web-shim/tree/main#setup) the exact same code in your [react-native-web](https://github.com/necolas/react-native-web) project as you would with [react-native](https://reactnative.dev/) directly.  

> **Expo** - Sadly this module is not available in the [Expo Go](https://expo.dev/client) app by default due to including much needed native code. However you can get things working via the [expo-dev-client](https://docs.expo.dev/development/getting-started/) library and out-of-tree [config-plugins/react-native-webrtc](https://github.com/expo/config-plugins/tree/master/packages/react-native-webrtc) package.  

> **Unified Plan** - Currently in development but expected to be released sometime in the near future.  
Plan B will remain default until adequate support and testing has been completed.  

## WebRTC Revision

* Currently used revision: [M100](https://github.com/jitsi/webrtc/releases/tag/v100.0.0)
* Supported architectures
  * Android: armeabi-v7a, arm64-v8a, x86, x86_64
  * iOS: arm64, x86_64 (for bitcode support, run [this script](./tools/downloadBitcode.sh))
  * macOS: x86_64

## Getting Started

Use one of the following preferred package install methods to immediately get going.  
Don't forget to follow platform guides below to cover any extra required steps.  

**npm:** `npm install react-native-webrtc --save`  
**yarn:** `yarn add react-native-webrtc`  

## Guides

- [Android Install](./Documentation/AndroidInstallation.md)
- [iOS Install](./Documentation/iOSInstallation.md)
- [Basic Usage](./Documentation/BasicUsage.md)
- [Step by Step Call Guide](./Documentation/CallGuide.md)
- [Improving Call Reliability](./Documentation/ImprovingCallReliability.md)

## Example Projects

We have some very basic example projects included in the [examples](./examples) directory.  
Don't worry, there are plans to include a much more broader example with backend included.  

## Community

Come join our [Discourse Community](https://react-native-webrtc.discourse.group/) if you want to discuss any React Native and WebRTC related topics.  
Everyone is welcome and every little helps.  

## Related Projects

Looking for extra functionality coverage?  
The [react-native-webrtc](https://github.com/react-native-webrtc) organization provides a number of packages which are more than useful when developing Real Time Communication applications.  