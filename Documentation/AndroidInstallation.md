# Android Installation

Starting with React Native 0.60 due to a new auto linking feature, you won't need to follow the manual linking steps below but you
will need to follow the other steps if you plan on releasing an app to production.  
See a sample app in the `examples/GumTestApp` directory.  

## Manual Linking

This is not needed with React Native >= 0.60.

<details>
<summary>Show Instructions</summary>

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

In `android/app/main/AndroidManifest.xml` add the following permissions before the `<application>` section.  

```xml
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.autofocus" />
<uses-feature android:name="android.hardware.audio.output" />
<uses-feature android:name="android.hardware.microphone" />

<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.INTERNET" />
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

## Screen Capture Support - Android 10+

You'll need [Notifee](https://notifee.app/react-native/docs/overview) or another library which can handle foreground services for you.  
The basic requirement to get screen capturing working on Android 10 and above is to have a foreground service running with `mediaProjection` as a service type before you start screen capturing.  

In `android/app/main/AndroidManifest.xml` add the following inside the `<application>` section.  

```xml
<service
	android:name="app.notifee.core.ForegroundService"
	android:foregroundServiceType="mediaProjection|camera|microphone" />
```

To actually get screen capturing working you'll need to start the foreground service which includes an ongoing notification.  

You will be prompted for permission each time you want to initialise screen capturing.  
A notification channel is also required and created below.  

```javascript
import notifee, { AndroidImportance } from '@notifee/react-native';

try {
	const channelId = await notifee.createChannel( {
		id: 'screen_capture',
		name: 'Screen Capture',
		lights: false,
		vibration: false,
		importance: AndroidImportance.DEFAULT
	} );

	const notificationId = await notifee.displayNotification( {
		title: 'Screen Capture',
		body: 'This notification will be here until you stop capturing.',
		android: {
			channelId,
			asForegroundService: true
		}
	} );
} catch( err ) {
	// Handle Error
};
```

Once screen capturing has finished you should then cancel the notification.  
That will ofcourse also stop the foreground service.  

```javascript
try {
	await notifee.cancelNotification( notificationId );
} catch( err ) {
	// Handle Error
};
```

## Fatal Exception: java.lang.UnsatisfiedLinkError

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

If you're experiencing the error above then in `android/gradle.properties` add the following.  

```
# This one fixes a weird WebRTC runtime problem on some devices.
# https://github.com/jitsi/jitsi-meet/issues/7911#issuecomment-714323255

android.enableDexingArtifactTransform.desugaring=false
```