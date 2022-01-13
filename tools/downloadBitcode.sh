#!/bin/bash

set -euo pipefail

THIS_DIR=$(cd -P "$(dirname "$(readlink "${BASH_SOURCE[0]}" || echo "${BASH_SOURCE[0]}")")" && pwd)

pushd ${THIS_DIR}

export RN_WEBRTC_BITCODE=1
export RN_WEBRTC_FORCE_DOWNLOAD=1

node downloadWebRTC.js

popd
