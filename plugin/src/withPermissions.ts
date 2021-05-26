import { ConfigPlugin, withInfoPlist } from "@expo/config-plugins";

const CAMERA_USAGE = "Allow $(PRODUCT_NAME) to access your camera";
const MICROPHONE_USAGE = "Allow $(PRODUCT_NAME) to access your microphone";

export type IOSPermissionsProps = {
  cameraPermission?: string;
  microphonePermission?: string;
};

export const withPermissions: ConfigPlugin<IOSPermissionsProps | void> = (
  config,
  props
) => {
  return withInfoPlist(config, (config) => {
    const { cameraPermission, microphonePermission } = props || {};

    config.modResults.NSCameraUsageDescription =
      cameraPermission ||
      config.modResults.NSCameraUsageDescription ||
      CAMERA_USAGE;

    config.modResults.NSMicrophoneUsageDescription =
      microphonePermission ||
      config.modResults.NSMicrophoneUsageDescription ||
      MICROPHONE_USAGE;

    return config;
  });
};
