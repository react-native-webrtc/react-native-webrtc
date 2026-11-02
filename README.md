> **This is `@fluxlabs/react-native-webrtc`**, a fork of [react-native-webrtc](https://github.com/react-native-webrtc/react-native-webrtc) 155.0.0 that adds seamless picture-in-picture on Android and the system player's picture-in-picture on iOS. The sections after this one are upstream's README.

## About this fork

Upstream **155.0.0**, plus picture-in-picture for `RTCView` on Android. It starts from [react-native-webrtc#1710](https://github.com/react-native-webrtc/react-native-webrtc/pull/1710) by [@EdgarJMesquita](https://github.com/EdgarJMesquita), reworked so the transition is seamless:

- The video replaces the screen as the transition starts (Android 15+ reports it; earlier versions when the activity pauses into PiP), so the shrink animation shows the video, not the controls drawn over it.
- A still of the last frame covers the moved video surface until it draws again, then fades out, so there is no black flash and no hard cut. It sits where the view is drawn, transforms included.
- The PiP window takes the video's shape. With `objectFit="contain"` it animates from where the video is on screen; with `cover` from the whole window, since Android refuses a source smaller than the PiP window (a view scaled into a small preview, say) and animates the app's icon instead.
- Leaving the app starts PiP on Android 8 and later; `startPictureInPicture()` starts it on request.

### Install

Install it under the upstream name, so imports and native project names do not change:

```sh
yarn add react-native-webrtc@npm:@fluxlabs/react-native-webrtc@155.2.2
```

### Picture-in-picture

```tsx
import { RTCView, isInPictureInPicture, isPictureInPictureSupported } from 'react-native-webrtc';

<RTCView
    streamURL={streamURL}
    objectFit="cover"
    pictureInPictureEnabled
    autoStartPictureInPicture
    onPictureInPictureChange={(active, { dismissed }) => {
        // dismissed: the viewer closed the window rather than returning to the app.
    }}
    onPictureInPicturePlayingChange={(playing) => {
        // iOS: the viewer stopped or played the video in the window.
    }}
/>
```

- `ref.current?.startPictureInPicture()` enters on request.
- `floatingPictureInPicture` puts the video in PiP over another app the app opens itself, like a UPI app for a payment, which the system's own PiP misses. It closes when the prop turns false or the viewer comes back to the app.
  - Android opens it as the prop turns true, with the app in front, once each time. The video shows in a window of its own, with the app full screen behind it. The app's own PiP takes its whole task along, so an activity the app opens in that task, like a UPI app started for a result, would open inside the window or end it; this one opens under the window and returns its result as usual. Android 13 and later start it straight in the window.
  - iOS starts its PiP as the app leaves only from a view on screen, and a call to start it from the app fails while the layer is out of sight. So while the prop is true, a view a screen of the app's own covers lends its player layer to the app's window, behind everything there, and the system starts the window from it as the app leaves.
- `isPictureInPictureSupported()` resolves `false` when the device lacks PiP or the user turned it off for the app.
- `isInPictureInPicture()` answers synchronously. On Android, AppState reports `background` as soon as the activity pauses into PiP, sometimes before `onPictureInPictureChange` arrives. Android 12 and 13 pause it before they say it is in PiP, so there a pause with automatic entry on, the screen on and unlocked counts as PiP starting. If no window follows, `onPictureInPictureChange` reports it closed: `dismissed` when the app stopped or two seconds passed, not when it came back.
- One view manages PiP at a time: the most recently enabled one. Automatic entry waits for a video track, and on iOS for the view to be on screen: a view on a screen the app has moved away from does not start the window.
- On iOS the video floats in the system player's window, as live video does, with the system's controls in its own look (Liquid Glass from iOS 26): a LIVE badge and the live bar, stop and play, and the buttons to close the window and to return to the app. Its skip buttons are greyed out, as a live stream has nothing to skip to. The window takes the video's shape, upright and mirrored as the view shows it.
- Stopping there holds the last frame and silences the stream's remote audio; play goes back to live. Closing the window with the app in the background stops it too, as the system's own players do, and returning to the app plays it. The stream still arrives while stopped: `onPictureInPicturePlayingChange` says when, so an app can end it over a long stop.
- `pictureInPictureKind="videoCall"` uses the video call's window instead, which has no controls: Apple meant it for a call. It takes the screen's shape, unless `pictureInPicturePreferredSize` gives one (only its shape counts: the system picks the size), and shows the view's children until the video has a frame.

Android needs `android:supportsPictureInPicture="true"` on the activity, `configChanges` that include `screenSize|smallestScreenSize|screenLayout`, and compileSdk 36 (androidx.activity 1.13). While in PiP the activity is paused, and React Native does not run JS timers for a paused activity. iOS needs iOS 15 and the `audio` background mode. The Expo config plugin's `pictureInPicture` option sets both up.

### Muting

```tsx
<RTCView streamURL={streamURL} muted={muted} />
```

- `muted` silences the remote audio of the stream the view shows, as a video element's does. Remote audio plays whether or not a view shows its stream, so this is how a player mutes it, with no tracks to find.
- The audio plays again when `muted` turns false, when the view shows another stream, and when the view goes away.
- Audio tracks that join the stream while it is muted are silent too, as when the server renegotiates. A track whose `enabled` is set meanwhile takes that state when the audio plays again.
- With more than one view on a stream, it stays silent while any of them is muted, or, on iOS, while a picture-in-picture window stopped on it.

### Livestreaming

For a live platform: viewers watch, hosts go live. The two sides are independent. A viewer plays whatever the server has, however the host published it: WHIP from this library, or RTMP or SRT from an encoder or another app.

- **Livestream audio**, built in and on from launch: remote audio plays as media through a voice leveller, optionally behind live to ride out a poor network, and a host's microphone goes out as it sounds. It replaces [react-native-webrtc-playback](#from-react-native-webrtc-playback).
- **`WHEPClient`** plays a stream from a WHEP endpoint, and keeps playing through a weak or changing network: it waits for a host who has not started, follows the phone from network to network, says how watching is going, and can fall back to audio when the network cannot carry the video. See [Weak and changing networks](#weak-and-changing-networks).
- **`LivestreamViewer`** is the same player for a server with signalling of its own: implement the signalling, and the rest comes with it.
- **`WHIPClient`** publishes a camera and microphone to a WHIP endpoint, tuned for broadcast rather than calls.
- **`LivestreamStatsSampler`** turns `getStats()` into what a quality overlay or a QoE report needs, with a judgement of the quality and of what limits it. The players sample for themselves.
- **`LivestreamWatchReport`** adds a whole watch's stats into one QoE report, across reconnects: how often and how long the picture stood still, the resolution and delay it played at, audio gaps, round trip and relay, and the time at each quality.

WHIP and WHEP are the IETF's WebRTC publishing and playback protocols: mediamtx, SRS, OvenMediaEngine, Janus, Cloudflare Stream, Dolby/Millicast, Wowza and Red5 speak both, and Ant Media takes WHIP. [`examples/LivestreamExample`](examples/LivestreamExample) is an Expo app with both sides; it also plays from Ant Media over its WebSocket signalling, as a `LivestreamViewer`.

#### Setup

With Expo, in `app.json`. Every option is optional:

```json
["react-native-webrtc", {
  "cameraPermission": "Allow $(PRODUCT_NAME) to use your camera to go live",
  "microphonePermission": "Allow $(PRODUCT_NAME) to use your microphone to go live",
  "capture": true,
  "pictureInPicture": true,
  "backgroundAudio": true,
  "livestream": {
    "playoutDelayMs": 1200,
    "maxPlayoutDelayMs": 2500,
    "leveller": true,
    "levellerInputGainDb": 15,
    "networkResilience": true,
    "ios": { "manageAudioSession": true },
    "android": { "audioFocus": true, "audioSource": "mic" }
  }
}]
```

| Option | Default | |
| --- | --- | --- |
| `capture` | `true` | `false` for a watch-only app: Android then declares no camera or microphone permission. iOS keeps both usage strings, which App Store review asks of any app linking WebRTC. |
| `pictureInPicture` | `false` | The Android activity attributes and the iOS `audio` background mode picture-in-picture needs. |
| `backgroundAudio` | `false` | iOS: keep playing with the app in the background. |
| `livestream.enabled` | `true` | `false` keeps libwebrtc's call audio, with echo cancellation: for an app whose audio goes both ways, such as co-hosts talking over loudspeakers. |
| `livestream.playoutDelayMs` | `0` | Play remote audio and video this far behind live, up to 10000 ms, which gives lost packets time to be resent. At the top level it applies to iOS only; see [Limitations](#limitations). |
| `livestream.maxPlayoutDelayMs` | 2500 or the delay | How far video may drift behind under heavy jitter. |
| `livestream.leveller` | `true` | A voice leveller on remote audio: input gain, a soft-knee compressor and a −1 dBFS limiter, so quiet hosts are lifted and nothing clips. |
| `livestream.levellerInputGainDb` | `15` | How far the leveller lifts a quiet speaker, 0 to 30. |
| `livestream.networkResilience` | `true` | A 1 MB socket buffer that holds a sharp keyframe. Since libwebrtc M155 the wait for a resent video packet has no switch: at most 200 ms past the jitter. A playout delay gives resends more time. |
| `livestream.ios.manageAudioSession` | `true` | Set the audio session: `.playback` / `.moviePlayback` while watching, `.playAndRecord` / `.videoRecording` while live. |
| `livestream.android.audioFocus` | `true` | Take audio focus while playing or recording, and go quiet for a phone call. |
| `livestream.android.audioSource` | `mic` | Where a host's microphone is recorded from: `mic`, `camcorder`, `voiceCommunication` (the platform's echo canceller and noise suppressor) or `unprocessed`. |

A key in `livestream.ios` or `livestream.android` wins over the same key at the top level. The options are built into the native app, so rebuild after changing them. Without Expo, set them by hand: in Info.plist, a `WebRTCLivestream` dictionary with the same keys; in the AndroidManifest, `<meta-data>` inside `<application>` named `com.fluxlabs.webrtc.livestream.<key>`. An `audioDevice` (iOS) or `audioDeviceModule` (Android) set on `WebRTCModuleOptions` takes precedence over livestream audio.

#### Watching

```tsx
import { RTCView, WHEPClient } from 'react-native-webrtc';

const player = new WHEPClient({
    url: 'https://media.example.com/live/show/whep',
    token, // sent as Authorization: Bearer
    audioOnlyFallback: true,
    onStream: stream => setStreamURL(stream.toURL()),
    onStateChange: (state, reason) => setStatus(state),
    onStats: stats => setQuality(stats.quality),
    onAudioOnlyChange: audioOnly => setAudioOnly(audioOnly),
});
player.start();
// …
player.stop();

<RTCView streamURL={streamURL} objectFit="contain" pictureInPictureEnabled />
```

It takes the stream as the server has it, whatever published it:

- **Any codec** libwebrtc decodes, H.264 from an encoder included, in hardware on both platforms.
- **Stereo** when the source is stereo; the playout path is stereo on both platforms.
- **Audio, video or both**, as the server sends them.
- **No keyframes on request**: a server cannot ask an RTMP encoder for one. Lost packets are recovered by resending, and the playout delay gives the resends time to arrive.

States:
- `connecting`, then `connected` once media flows.
- `offline` while nobody is publishing: it asks again every 2 s, jittered so viewers do not return in step, and plays when the host goes live.
- `reconnecting` when the connection fails, stays disconnected for 8 s, or no media arrives for 6 s, as when the host drops, and while there is no network. It sets up a new session, waiting longer after each failure, from 1 s to 10 s.
- `failed` only when the server refuses the credentials or the request.

The offer goes out as soon as ICE has what the server needs, rather than after every candidate, and later candidates follow by PATCH to servers that accept them.

For a server with signalling of its own, subclass `LivestreamViewer` and implement the signalling (see its doc comment, and the example's `antMedia.ts`); reconnecting, following the network, stats and quality, and the audio-only fallback come with it. For a peer connection set up entirely by hand, give it `LIVESTREAM_VIEWER_CONFIGURATION`, ask for stereo in the description you send (`setOpusParameters(sdp, { stereo: 1 })`), and sample it with `LivestreamStatsSampler`.

#### Weak and changing networks

What a player does:

- **Follows the network.** With no network it waits rather than use up retries, and the moment one is back, or the phone moves to another, it connects again instead of waiting out a backoff; a disconnect just after a network change is acted on in 2 s rather than 8. ICE keeps gathering meanwhile, so with a server that takes new candidates the stream carries on over the new network without a new session. `livestreamNetwork` tells the app the same: online or not, Wi‑Fi or cellular, metered, and Low Data Mode or Data Saver.
- **Recovers lost packets.** It asks for lost video and audio packets again (for audio, where the server offers it), and Opus conceals what does not come back. A playout delay gives resends time to arrive: libwebrtc itself waits at most 200 ms past the jitter for one.
- **Shows the frames resends complete, on Android too.** A resend completes a frame late, and the frames behind it all at once. libwebrtc's Android hardware decoder dropped such bursts, and after each frame the decoder skipped it reported the next frames' decode time a frame too long, so libwebrtc kept shrinking the jitter buffer until frames came too late to show. At 2% loss a Snapdragon phone played 4 frames a second. H.264 now decodes through `SteadyVideoDecoderFactory`, the same decoder with frames matched by timestamp and bursts queued, which plays that stream at 29.
- **Says how it is going.** `onStats` every 2 s, with a `quality` of `excellent`, `good`, `poor` or `bad`, judged on what the viewer gets: packets lost for good, freezes, missing frames, audio made up for, and the round trip, rather than bandwidth estimates, which swing by half with nothing wrong. `qualityLimitation` says whose it is: `network` (this phone's connection), `source` (the stream itself arrives thin with nothing lost on the way: the host's connection, or the server's), or `device`. For a banner that blames the right side, a QoE event, or a switch to another rendition or to HLS.
- **Adds it up for analytics.** A `LivestreamWatchReport` takes each interval (`onStats: stats => report.add(stats)`) and, as the viewer leaves, `report.summary()` gives the watch as a whole: freezes (a frame far later than those before it) and stalls (no frames for seconds, including one a reconnect cut short), the share of the time the picture stood still, the median and lowest height, median frame rate, bitrate, loss and dropped frames, the jitter buffer at p50 and p95, audio concealment and the seconds of audible gaps, the round trip at p50 and p95, whether a TURN relay carried any of it, and the seconds at each quality with what held it back longest. It reads each interval's numbers, never a connection's running totals, so one report follows the watch across new peer connections. Send it once per watch, as one event, rather than each sample.
- **Keeps the voice when the video cannot fit.** With `audioOnlyFallback: true`, once the stream has been more than the network carries for about 4 s (video packets lost even after resends, or the round trip swelling with queues), the player drops the video and keeps the audio. It tries the video again after 15 s, then 30 s and every minute, and at once when the phone moves to another network or the round trip drops (a queue gone: the network got better); a try ends at the first sign of congestion, and the video stays once it has played well for 8 s. A network that loses packets at random keeps the video: dropping it would not save the sound. `onAudioOnlyChange` tells the app, to show a still or a message. WHEP cannot change a session, so there each switch is a new session and about a second of silence; a player whose server can pause the video, as Ant Media can, switches with no gap.

What the server and the host do counts for more:

- **Adaptive bitrate** on the server, from renditions it transcodes or a host's simulcast, gives a phone on a weak connection a lower rendition rather than a broken picture. Without it every viewer gets the host's bitrate, and a phone with less bandwidth than that cannot play the video at all; audio only is then the best a player can do. On a 3 Mbps connection a 2.5 Mbps stream played at 30 fps, and a 4 Mbps one at 1 to 3.
- **A keyframe every 1 to 2 seconds** from an RTMP encoder. Nothing can ask it for one, so a viewer that loses one waits for the next. Every second rather than every two cut the time frozen after outages by a fifth to a third.

Measured with the example on a Nothing Phone (2) (Android 14) and an iPhone X (iOS 16), playing a 4 Mbps 720×1280 stream published over RTMP (H.264, and AAC the server turns into Opus), through a network emulator ([`tools/livestream-lab`](tools/livestream-lab)):

| Network | Android | iPhone (1.2 s playout delay) |
| --- | --- | --- |
| 4G: 6 Mbps, 60 ms round trip, 0.3% loss | 29.7 fps (16.5 with libwebrtc's own decoder) | 29.9 fps |
| 2% loss | 29.2 fps, frozen 2% of the time (4.6 fps with libwebrtc's own decoder) | 29.5 fps, never frozen |
| 5% loss | 28.7 fps, frozen 3.4% (1.1 fps) | 29.0 fps, frozen 1.7% |
| 20 s outage | Playing again 2 to 4 s after it ends | The same |
| Changing every few seconds: 4G, a 2 Mbps link, 3G, EDGE, a 4 s outage, Wi‑Fi, over and over, with the audio-only fallback | Audio alone while the network cannot carry the video, and the video back in each good spell. Audio made up for: 12% of the time on the 2 Mbps link, 6% on 3G, 2% on EDGE, none on 4G and Wi‑Fi | The same |

#### Going live

```tsx
import { LIVESTREAM_HOST_AUDIO_CONSTRAINTS, WHIPClient, mediaDevices } from 'react-native-webrtc';

const stream = await mediaDevices.getUserMedia({
    audio: LIVESTREAM_HOST_AUDIO_CONSTRAINTS.voice,
    video: { facingMode: 'user', width: 1280, height: 720, frameRate: 30 },
});
const publisher = new WHIPClient({ url, token, stream, video: { maxBitrateKbps: 2500 } });
publisher.start();
```

- **Microphone**: `voice` has noise suppression and gain control; `studio` has no processing, for music or a good microphone. Echo cancellation is off in both: it is for calls, and colours a voice that has nothing to cancel. The standard `echoCancellation`, `noiseSuppression` and `autoGainControl` constraints now work on both platforms; before, iOS ignored audio constraints altogether.
- **Video**: H.264, offered alone, because servers pick codecs by their own preference rather than the offer's order. Phones encode it in hardware, so an hour-long stream does not heat the phone, and RTMP and HLS outputs carry it without transcoding. `codec: null` leaves the choice to the server.
- **Bitrates**: starts at 1000 kbps rather than libwebrtc's 300, so the first seconds are sharp. Opus runs at 64 kbps without discontinuous transmission.
- **Degradation**: when bandwidth or the CPU runs short, frame rate and resolution give a little each (`degradationPreference`).
- **Orientation**: frames are rotated before encoding, so a portrait stream plays upright in any player, HLS and RTMP outputs included.
- **Simulcast**: `simulcast: true` sends full, half and quarter resolution, for servers that forward each viewer the one it can take.

For a server's own signalling, apply the same tuning in four steps: `addLivestreamTracks(pc, stream, options)`, `tuneLivestreamOffer(sdp)`, `tuneLivestreamAnswer(sdp, options)`, and `applyLivestreamSenderParameters(videoSender, options)` once connected.

#### Livestream audio at run time

```ts
import { livestreamAudio } from 'react-native-webrtc';

livestreamAudio.isInstalled;       // false when turned off, or the app set its own audio device
livestreamAudio.isPlaying;         // remote audio is playing
livestreamAudio.isRecording;       // the microphone is live
livestreamAudio.config;            // what it runs with
livestreamAudio.levellerEnabled = false;  // switch while playing, to compare by ear
livestreamAudio.takeLevels();      // iOS: the leveller's peaks since the last call
```

What it changes, against libwebrtc's call audio:

| | iOS | Android |
| --- | --- | --- |
| Playback | RemoteIO under `.playback`, instead of VoiceProcessingIO, the phone-call unit: full-band sound at media volume, and no microphone indicator while watching | `USAGE_MEDIA`, in stereo, instead of `USAGE_VOICE_COMMUNICATION`, band-limited on the call volume |
| Leveller | In software on the playout path, with metering | The platform's DynamicsProcessing effect on WebRTC's track, LoudnessEnhancer below Android 9. On Android 9 to 11 only DynamicsProcessing's first engine layout is tried, and a build that refuses it gets LoudnessEnhancer: there a refused layout's effect lingers on the session, and a second try made the app abort |
| Playout delay | Audio held back in step with the video from the first frame | Opt-in; see [Limitations](#limitations) |
| Playout buffer | | Twice the platform minimum, so a late audio thread does not crackle |
| Other apps | The session is not mixable: music in another app pauses, as for a video | Takes audio focus; mutes for a call |
| Host microphone | Captured without voice processing, mono at 48 kHz | `MIC` source, without the platform's echo canceller and noise suppressor |

#### Hosts on RTMP

A host publishing RTMP, from OBS or a phone library, plays in a `WHEPClient` like any other, given three things on the way in:

- **Opus audio.** RTMP carries AAC and WebRTC plays Opus, so the server must transcode. Ant Media, Wowza and Red5 do; mediamtx does not, and its viewers get video only. Enhanced RTMP, SRT and RTSP can carry Opus directly.
- **No B-frames.** WebRTC decodes frames in the order they arrive; B-frames stutter.
- **A keyframe at least every 2 seconds.** Nothing can ask the encoder for one, so a viewer that joins or loses a keyframe waits for the next.

#### Limitations

- **Android holds back only the video.** libwebrtc has no hook for delaying audio on Android, so with a playout delay the voice runs ahead of the picture for the first seconds of each session, until lip sync catches up at 80 ms a second. So at the top level, `playoutDelayMs` applies to iOS only. Set `livestream.android.playoutDelayMs` to accept the trade: at 500 ms, lip sync takes about 6 s, and on a network losing 5% of packets the picture freezes half as often (1.7% of the time rather than 3.4%).
- **H.264 on Android hosts needs a Qualcomm or Exynos encoder.** libwebrtc 124 encodes H.264 in hardware only on those. On other chips, MediaTek among them, the host sends VP8 in software instead. Viewers decode H.264 in hardware on every chip.
- **Two-way audio needs echo cancellation.** Livestream audio has none: with a host listening to a co-host on a loudspeaker, turn it off (`enabled: false`), or on Android use `audioSource: "voiceCommunication"`.
- **Android has no leveller metering.** DynamicsProcessing exposes none, so `takeLevels()` returns `null` there.
- **iOS only on Apple platforms.** tvOS and macOS keep libwebrtc's own audio device.

#### From react-native-webrtc-playback

| react-native-webrtc-playback | here |
| --- | --- |
| `["react-native-webrtc-playback", { … }]` | `["react-native-webrtc", { "livestream": { … } }]`, same keys |
| `getWebRtcPlayback()` | `livestreamAudio` |
| `.isInstalled`, `.isPlaying`, `.config`, `.levellerEnabled` | the same |
| `.takeLevels()` returning `undefined` | `.takeLevels()` returning `null` |
| `RECOMMENDED_RTC_CONFIGURATION` | `LIVESTREAM_VIEWER_CONFIGURATION`, which `WHEPClient` applies itself |

Remove the package and its plugin: with both installed, the one that sets the audio device first wins. The Info.plist key is now `WebRTCLivestream`, and the meta-data prefix `com.fluxlabs.webrtc.livestream.`.

### New Architecture

This fork runs on the New Architecture only, React Native 0.76 or later: `WebRTCModule` is a TurboModule with typed events, and `RTCView` and `ScreenCapturePickerView` are Fabric components. Codegen builds them from the specs in `src/` (`NativeWebRTCModule.ts`, `RTCVideoViewNativeComponent.ts`, `ScreenCapturePickerViewNativeComponent.ts`), as `codegenConfig` in `package.json` describes. An app on the old architecture gets an error at startup; 124.1.0 is the last version for it.

- The JS API is the same, except `ScreenCapturePickerView`: it is a component with a `show()` method. Replace `NativeModules.ScreenCapturePickerViewManager.show(findNodeHandle(ref.current))` with `ref.current?.show()`.
- A new native method goes in `src/NativeWebRTCModule.ts`, then on both platforms with the signature codegen generates. On iOS, debug builds log any spec method the module lacks or declares with other types.
- A new event goes in the spec too, then in `WebRTCModule.h` and `WebRTCModuleEvents()` on iOS and in `WebRTCModule.emitEvent()` on Android.

### Release builds

An Android release build shrunk by R8 needs no rules of its own: the library's consumer rules (`android/consumer-rules.pro`) keep what libwebrtc's native code reaches by name.

- `org.webrtc.**`, in full.
- From M155, the methods and fields that jni_zero, libwebrtc's JNI layer, marks `@CalledByNative`, `@CalledByNativeUnchecked` or `@AccessedByNative`. Loading the library calls one of them, `org.jni_zero.JniZero.init`.
- `J.N`, where M155 declares the native methods its Java classes call.
- `-dontwarn org.webrtc.Dav1dDecoder$Natives`: the M155 build refers to that class but does not ship it.

Before 155.1.1 the rules kept `org.webrtc.**` only. R8 then failed on `Dav1dDecoder$Natives`, and an app whose own rules silenced that crashed as WebRTC loaded, in `JNI_OnLoad`, with `JNI DETECTED ERROR IN APPLICATION: java_class == null in call to GetStaticMethodID`. Upstream 155.0.0 has the same rules. Debug builds are not shrunk, so they never show it.

### Performance

A benchmark app calling this library, Release builds, median of 3 runs.

- **Old arch**: the library over the bridge, in a React Native 0.81 app with the New Architecture off.
- **New arch (interop)**: the library before this fork's conversion, in a React Native 0.87 app, running through React Native's interop layer for legacy modules and views.
- **New arch (bridgeless)**: this fork, TurboModule and Fabric components, in the same React Native 0.87 app.

Both New Architecture columns run bridgeless, as React Native 0.87 always does; they differ in whether the library goes through the interop layer.

Android, arm64 emulator (API 35):

| | Old arch | New arch (interop) | New arch (bridgeless) |
|---|---|---|---|
| Native sync call | 0.76 µs | 0.16 µs | 0.14 µs |
| Async call | 290 µs | 195 µs | 194 µs |
| Call setup | 23.3 ms | 19.2 ms | 18.6 ms |
| `getStats()` | 0.57 ms | 0.53 ms | 0.59 ms |
| 1,000 binary messages | 702 ms | 407 ms | 427 ms |
| 1,000 text messages | 138 ms | 151 ms | 161 ms |

iOS, iPhone 17 simulator (iOS 26.5):

| | New arch (interop) | New arch (bridgeless) |
|---|---|---|
| Native sync call | 2.35 µs | 1.94 µs |
| Async call | 14.8 µs | 14.0 µs |
| Call setup | 3.0 ms | 3.0 ms |
| `getStats()` | 0.33 ms | 0.30 ms |
| 1,000 binary messages | 473 ms | 474 ms |
| 1,000 text messages | 60 ms | 54 ms |

- **Native sync call**: one synchronous method call. **Async call**: one promise round trip.
- **Call setup**: offer, answer and ICE between two peer connections in the app, until a data channel opens.
- **`getStats()`**: on a connected call; with audio and video on Android, data channel only on iOS, whose simulator has no camera.
- **Messages**: 4 KB each over a data channel, at most 64 in flight. Binary messages cost more because they cross to native as base64.

Moving the app from the old architecture to the new one is the gain: synchronous calls about 5× faster, async calls and call setup 20–35% faster, binary messages 40% faster. The conversion keeps that speed without the interop layer. These are emulator and simulator numbers, not devices; the old architecture was measured on Android only.

---

[<img src="https://avatars.githubusercontent.com/u/42463376" alt="React Native WebRTC" style="height: 6em;" />](https://github.com/react-native-webrtc/react-native-webrtc)

# React-Native-WebRTC

[![npm version](https://img.shields.io/npm/v/react-native-webrtc)](https://www.npmjs.com/package/react-native-webrtc)
[![npm downloads](https://img.shields.io/npm/dm/react-native-webrtc)](https://www.npmjs.com/package/react-native-webrtc)
[![Discourse topics](https://img.shields.io/discourse/topics?server=https%3A%2F%2Freact-native-webrtc.discourse.group%2F)](https://react-native-webrtc.discourse.group/)

A WebRTC module for React Native.

## Feature Overview

|  | Android | iOS | tvOS | macOS* | Windows* | Web* | Expo* |
| :- | :-: | :-: | :-: | :-: | :-: | :-: | :-: |
| Audio/Video | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | - | - | :heavy_check_mark: | :heavy_check_mark: |
| Data Channels | :heavy_check_mark: | :heavy_check_mark: | - | - | - | :heavy_check_mark: | :heavy_check_mark: |
| Screen Capture | :heavy_check_mark: | :heavy_check_mark: | - | - | - | :heavy_check_mark: | :heavy_check_mark: |
| Plan B | - | - | - | - | - | - | - |
| Unified Plan* | :heavy_check_mark: | :heavy_check_mark: | - | - | - | :heavy_check_mark: | :heavy_check_mark: |
| Simulcast* | :heavy_check_mark: | :heavy_check_mark: | - | - | - | :heavy_check_mark: | :heavy_check_mark: |

> **macOS** - We don't currently actively support macOS at this time.  
Support might return in the future.

> **Windows** - We don't currently support the [react-native-windows](https://github.com/microsoft/react-native-windows) platform at this time.  
Anyone interested in getting the ball rolling? We're open to contributions.

> **Web** - The [react-native-webrtc-web-shim](https://github.com/react-native-webrtc/react-native-webrtc-web-shim) project provides a shim for [react-native-web](https://github.com/necolas/react-native-web) support.  
Which will allow you to use [(almost)](https://github.com/react-native-webrtc/react-native-webrtc-web-shim/tree/main#setup) the exact same code in your [react-native-web](https://github.com/necolas/react-native-web) project as you would with [react-native](https://reactnative.dev/) directly.  

> **Expo** - As this module includes native code it is not available in the [Expo Go](https://expo.dev/client) app by default.  
However you can get things working via the [expo-dev-client](https://docs.expo.dev/development/getting-started/) library and out-of-tree [config-plugins/react-native-webrtc](https://github.com/expo/config-plugins/tree/master/packages/react-native-webrtc) package.  

> **Unified Plan** - As of version 106.0.0 Unified Plan is the only supported mode.  
Those still in need of Plan B will need to use an older release.

> **Simulcast** - As of version 111.0.0 Simulcast is now possible with ease.  
Software encode/decode factories have been enabled by default.

## WebRTC Revision

* Currently used revision: [M155](https://github.com/jitsi/webrtc/tree/M155)
* Supported architectures
  * Android: armeabi-v7a, arm64-v8a, x86, x86_64
  * iOS: arm64, x86_64
  * tvOS: arm64
  * macOS: arm64, x86_64

## Getting Started

Use one of the following preferred package install methods to immediately get going.  
Don't forget to follow platform guides below to cover any extra required steps.  

**npm:** `npm install react-native-webrtc --save`  
**yarn:** `yarn add react-native-webrtc`  
**pnpm:** `pnpm install react-native-webrtc`  

## Guides

- [Android Install](./Documentation/AndroidInstallation.md)
- [iOS Install](./Documentation/iOSInstallation.md)
- [tvOS Install](./Documentation/tvOSInstallation.md)
- [Basic Usage](./Documentation/BasicUsage.md)
- [Step by Step Call Guide](./Documentation/CallGuide.md)
- [Improving Call Reliability](./Documentation/ImprovingCallReliability.md)
- [Migrating to Unified Plan](https://docs.google.com/document/d/1-ZfikoUtoJa9k-GZG1daN0BU3IjIanQ_JSscHxQesvU/edit#heading=h.wuu7dx8tnifl)

## Example Projects

We have some very basic example projects included in the [examples](./examples) directory.  
Don't worry, there are plans to include a much more broader example with backend included.  

## Community

Come join our [Discourse Community](https://react-native-webrtc.discourse.group/) if you want to discuss any React Native and WebRTC related topics.  
Everyone is welcome and every little helps.  

## Related Projects

Looking for extra functionality coverage?  
The [react-native-webrtc](https://github.com/react-native-webrtc) organization provides a number of packages which are more than useful when developing Real Time Communication applications.  
