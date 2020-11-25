#!/bin/bash

set -euo pipefail

# Files to be downloaded
WEBRTC_BUILD="M87-1"
WEBRTC_FILE="https://dl.bintray.com/webrtc-builds/webrtc-builds/${WEBRTC_BUILD}/WebRTC.tar.xz"


THIS_DIR=$(cd -P "$(dirname "$(readlink "${BASH_SOURCE[0]}" || echo "${BASH_SOURCE[0]}")")" && pwd)

pushd ${THIS_DIR}/../apple

# Cleanup
rm -rf WebRTC.xcframework WebRTC.dSYMs

# Download
echo "Downloading files..."
curl -L -s ${WEBRTC_FILE} | tar Jxf -
echo "Done!"

popd
