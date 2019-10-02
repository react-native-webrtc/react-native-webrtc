

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

React Native versions 0.60.0 after
```java
import com.oney.WebRTCModule.WebRTCModulePackage;  // <--- Add this line
...
    @Override
    protected List<ReactPackage> getPackages() {
      @SuppressWarnings("UnnecessaryLocalVariable")
      List<ReactPackage> packages = new PackageList(this).getPackages();
      // Packages that cannot be autolinked yet can be added manually here, for example:
      // packages.add(new MyReactNativePackage());
      packages.add(new WebRTCModulePackage()); // <-- Add this line
      return packages;
    }
```
React Native versions 0.60.0 before
```java
import com.oney.WebRTCModule.WebRTCModulePackage;  // <--- Add this line
...

@Override
protected List<ReactPackage> getPackages() {
    return Arrays.<ReactPackage>asList(
        new MainReactPackage(),
        new WebRTCModulePackage() // <-- Add this line
    );
}
```

5.) Enable Java 8 support in your project. You will probably need to have React Native 0.55+ for this.

5.a.) In `android/app/build.gradle` add inside `android` section:
```gradle
compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
}
```

5.b.) In `android/build.gradle` replace to:

```gradle
dependencies {
  classpath 'com.android.tools.build:gradle:3.0.1'
}

//...

ext {
  //...
  compileSdkVersion 27
  buildToolsVersion '27.0.3'
  //...
}
```

5.c.) In `android/gradle/wrapper/gradle-wrapper.properties` set `distributionUrl` variable to
```
distributionUrl=https\://services.gradle.org/distributions/gradle-4.1-all.zip
```


## CLEAN PROCESS

if you encounter any build time errors, like "linking library not found",  
try the cleaning steps below, and do it again carefully with every steps.

1. remove npm module: `rm -rf $YourProject/node_modules/react-native-webrtc`
2. clean npm cache: `npm cache clean`
3. clear temporary build files ( depends on your env )
    * ANDROID: clear intermediate files in `gradle buildDir`
    * iOS: in xocde project, click `Product` -> `clean`
4. `npm install react-native-webrtc`
