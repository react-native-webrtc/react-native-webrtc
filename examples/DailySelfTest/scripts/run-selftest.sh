#!/usr/bin/env bash
# Launches the installed DailySelfTest build on the running Android emulator/device
# or booted iOS simulator, prints the results, and exits 1 unless no check failed.
# Usage: run-selftest.sh android|ios
set -euo pipefail

APP_ID=co.daily.selftest
out=""

# Taps the first on-screen element whose text matches the regex (Android).
tap() {
  local b
  b=$(adb exec-out uiautomator dump /dev/tty 2>/dev/null | tr '>' '\n' |
    grep -E "text=\"($1)\"" | grep -o 'bounds="[^"]*"' | head -1 | tr -c '0-9\n' ' ')
  if [ -n "$b" ]; then
    set -- $b
    adb shell input tap $((($1 + $3) / 2)) $((($2 + $4) / 2))
  fi
}

case "${1:-}" in
android)
  for p in CAMERA RECORD_AUDIO POST_NOTIFICATIONS; do
    adb shell pm grant $APP_ID android.permission.$p
  done
  adb shell am force-stop $APP_ID
  adb logcat -c
  adb shell am start -n $APP_ID/.MainActivity >/dev/null
  for _ in $(seq 120); do
    out=$(adb logcat -d -s ReactNativeJS | grep -o 'SELFTEST\] .*' | cut -c11- || true)
    grep -q 'SELFTEST DONE' <<<"$out" && break
    # The last check asks to share the screen; labels differ between Android versions.
    if adb exec-out uiautomator dump /dev/tty 2>/dev/null | grep -q 'Share your screen\|Start recording\|Start now'; then
      tap 'A single app|Share one app' && sleep 1
      tap 'Entire screen|Share entire screen' && sleep 1
      tap 'Start now|Start|Share screen'
    fi
    sleep 1
  done
  ;;
ios)
  for s in camera microphone; do
    xcrun simctl privacy booted grant $s $APP_ID
  done
  xcrun simctl terminate booted $APP_ID 2>/dev/null || true
  prefs="$(xcrun simctl get_app_container booted $APP_ID data)/Library/Preferences/$APP_ID.plist"
  rm -f "$prefs"
  xcrun simctl launch booted $APP_ID >/dev/null
  for _ in $(seq 120); do
    out=$(plutil -extract selftest raw "$prefs" 2>/dev/null || true)
    grep -q 'SELFTEST DONE' <<<"$out" && break
    sleep 1
  done
  ;;
*)
  echo "usage: $0 android|ios"
  exit 2
  ;;
esac

echo "${out:-no result (timed out)}"
grep -q 'SELFTEST DONE failed=0 ' <<<"$out"
