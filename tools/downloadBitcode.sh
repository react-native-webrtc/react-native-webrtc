#!/bin/bash

set -euo pipefail

# Files to be downloaded
WEBRTC_BUILD="M75-1"
WEBRTC_FRAMEWORK="https://dl.bintray.com/webrtc-builds/webrtc-builds/${WEBRTC_BUILD}/WebRTC.framework.tar.xz"
WEBRTC_DSYM="https://dl.bintray.com/webrtc-builds/webrtc-builds/${WEBRTC_BUILD}/WebRTC.dSYM.tar.xz"


THIS_DIR=$(cd -P "$(dirname "$(readlink "${BASH_SOURCE[0]}" || echo "${BASH_SOURCE[0]}")")" && pwd)

pushd ${THIS_DIR}/../ios

# Cleanup
rm -rf WebRTC.framework WebRTC.dSYM

# Download
echo "Downloading files..."
curl -L -s ${WEBRTC_FRAMEWORK} | tar Jxf -
curl -L -s ${WEBRTC_DSYM} | tar Jxf -
echo "Done!"

popd

