# fishjam-react-native-webrtc

React Native WebRTC native module (npm; `package-lock.json`).

## Cursor Cloud specific instructions

Node (via nvm) is pre-installed; the startup script runs `npm install`. Non-obvious notes:

- Validated command: `npm run lint` (runs `eslint --max-warnings 0` + `tsc --noEmit`, passes).
- This is a native module: there are no headless-buildable JS bundles here, and iOS/Android builds require Xcode / the Android SDK (not available in the cloud VM). `npm run lint` is the meaningful local check.
