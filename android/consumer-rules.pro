# WebRTC
-keep class org.webrtc.** { *; }

# E2EE frame cryptor: encryptFrame/decryptFrame are called from native code.
-keep class com.oney.WebRTCModule.E2EEFrameCryptorManager { *; }
