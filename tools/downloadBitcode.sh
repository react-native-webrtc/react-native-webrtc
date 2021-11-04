#!/bin/bash

set -euo pipefail

THIS_DIR=$(cd -P "$(dirname "$(readlink "${BASH_SOURCE[0]}" || echo "${BASH_SOURCE[0]}")")" && pwd)
PACKAGE_VERSION=$(cat "${THIS_DIR}/../package.json" \
  | grep "\"version\":" \
  | head -1 \
  | awk -F: '{ print $2 }' \
  | sed 's/[",]//g' \
  | tr -d '[[:space:]]')
WEBRTC_DL="https://github.com/react-native-webrtc/react-native-webrtc/releases/download/${PACKAGE_VERSION}/WebRTC.tar.xz"


pushd "${THIS_DIR}/../apple"

# Cleanup
rm -rf WebRTC.xcframework WebRTC.dSYMs

# Download
echo "Downloading files..."
echo $PACKAGE_VERSION
curl -L -s ${WEBRTC_DL} | tar Jxf -
echo "Done!"

popd
