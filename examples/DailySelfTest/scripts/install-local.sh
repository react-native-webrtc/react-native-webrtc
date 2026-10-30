#!/usr/bin/env bash
# Packs this react-native-webrtc checkout exactly as it would be published to npm
# and installs it into the app. Re-run after every change to the library.
set -euo pipefail

APP="$(cd "$(dirname "$0")/.." && pwd)"
ROOT="$(cd "$APP/../.." && pwd)"

mkdir -p "$APP/.local"
rm -f "$APP"/.local/*.tgz
# npm pack runs the library's prepare script (bob build), so the root needs `npm install` first.
(cd "$ROOT" && npm pack --pack-destination "$APP/.local" >/dev/null)
mv "$APP"/.local/daily-co-react-native-webrtc-*.tgz "$APP/.local/react-native-webrtc.tgz"

cd "$APP"
rm -rf node_modules/@daily-co/react-native-webrtc
npm install
node -p "'installed @daily-co/react-native-webrtc ' + require('@daily-co/react-native-webrtc/package.json').version"
