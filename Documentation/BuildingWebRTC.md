# Building WebRTC

This document shows how to prepare a WebRTC build for its inclusion in this
plugin.

The build will be made with the `build-webrtc.py` Python script located in the
`tools/` directory.

## Preparing the build

Running the script with `--setup` will download all necessary tools for building
WebRTC. The script must be run with a target directory where all WebRTC source
code and resulting build artifacts will be placed. A `build_webrtc` directory
will be created containing it all.

The setup process only needs to be carried out once.

### iOS

```
python build-webrtc.py --setup --ios ~/src/
```

### Android

NOTE: Make sure you have the Java JDK installed beforehand. On Debian and
Ubuntu systems this can be accomplished by installing the `default-jdk-headless`
package.

```
python build-webrtc.py --setup --android ~/src/
```

## Selecting the branch

Once the setup process has finished, the target branch must be selected, also
adding any required cherry-picks. The following example shows how the M57 branch
was made:

```
cd ~/src/build_webrtc/webrtc/ios/src/
git checkout -b build-M57 refs/remotes/branch-heads/57
git cherry-pick 0e22a4cfd3790d80ad1ae699891341fe322cb418
cd
```

Now the code is ready for building!

## Building

### iOS

```
python build-webrtc.py --build --ios ~/src/
```

The build artifacts will be located in `~/src/build_webrtc/build/ios/`.

### Android

**NOTE**: WebRTC for Android can only be built on Linux at the moment.

```
python build-webrtc.py --build --android ~/src/
```

The build artifacts will be located in `~/src/build_webrtc/build/android/`.

### Making debug builds

Debug builds can be made by adding `--debug` together with `--build`. For
example, to make a debug iOS build:

```
python build-webrtc.py --build --ios --debug ~/src/
```

