# Expo installation

> This package cannot be used in the "Expo Go" app because [it requires custom native code](https://docs.expo.io/workflow/customizing/).

First install the package with yarn, npm, or `npx expo install`.

```sh
npx expo install react-native-webrtc
```

## With Prebuild

If your app uses [Expo Prebuild](https://docs.expo.dev/workflow/prebuild/), then add the [config plugin](https://docs.expo.io/guides/config-plugins/) to the [`plugins`](https://docs.expo.io/versions/latest/config/app/#plugins) array of your `app.json` or `app.config.js`:

```json
{
  "expo": {
    "plugins": ["react-native-webrtc"]
  }
}
```

Next, rebuild your app as described in the ["Adding custom native code"](https://docs.expo.io/workflow/customizing/) guide.

## Without Prebuild

If your project is not using Expo Prebuild, then skip the config plugin and follow the manual setup guide for each platform:

- [Android Install](./AndroidInstallation.md)
- [iOS Install](./iOSInstallation.md)

Next, rebuild you app.

## API

The plugin provides props for extra customization. Every time you change the props or plugins, you'll need to rebuild (and `prebuild`) the native app. If no extra properties are added, defaults will be used.

- `cameraPermission` (_string_): Sets the iOS `NSCameraUsageDescription` permission message to the `Info.plist`. Defaults to `Allow $(PRODUCT_NAME) to access your camera`.
- `microphonePermission` (_string_): Sets the iOS `NSMicrophoneUsageDescription` permission message to the `Info.plist`. Defaults to `Allow $(PRODUCT_NAME) to access your microphone`.

`app.json`

```json
{
  "expo": {
    "plugins": [
      [
        "react-native-webrtc",
        {
          "cameraPermission": "Allow $(PRODUCT_NAME) to access your camera",
          "microphonePermission": "Allow $(PRODUCT_NAME) to access your microphone",
        }
      ]
    ]
  }
};
```

This plugin will also disable desugaring in the Android `gradle.properties`: `android.enableDexingArtifactTransform.desugaring=false`

