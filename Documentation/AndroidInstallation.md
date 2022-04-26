# Android Installation

Starting with React Native 0.60 auto-linking works out of the box, so there are no extra steps.  
See a sample app in the `examples/GumTestApp` directory.

## Manual Linking

This is not needed with React Native >= 0.60.

<details>
<summary>Show instructions</summary>

In `android/settings.gradle`, add WebRTCModule:

```gradle
include ':WebRTCModule', ':app'
project(':WebRTCModule').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-webrtc/android')
```

In `android/app/build.gradle`, add WebRTCModule to dependencies:

```gradle
dependencies {
  ...
  compile project(':WebRTCModule')
}
```

In your `MainApplication.java`:

```java
@Override
protected List<ReactPackage> getPackages() {
    return Arrays.<ReactPackage>asList(
        new MainReactPackage(),
        new com.oney.WebRTCModule.WebRTCModulePackage() // <-- Add this line
    );
}
```

</details>  

## Declaring Permissions

Locate your app's `AndroidManifest.xml` file and add these permissions:

```xml
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.autofocus" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

If you plan to also support Bluetooth devices then also add the following.

```xml
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

## Enable Java 8 Support

In `android/app/build.gradle` add the following inside the `android` section.

```gradle
compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
}
```

## R8/ProGuard Support

In `android/app/proguard-rules.pro` add the following on a new line.

```proguard
-keep class org.webrtc.** { *; }
```

## FAQ

### Fatal Exception: java.lang.UnsatisfiedLinkError

If you are getting this error:

```
Fatal Exception: java.lang.UnsatisfiedLinkError: No implementation found for void org.webrtc.PeerConnectionFactory.nativeInitializeAndroidGlobals() (tried Java_org_webrtc_PeerConnectionFactory_nativeInitializeAndroidGlobals and Java_org_webrtc_PeerConnectionFactory_nativeInitializeAndroidGlobals__)
       at org.webrtc.PeerConnectionFactory.nativeInitializeAndroidGlobals(PeerConnectionFactory.java)
       at org.webrtc.PeerConnectionFactory.initialize(PeerConnectionFactory.java:306)
       at com.oney.WebRTCModule.WebRTCModule.initAsync(WebRTCModule.java:79)
       at com.oney.WebRTCModule.WebRTCModule.lambda$new$0(WebRTCModule.java:70)
       at com.oney.WebRTCModule.-$$Lambda$WebRTCModule$CnyHZvkjDxq52UReGHUZlY0JsVw.run(-.java:4)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1162)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:636)
       at java.lang.Thread.run(Thread.java:764)
```

Add this line to `android/gradle.properties`:

```
# This one fixes a weird WebRTC runtime problem on some devices.
# https://github.com/jitsi/jitsi-meet/issues/7911#issuecomment-714323255

android.enableDexingArtifactTransform.desugaring=false
```