# DailySelfTest

Minimal app that checks the Daily-specific features of this fork on an emulator, simulator or device: native audio APIs, audio routing, device listing, `devicechange` events, camera switching and Android screen share. It runs the checks in `src/selfTest.ts` on launch.

```bash
npm install                 # in the repo root, once
cd examples/DailySelfTest
npm run install-local       # packs this checkout (as published) and installs it
npx expo prebuild --platform android --no-install
npm run android             # release build on the running emulator/device (or: ios)
npm test -- android         # prints each check; exits 1 unless failed=0 (or: ios)
```
