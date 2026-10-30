---
name: daily-selftest
description: Build and run examples/DailySelfTest, the automated self-test for the Daily-specific features of this fork (native audio APIs, audio routing, device listing, devicechange events, camera switching, Android screen share), against the current checkout. Use after changing the fork or syncing with upstream, or when the user asks to run or validate the self-test.
---

# Daily self-test

`examples/DailySelfTest` is a minimal Expo app. On launch it runs the checks in `src/selfTest.ts`, which cover the fork APIs that react-native-daily-js and daily-js depend on. `scripts/run-selftest.sh` launches it, accepts Android's screen-share dialog, prints one line per check, and exits 1 unless the summary says `failed=0`.

Run everything in `examples/DailySelfTest/`.

```bash
npm run install-local                                   # packs this checkout (incl. uncommitted changes) and installs it; the repo root needs `npm install` first
npx expo prebuild --platform <android|ios> --no-install
npm run android   # or: npm run ios  (release build, installs on the running emulator/simulator; takes minutes, run in background)
npm test -- android   # or: npm test -- ios
```

- **Target:** an Android emulator or device (`adb devices`) or a booted iOS simulator (`xcrun simctl list devices booted`). Ask which one if that's unclear. To start one: `emulator -avd <name>` (from `emulator -list-avds`) or `xcrun simctl boot "<device>"`. The Android emulator covers the most checks, because it has virtual cameras.
- **Build errors:** a native compile error points at a broken patch. Show the end of the log, and name the patch that owns the file (`scripts/daily-patches/manifest.json`). If `npm run ios` only fails at the end with an `osascript` error ("Opening on ..."), the build was installed fine; that error comes from the Simulator window.
- **Results:** report the per-check lines `npm test` prints. For each `FAIL`, name the patch:

| Check | Patch |
|---|---|
| native: fork methods exist on WebRTCModule | the patch that owns the missing method: `camera-switch-facing-mode`, `devices-manager-devicechange` or `daily-audio-mode` |
| js: MediaStreamTrack._switchCamera/_getCameraFacingMode exist | `camera-switch-facing-mode` |
| js: mediaDevicesOnDeviceChange reaches navigator devicechange listeners | `devices-manager-devicechange` |
| enumerateDevices lists audio outputs | `devices-manager-devicechange` |
| setDailyAudioMode video/voice/idle | `daily-audio-mode` |
| setAudioDevice + getAudioDevice round trip | `devices-manager-devicechange` |
| camera: _switchCamera toggles facing mode / while muted | `camera-switch-facing-mode` |
| iOS: enableNoOpRecordingEnsuringBackgroundContinuity | `daily-audio-mode` |
| Android: getDisplayMedia starts screen capture | `android-screenshare-fgs` |

- **Expected skips:**
  - Android: 1 (the iOS-only check).
  - iOS simulator: 4 (the camera checks, the audio-route round trip, and screen share, which needs the broadcast picker).
- **Not covered:** hearing the audio route change, Bluetooth, `devicechange` on a real headset plug, and the app staying alive in the background on iOS. The user checks those on a real device.

When a patch adds or changes a Daily-facing API, add or update a `{ name, run }` entry in `src/selfTest.ts` (throw to fail, `skip()` to skip) and the table above. The app belongs to the `daily-selftest-app` patch, so change it through the patch queue.
