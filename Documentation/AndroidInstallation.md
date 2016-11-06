

1.) In `android/app/src/main/AndroidManifest.xml` add these permissions

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.autofocus"/>

<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

2.) In `android/settings.gradle`, includes WebRTCModule
```gradle
include ':WebRTCModule', ':app'
project(':WebRTCModule').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-webrtc/android')
```

3.) In `android/app/build.gradle`, add WebRTCModule to dependencies
```gradle
dependencies {
  ...
  compile project(':WebRTCModule')
}

```

4.) In `android/app/src/main/java/com/xxx/MainApplication.java`

After 0.19.0
```java
import com.oney.WebRTCModule.WebRTCModulePackage;  // <--- Add this line
...
    @Override
    protected List<ReactPackage> getPackages() {
      return Arrays.<ReactPackage>asList(
        new MainReactPackage(),
        new WebRTCModulePackage()                  // <--- Add this line
      );
    }
```
Before 0.18.0
```java
import com.oney.WebRTCModule.WebRTCModulePackage;  // <--- Add this line
...

 public class MainActivity extends Activity implements DefaultHardwareBackBtnHandler {
 ...

     .addPackage(new MainReactPackage())
     .addPackage(new WebRTCModulePackage())        // <--- Add this line
     .setUseDeveloperSupport(BuildConfig.DEBUG)
```
