import { NativeModules, Platform } from 'react-native';

const { WebRTCModule } = NativeModules;

/**
 * Presents the iOS system `RPSystemBroadcastPickerView` programmatically.
 *
 * When no broadcast is active, this opens the extension picker (Start
 * Broadcast). When a broadcast is active, it opens the system "Stop
 * Broadcast" sheet — letting the user end the broadcast via
 * `broadcastFinished()` instead of the host-initiated socket close that
 * forces the extension to call `finishBroadcastWithError(_:)` and
 * surface an error dialog.
 *
 * iOS only. Resolves once the tap is dispatched, NOT once the user
 * confirms — callers should observe their screen-share track's `"ended"`
 * event to know when the broadcast actually stopped.
 *
 * No-op on non-iOS platforms.
 */
export default function presentBroadcastPicker(): Promise<void> {
    if (Platform.OS !== 'ios') {
        return Promise.resolve();
    }
    return WebRTCModule.presentBroadcastPicker();
}
