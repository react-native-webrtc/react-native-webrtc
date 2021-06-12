# Expo installation

> This package cannot be used in the "Expo Go" app because [it requires custom native code](https://docs.expo.io/workflow/customizing/).

First install the package with yarn, npm, or [`expo install`](https://docs.expo.io/workflow/expo-cli/#expo-install).

```sh
expo install react-native-webrtc
```

After installing this npm package, add the [config plugin](https://docs.expo.io/guides/config-plugins/) to the [`plugins`](https://docs.expo.io/versions/latest/config/app/#plugins) array of your `app.json` or `app.config.js`:

```json
{
  "expo": {
    "plugins": ["react-native-webrtc"]
  }
}
```

Next, rebuild your app as described in the ["Adding custom native code"](https://docs.expo.io/workflow/customizing/) guide.

## API

The plugin provides props for extra customization. Every time you change the props or plugins, you'll need to rebuild (and `prebuild`) the native app. If no extra properties are added, defaults will be used.

- `cameraPermission` (_string_): Sets the iOS `NSCameraUsageDescription` permission message to the `Info.plist`. Defaults to `Allow $(PRODUCT_NAME) to access your camera`.
- `microphonePermission` (_string_): Sets the iOS `NSMicrophoneUsageDescription` permission message to the `Info.plist`. Defaults to `Allow $(PRODUCT_NAME) to access your microphone`.

`app.config.js`

```ts
export default {
  plugins: [
    [
      "react-native-webrtc",
      {
        cameraPermission: "Allow $(PRODUCT_NAME) to access your camera",
        microphonePermission: "Allow $(PRODUCT_NAME) to access your microphone",
      },
    ],
  ],
};
```

This plugin will also disable desugaring in the Android `gradle.properties`: `android.enableDexingArtifactTransform.desugaring=false`

## Manual Setup

For bare workflow projects, you can follow the manual setup guides:

- [iOS](https://github.com/react-native-webrtc/react-native-webrtc/blob/master/Documentation/iOSInstallation.md)
- [Android](https://github.com/react-native-webrtc/react-native-webrtc/blob/master/Documentation/AndroidInstallation.md)
