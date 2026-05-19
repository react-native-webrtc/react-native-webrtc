[<img src="https://avatars.githubusercontent.com/u/42463376" alt="React Native WebRTC" style="height: 6em;" />](https://github.com/react-native-webrtc/react-native-webrtc)

# React-Native-WebRTC

[![npm version](https://img.shields.io/npm/v/react-native-webrtc)](https://www.npmjs.com/package/react-native-webrtc)
[![npm downloads](https://img.shields.io/npm/dm/react-native-webrtc)](https://www.npmjs.com/package/react-native-webrtc)
[![Discourse topics](https://img.shields.io/discourse/topics?server=https%3A%2F%2Freact-native-webrtc.discourse.group%2F)](https://react-native-webrtc.discourse.group/)

A WebRTC module for React Native.

## Feature Overview

|  | Android | iOS | tvOS | macOS* | Windows* | Web* | Expo* |
| :- | :-: | :-: | :-: | :-: | :-: | :-: | :-: |
| Audio/Video | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | - | - | :heavy_check_mark: | :heavy_check_mark: |
| Data Channels | :heavy_check_mark: | :heavy_check_mark: | - | - | - | :heavy_check_mark: | :heavy_check_mark: |
| Screen Capture | :heavy_check_mark: | :heavy_check_mark: | - | - | - | :heavy_check_mark: | :heavy_check_mark: |
| Plan B | - | - | - | - | - | - | - |
| Unified Plan* | :heavy_check_mark: | :heavy_check_mark: | - | - | - | :heavy_check_mark: | :heavy_check_mark: |
| Simulcast* | :heavy_check_mark: | :heavy_check_mark: | - | - | - | :heavy_check_mark: | :heavy_check_mark: |

> **macOS** - We don't currently actively support macOS at this time.  
Support might return in the future.

> **Windows** - We don't currently support the [react-native-windows](https://github.com/microsoft/react-native-windows) platform at this time.  
Anyone interested in getting the ball rolling? We're open to contributions.

> **Web** - The [react-native-webrtc-web-shim](https://github.com/react-native-webrtc/react-native-webrtc-web-shim) project provides a shim for [react-native-web](https://github.com/necolas/react-native-web) support.  
Which will allow you to use [(almost)](https://github.com/react-native-webrtc/react-native-webrtc-web-shim/tree/main#setup) the exact same code in your [react-native-web](https://github.com/necolas/react-native-web) project as you would with [react-native](https://reactnative.dev/) directly.  

> **Expo** - As this module includes native code it is not available in the [Expo Go](https://expo.dev/client) app by default.  
However you can get things working via the [expo-dev-client](https://docs.expo.dev/development/getting-started/) library and out-of-tree [config-plugins/react-native-webrtc](https://github.com/expo/config-plugins/tree/master/packages/react-native-webrtc) package.  

> **Unified Plan** - As of version 106.0.0 Unified Plan is the only supported mode.  
Those still in need of Plan B will need to use an older release.

> **Simulcast** - As of version 111.0.0 Simulcast is now possible with ease.  
Software encode/decode factories have been enabled by default.

## WebRTC Revision

* Currently used revision: [M124 (Fishjam fork)](https://github.com/fishjam-cloud/webrtc/tree/fishjam-m124)
* iOS dependency: [`FishjamWebRTC`](https://cocoapods.org/pods/FishjamWebRTC) — Fishjam's custom WebRTC build with the `defer mic permission` patch (replaces `JitsiWebRTC`).
* Supported architectures
  * Android: armeabi-v7a, arm64-v8a, x86, x86_64
  * iOS: arm64, x86_64
  * tvOS: arm64
  * macOS: arm64, x86_64

## Maintaining the iOS WebRTC build (FishjamWebRTC)

The iOS side ships against `FishjamWebRTC` — a CocoaPods pod published from [`fishjam-cloud/webrtc`](https://github.com/fishjam-cloud/webrtc) that wraps a prebuilt `WebRTC.xcframework`. Source patches live on the `fishjam-m124` branch; the podspec and release tooling live on `master`.

### When to cut a new release

Cut a new `FishjamWebRTC` version when **any of the following** happens:

- A new patch lands on `fishjam-m124` that we need on the client (e.g. another `audio_device_ios.mm` change, a fix to the network interfaces path, a privacy-manifest update).
- We rebase `fishjam-m124` onto a newer jitsi/webrtc tag (e.g. moving from `v124.0.2` to `v124.0.3`).
- A security advisory in upstream WebRTC requires picking up a fix.

You do **not** need to cut a new `FishjamWebRTC` for changes that only touch this RN wrapper (`ios/**.{h,m,swift}`, JS sources). Those ship as a normal `@fishjam-cloud/react-native-webrtc` npm bump.

### Versioning

`<upstream-version>.<fishjam-patch-N>`, e.g. `124.0.2.1`.

- First three parts = the jitsi upstream version the build is patched on top of.
- Fourth part = Fishjam patch counter against that base, starts at `1`, increments per release.
- On upstream rebase (e.g. jitsi ships `124.0.3`), reset counter: next release is `124.0.3.1`.
- Pin with `~> 124.0.2.0` to accept any Fishjam patch on top of jitsi `124.0.2`, or pin exact.

### How to cut a release

Full runbook (build, tag, GH release, trunk publish, smoke test) lives in [`fishjam-cloud/webrtc`'s `RELEASING.md`](https://github.com/fishjam-cloud/webrtc/blob/master/RELEASING.md) on the `master` branch. TL;DR:

1. On a macOS build host with [`depot_tools`](https://chromium.googlesource.com/chromium/tools/depot_tools.git) + a `fetch webrtc_ios` checkout, switch the gclient `src/` to `fishjam-m124`, run `gclient sync`, then `tools_webrtc/ios/build_ios_libs.py --build_config release --arch device:arm64 simulator:arm64 simulator:x64`.
2. Validate the patch is in the built binary: `strings WebRTC.xcframework/*/WebRTC.framework/WebRTC | grep -c "<unique-string-from-patch>"` — every slice must have a non-zero count. (Release builds strip C++ symbols, so use `strings`, not `nm`.)
3. Zip the xcframework as `FishjamWebRTC.xcframework.zip`, tag `vX.Y.Z.N` on `fishjam-m124`, push, and `gh release create` with the zip attached.
4. On `master`: bump `s.version` in `ios/FishjamWebRTC.podspec` to match, commit, push.
5. `pod trunk push ios/FishjamWebRTC.podspec --allow-warnings` (one-time `pod trunk register` per identity).

### After a new FishjamWebRTC release

Inside this repo:

1. Bump the dependency in `FishjamReactNativeWebrtc.podspec`: `s.dependency 'FishjamWebRTC', '~> X.Y.Z.0'` (or pin exact if needed).
2. Bump `version` in `package.json` — minor bump if the underlying WebRTC change is user-observable, patch otherwise.
3. Test with `cd ios && pod install` in a consuming app and exercise the affected behavior end-to-end.

## Getting Started

Use one of the following preferred package install methods to immediately get going.  
Don't forget to follow platform guides below to cover any extra required steps.  

**npm:** `npm install react-native-webrtc --save`  
**yarn:** `yarn add react-native-webrtc`  
**pnpm:** `pnpm install react-native-webrtc`  

## Guides

- [Android Install](./Documentation/AndroidInstallation.md)
- [iOS Install](./Documentation/iOSInstallation.md)
- [tvOS Install](./Documentation/tvOSInstallation.md)
- [Basic Usage](./Documentation/BasicUsage.md)
- [Step by Step Call Guide](./Documentation/CallGuide.md)
- [Improving Call Reliability](./Documentation/ImprovingCallReliability.md)
- [Migrating to Unified Plan](https://docs.google.com/document/d/1-ZfikoUtoJa9k-GZG1daN0BU3IjIanQ_JSscHxQesvU/edit#heading=h.wuu7dx8tnifl)

## Example Projects

We have some very basic example projects included in the [examples](./examples) directory.  
Don't worry, there are plans to include a much more broader example with backend included.  

## Community

Come join our [Discourse Community](https://react-native-webrtc.discourse.group/) if you want to discuss any React Native and WebRTC related topics.  
Everyone is welcome and every little helps.  

## Related Projects

Looking for extra functionality coverage?  
The [react-native-webrtc](https://github.com/react-native-webrtc) organization provides a number of packages which are more than useful when developing Real Time Communication applications.  
