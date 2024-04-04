# Android Installation

Starting with React Native 0.60 due to a new auto linking feature you no longer need to follow manual linking steps but you will need to follow the other steps below if you plan on releasing your app to production.  

See a sample app in the `examples/GumTestApp` directory.  

**IMPORTANT:** Android API level >= 24 are supported.

## Declaring Permissions

In `android/app/src/main/AndroidManifest.xml` add the following permissions before the `<application>` section.  

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

### Screen-sharing

Starting with version 118.0.2 a foreground service is included in this library in order to make
screen-sharing possible under Android 14 rules.

If you want to enable it, first declare the following permissions:

```xml
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
```

Then enable the builtin service as follows, by adding the following code very early in your
application, in your main activity's `onCreate` for instance:

```java
    // Initialize the WebRTC module options.
    WebRTCModuleOptions options = WebRTCModuleOptions.getInstance();
    options.enableMediaProjectionService = true;
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

You'll need [Notifee](https://notifee.app/react-native/docs/overview) or another library that can handle foreground services for you.  
The basic requirement to get screen capturing working since Android 10 and above is to have a foreground service with `mediaProjection` included as a service type and to have that service running before starting a screen capture session.  

In `android/app/main/AndroidManifest.xml` add the following inside the `<application>` section.  

```xml
<service
	android:name="app.notifee.core.ForegroundService"
	android:foregroundServiceType="mediaProjection|camera|microphone" />
```

Additionally, add the respective foreground service type permissions before the `<application>` section.

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
```

The following will create an ongoing persistent notification which also comes with a foreground service.  
You will be prompted for permissions automatically each time you want to initialise screen capturing.  
A notification channel is also required and created.  

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

	await notifee.displayNotification( {
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

Once screen capturing has finished you should then stop the foreground service.  
Usually you'd run a notification cancellation function but as the service is involved, instead run the following.  

```javascript
try {
	await notifee.stopForegroundService();
} catch( err ) {
	// Handle Error
};
```

Lastly, you'll need to add this to your project's main `index.js` file.  
Otherwise, you'll receive errors relating to the foreground service not being registered correctly.  

```javascript
notifee.registerForegroundService( notification => {
	return new Promise( () => {

	} );
} );
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

## Crash on Android 9 NoSuchMethodError on java/nio/FloatBuffer.position

After upgrading to the latest M106 version we have a working app on iOS and android 10 and higher.
We expect the App to work (not crash) on android 9.

#### Observed Behavior

On Android 9 we get a crash (the same as reported [here](https://community.jitsi.org/t/jitsi-android-sdk-crash/120099) )

```
NoSuchMethodError 
No virtual method position(I)Ljava/nio/FloatBuffer; in class Ljava/nio/FloatBuffer; or its super classes (declaration of 'java.nio.FloatBuffer' appears in /system/framework/core-oj.jar))
```

Stacktrace

```
org.webrtc.GlUtil in createFloatBuffer at line 47
org.webrtc.GlGenericDrawer in <clinit> at line 75
org.webrtc.YuvConverter in <init> at line 111
org.webrtc.YuvConverter in <init> at line 118
org.webrtc.SurfaceTextureHelper in create at line 92
com.oney.WebRTCModule.GetUserMediaImpl in createVideoTrack at line 376
com.oney.WebRTCModule.GetUserMediaImpl in getUserMedia at line 206
com.oney.WebRTCModule.WebRTCModule in lambda$getUserMedia$10$WebRTCModule at line 742
com.oney.WebRTCModule.-$$Lambda$WebRTCModule$4OOJLDjizOGGW7tkUrJ04dyPO_A in run at line 8
java.util.concurrent.ThreadPoolExecutor in runWorker at line 1167
java.util.concurrent.ThreadPoolExecutor$Worker in run at line 641
java.lang.Thread in run at line 764
```

From a more specific [similar Error](https://github.com/msgpack/msgpack-java/issues/516) explaining FloatBuffer change in java 8 and in java 11.

We are verifying that the jar files in android/libs/ are compiled for JRE11 (Major version 55), 

We think that the library being compiled against the JRE 11 runtime and osVersion 9 seems to use the Java 8 context where the classByteBuffer inherits the position method from Buffer and has a return type of java.nio.Buffer. 
(On Java 11 the method is overridden with an implementation that returns java.nio.ByteBuffer.)

We do not understand why it works on osVersion higher than 9 (maybe /system/framework/core-oj.jar is the difference...).

We also looked further into 
- https://github.com/react-native-webrtc/react-native-webrtc/issues/1026
- https://github.com/react-native-webrtc/react-native-webrtc/issues/720
- https://github.com/react-native-webrtc/react-native-webrtc/issues/845
and tried changing our current minSdkVersion from 23 to 24.
But this still resulted in the same issue (NoSuchMethodError No virtual method position(I)Ljava/nio/FloatBuffer; in class Ljava/nio/FloatBuffer; or its super classes (declaration of 'java.nio.FloatBuffer' appears in /system/framework/core-oj.jar))

#### Steps to reproduce the issue

Start a React-native app with a WebRtc video session using the latest version M106 on android 9 that uses getUserMedia and trigger the createFloatBuffer function (See above stacktrace).

#### Platform Information

* **React Native Version**: 0.66.5 
* **Plugin Version**:  106.0.4
* **OS**: Android 9 (multiple devices tested (simulator and real devices))
* **OS Version**: MacOS Monterey 12.6
* With correct pro-guard config (as specified in the readme).

We already were using java11 (zulu11) so that is not influencing this as far as we can determine.
If we use the following in android/app/build.gradle
```
    compileOptions {
      sourceCompatibility JavaVersion.VERSION_1_8
      targetCompatibility JavaVersion.VERSION_11
    }
```
And upgrading gradle from 6.9 to 7.4 and gradle tools to 7.3.1 it works. 
