import {
  AndroidConfig,
  ConfigPlugin,
  createRunOncePlugin,
} from "@expo/config-plugins";
import { withBuildProperties } from "expo-build-properties";

import { withBitcodeDisabled } from "./withBitcodeDisabled";
import { withDesugaring } from "./withDesugaring";
import { IOSPermissionsProps, withPermissions } from "./withPermissions";

const pkg = require("react-native-webrtc/package.json");

const withWebRTC: ConfigPlugin<IOSPermissionsProps | void> = (
  config,
  props = {}
) => {
  const _props = props || {};

  // iOS
  config = withPermissions(config, _props);
  config = withBitcodeDisabled(config);

  // Android
  config = AndroidConfig.Permissions.withPermissions(config, [
    "android.permission.ACCESS_NETWORK_STATE",
    "android.permission.BLUETOOTH",
    "android.permission.CAMERA",
    "android.permission.INTERNET",
    "android.permission.MODIFY_AUDIO_SETTINGS",
    "android.permission.RECORD_AUDIO",
    "android.permission.SYSTEM_ALERT_WINDOW",
    "android.permission.WAKE_LOCK",
  ])
  
  config = withBuildProperties(config, {
    android: {
      // https://github.com/expo/expo/blob/sdk-46/templates/expo-template-bare-minimum/android/build.gradle#L8
      minSdkVersion: 24,
    },
  });;

  config = withDesugaring(config, true);

  return config;
};

export default createRunOncePlugin(withWebRTC, pkg.name, pkg.version);
