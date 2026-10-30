/**
 * Checks the Daily-specific features of @daily-co/react-native-webrtc (see
 * scripts/daily-patches/manifest.json): the APIs react-native-daily-js and
 * daily-js depend on, exercised without joining a call.
 */
import {
  DeviceEventEmitter,
  NativeModules,
  Platform,
  Settings,
} from 'react-native';
import {
  mediaDevices,
  MediaStream,
  MediaStreamTrack,
  registerGlobals,
} from '@daily-co/react-native-webrtc';

// react-native-daily-js calls this on import; daily-js then uses the globals.
registerGlobals();

export type Status = 'pass' | 'fail' | 'skip';

export interface Result {
  name: string;
  status: Status;
  detail?: string;
}

const { WebRTCModule } = NativeModules;
const isIOS = Platform.OS === 'ios';

class Skip extends Error {}

const skip = (reason: string): never => {
  throw new Skip(reason);
};

const assert = (condition: unknown, message: string) => {
  if (!condition) {
    throw new Error(message);
  }
};

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

const withTimeout = <T>(promise: Promise<T>, ms: number, what: string) =>
  Promise.race([
    promise,
    new Promise<T>((_, reject) =>
      setTimeout(() => reject(new Error(`${what} timed out after ${ms}ms`)), ms)
    ),
  ]);

// Polls until `read` returns `expected` (audio routes change asynchronously).
const eventually = async (
  read: () => Promise<string>,
  expected: string,
  ms = 3000
) => {
  let value = await read();
  for (let waited = 0; value !== expected && waited < ms; waited += 250) {
    await sleep(250);
    value = await read();
  }
  return value;
};

type Device = { deviceId: string; kind: string; label: string };

interface Context {
  devices: Device[];
  hasCamera: boolean;
}

type Check = {
  name: string;
  run: (ctx: Context) => Promise<string | void>;
};

const NATIVE_METHODS = [
  'enumerateDevices',
  'setDailyAudioMode',
  'setAudioDevice',
  'getAudioDevice',
  'startMediaDevicesEventMonitor',
  'mediaStreamTrackSwitchCamera',
  'mediaStreamTrackGetCameraFacingMode',
  ...(isIOS ? ['enableNoOpRecordingEnsuringBackgroundContinuity'] : []),
];

const withCamera = async (
  ctx: Context,
  test: (track: any) => Promise<string | void>
) => {
  if (!ctx.hasCamera) {
    skip('no camera (simulator?)');
  }
  const stream: MediaStream = await mediaDevices.getUserMedia({
    video: { facingMode: 'user' },
    audio: false,
  });
  const track: any = stream.getVideoTracks()[0];
  try {
    assert(track, 'getUserMedia returned no video track');
    return await test(track);
  } finally {
    stream.getTracks().forEach((t) => t.stop());
    stream.release();
  }
};

const checks: Check[] = [
  {
    // react-native-daily-js binds these methods directly (DailyNativeUtils).
    name: 'native: fork methods exist on WebRTCModule',
    run: async () => {
      const missing = NATIVE_METHODS.filter(
        (m) => typeof WebRTCModule?.[m] !== 'function'
      );
      assert(missing.length === 0, `missing: ${missing.join(', ')}`);
      return `${NATIVE_METHODS.length} methods`;
    },
  },
  {
    // daily-js calls these on the local video track.
    name: 'js: MediaStreamTrack._switchCamera/_getCameraFacingMode exist',
    run: async () => {
      const proto: any = MediaStreamTrack.prototype;
      assert(typeof proto._switchCamera === 'function', '_switchCamera missing');
      assert(
        typeof proto._getCameraFacingMode === 'function',
        '_getCameraFacingMode missing'
      );
    },
  },
  {
    // Native 'mediaDevicesOnDeviceChange' must reach navigator.mediaDevices
    // 'devicechange' listeners (that's what daily-js subscribes to).
    name: 'js: mediaDevicesOnDeviceChange reaches navigator devicechange listeners',
    run: async () => {
      const nav: any = (globalThis as any).navigator?.mediaDevices;
      assert(
        typeof nav?.addEventListener === 'function',
        'navigator.mediaDevices.addEventListener missing (registerGlobals?)'
      );
      let fired = 0;
      const listener = () => fired++;
      nav.addEventListener('devicechange', listener);
      try {
        DeviceEventEmitter.emit('mediaDevicesOnDeviceChange', {});
        await sleep(100);
      } finally {
        nav.removeEventListener('devicechange', listener);
      }
      assert(fired > 0, 'devicechange listener was not called');
    },
  },
  {
    // Daily lists audio outputs with kind 'audio' from enumerateDevices.
    name: 'enumerateDevices lists audio outputs',
    run: async (ctx) => {
      const ids = ctx.devices
        .filter((d) => d.kind === 'audio')
        .map((d) => d.deviceId);
      assert(ids.includes('SPEAKERPHONE'), `no SPEAKERPHONE in [${ids}]`);
      assert(
        ids.includes('WIRED_OR_EARPIECE'),
        `no WIRED_OR_EARPIECE in [${ids}]`
      );
      const cameras = ctx.devices.filter((d) => d.kind === 'videoinput').length;
      return `audio: ${ids.join(', ')}; cameras: ${cameras}`;
    },
  },
  {
    name: 'setDailyAudioMode video/voice/idle',
    run: async () => {
      for (const mode of ['video', 'voice', 'idle']) {
        WebRTCModule.setDailyAudioMode(mode);
        await sleep(200);
      }
    },
  },
  {
    name: 'setAudioDevice + getAudioDevice round trip',
    run: async (ctx) => {
      if (isIOS && !ctx.hasCamera) {
        skip('iOS simulator has no audio route control');
      }
      // Routes only apply in a call: Daily enters an in-call mode first, which on
      // Android switches to MODE_IN_COMMUNICATION (setSpeakerphoneOn is ignored
      // otherwise).
      WebRTCModule.setDailyAudioMode('video');
      await sleep(500);
      const routes: string[] = [];
      for (const id of ['SPEAKERPHONE', 'WIRED_OR_EARPIECE']) {
        WebRTCModule.setAudioDevice(id);
        const current = await eventually(() => WebRTCModule.getAudioDevice(), id);
        assert(current === id, `set ${id}, got ${current}`);
        routes.push(current);
      }
      WebRTCModule.setDailyAudioMode('idle');
      return routes.join(' -> ');
    },
  },
  {
    name: 'camera: _switchCamera toggles facing mode',
    run: (ctx) =>
      withCamera(ctx, async (track) => {
        const initial = await track._getCameraFacingMode();
        assert(initial === 'user', `initial facing mode ${initial}`);
        const switched = await track._switchCamera();
        assert(switched === 'environment', `_switchCamera resolved ${switched}`);
        const reported = await track._getCameraFacingMode();
        assert(reported === 'environment', `then reported ${reported}`);
        const back = await track._switchCamera();
        assert(back === 'user', `switching back resolved ${back}`);
        return 'user -> environment -> user';
      }),
  },
  {
    // Muting (track.enabled = false) stops the capturer; Daily still lets the
    // user flip the camera, and the choice must survive unmuting.
    name: 'camera: _switchCamera while muted is kept after unmute',
    run: (ctx) =>
      withCamera(ctx, async (track) => {
        track.enabled = false;
        await sleep(500);
        const switched = await track._switchCamera();
        assert(switched === 'environment', `muted switch resolved ${switched}`);
        track.enabled = true;
        await sleep(1500);
        const reported = await track._getCameraFacingMode();
        assert(reported === 'environment', `after unmute reported ${reported}`);
      }),
  },
  {
    name: 'iOS: enableNoOpRecordingEnsuringBackgroundContinuity',
    run: async () => {
      if (!isIOS) {
        skip('iOS only');
      }
      WebRTCModule.enableNoOpRecordingEnsuringBackgroundContinuity(true);
      await sleep(500);
      WebRTCModule.enableNoOpRecordingEnsuringBackgroundContinuity(false);
    },
  },
  {
    // Last on purpose: Android shows a system dialog that scripts/run-selftest.sh accepts.
    name: 'Android: getDisplayMedia starts screen capture',
    run: async () => {
      if (isIOS) {
        skip('iOS needs the broadcast extension picker');
      }
      const stream: MediaStream = await withTimeout(
        mediaDevices.getDisplayMedia(),
        60000,
        'getDisplayMedia'
      );
      try {
        const track = stream.getVideoTracks()[0];
        assert(track, 'no video track');
        assert(track.readyState === 'live', `track is ${track.readyState}`);
      } finally {
        stream.getTracks().forEach((t) => t.stop());
        stream.release();
      }
    },
  },
];

export const summarize = (results: Result[]) => {
  const count = (s: Status) => results.filter((r) => r.status === s).length;
  return `SELFTEST DONE failed=${count('fail')} passed=${count(
    'pass'
  )} skipped=${count('skip')}`;
};

// Logs one "[SELFTEST] ..." line per check plus the summary (read from logcat on
// Android). iOS release builds don't log JS, so the lines are also saved in
// NSUserDefaults, where scripts/run-selftest.sh reads them.
const report = (results: Result[]) => {
  const lines = results.map(
    (r) =>
      `${r.status.toUpperCase()} ${r.name}` + (r.detail ? ` (${r.detail})` : '')
  );
  lines.push(summarize(results));
  lines.forEach((line) => console.log(`[SELFTEST] ${line}`));
  if (isIOS) {
    Settings.set({ selftest: lines.join('\n') });
  }
};

export async function runSelfTest(
  onProgress: (results: Result[], running?: string) => void
): Promise<Result[]> {
  const results: Result[] = [];
  const devices: Device[] = (await mediaDevices.enumerateDevices()) as any;
  const ctx: Context = {
    devices,
    hasCamera: devices.some((d) => d.kind === 'videoinput'),
  };
  for (const check of checks) {
    onProgress([...results], check.name);
    let result: Result;
    try {
      const detail = await check.run(ctx);
      result = { name: check.name, status: 'pass', detail: detail || undefined };
    } catch (e: any) {
      result =
        e instanceof Skip
          ? { name: check.name, status: 'skip', detail: e.message }
          : { name: check.name, status: 'fail', detail: String(e?.message ?? e) };
    }
    results.push(result);
  }
  onProgress([...results]);
  report(results);
  return results;
}
