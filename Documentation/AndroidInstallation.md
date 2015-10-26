

1.) In `android/app/src/main/AndroidManifest.xml` add these permissions

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.autofocus"/>

<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

2.) In `android/app/src/main/java/com/xxx/MainActivity.java`

```java
import com.oney.WebRTCModule.WebRTCModulePackage;  // <--- Add this line
...

 public class MainActivity extends Activity implements DefaultHardwareBackBtnHandler {
 ...
 
     .addPackage(new MainReactPackage())
     .addPackage(new WebRTCModulePackage())  // <--- Add this line
     .setUseDeveloperSupport(BuildConfig.DEBUG)
```

3.) In `android/settings.gradle`, includes WebRTCModule
```gradle
include ':WebRTCModule', ':app'
project(':WebRTCModule').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-webrtc/android')
```
