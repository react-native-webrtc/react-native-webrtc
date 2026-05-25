#import <Foundation/Foundation.h>

#if TARGET_OS_IOS
#import <ReplayKit/ReplayKit.h>
#endif

NS_ASSUME_NONNULL_BEGIN

/**
 * Programmatically presents the iOS system `RPSystemBroadcastPickerView`.
 *
 * When no broadcast is active, this opens the extension picker (Start
 * Broadcast). When a broadcast is active, it opens the system "Stop
 * Broadcast" sheet — letting the user end the broadcast via
 * `broadcastFinished()` instead of the host-initiated socket close that
 * forces the extension to call `finishBroadcastWithError(_:)` and
 * surface an error dialog.
 *
 * Uses the public-API technique of locating the picker's inner
 * `UIButton` and dispatching `UIControlEventTouchUpInside`. Must be
 * invoked on the main thread.
 *
 * iOS 12+ only.
 */
#if TARGET_OS_IOS
API_AVAILABLE(ios(12))
@interface BroadcastPickerHelper : NSObject

/**
 * Creates an ad-hoc `RPSystemBroadcastPickerView`, configured with the
 * extension bundle id read from the host app's Info.plist key
 * `RTCScreenSharingExtension`, then taps it. Returns NO and populates
 * `error` if the picker's button subview cannot be located.
 */
+ (BOOL)presentSystemPickerWithError:(NSError *_Nullable *_Nullable)error;

/**
 * Taps an existing mounted `RPSystemBroadcastPickerView` (e.g. one
 * placed in the React Native view hierarchy). Returns NO and populates
 * `error` if the button subview cannot be located.
 */
+ (BOOL)tapPickerView:(RPSystemBroadcastPickerView *)view error:(NSError *_Nullable *_Nullable)error;

@end
#endif

NS_ASSUME_NONNULL_END
