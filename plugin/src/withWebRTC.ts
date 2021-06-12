import {
  AndroidConfig,
  ConfigPlugin,
  createRunOncePlugin,
} from "@expo/config-plugins";

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
  ]);

  config = withDesugaring(config, true);

  return config;
};

export default createRunOncePlugin(withWebRTC, pkg.name, pkg.version);
