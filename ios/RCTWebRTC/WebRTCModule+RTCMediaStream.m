#import <objc/runtime.h>

#import <WebRTC/RTCCameraVideoCapturer.h>
#import <WebRTC/RTCMediaConstraints.h>
#import <WebRTC/RTCMediaStreamTrack.h>
#import <WebRTC/RTCVideoTrack.h>

#import "RTCMediaStreamTrack+React.h"
#import "WebRTCModule+RTCMediaStream.h"
#import "WebRTCModule+RTCPeerConnection.h"
#import "WebRTCModuleOptions.h"

#import <React/RCTUIManager.h>
#import "CustomVideoCaptureController.h"
#import "ProcessorProvider.h"
#import "ScreenCaptureController.h"
#import "ScreenCapturer.h"
#import "TrackCapturerEventsEmitter.h"
#import "VideoCaptureController.h"

#if TARGET_OS_IOS

#import <React/RCTLog.h>
#import <ReplayKit/ReplayKit.h>
#import "BroadcastPickerHelper.h"

#endif

@implementation WebRTCModule (RTCMediaStream)

- (VideoEffectProcessor *)videoEffectProcessor {
    return objc_getAssociatedObject(self, _cmd);
}

- (void)setVideoEffectProcessor:(VideoEffectProcessor *)videoEffectProcessor {
    objc_setAssociatedObject(
        self, @selector(videoEffectProcessor), videoEffectProcessor, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

#pragma mark - getUserMedia

/**
 * Initializes a new {@link RTCAudioTrack} which satisfies the given constraints.
 *
 * @param constraints The {@code MediaStreamConstraints} which the new
 * {@code RTCAudioTrack} instance is to satisfy.
 */
- (RTCAudioTrack *)createAudioTrack:(NSDictionary *)constraints {
    NSString *trackId = [[NSUUID UUID] UUIDString];
    RTCAudioTrack *audioTrack = [self.peerConnectionFactory audioTrackWithTrackId:trackId];
    return audioTrack;
}
/**
 * Initializes a new {@link RTCVideoTrack} with the given capture controller
 */
- (RTCVideoTrack *)createVideoTrackWithCaptureController:
    (CaptureController * (^)(RTCVideoSource *))captureControllerCreator {
#if TARGET_OS_TV
    return nil;
#else

    RTCVideoSource *videoSource = [self.peerConnectionFactory videoSource];

    NSString *trackUUID = [[NSUUID UUID] UUIDString];
    RTCVideoTrack *videoTrack = [self.peerConnectionFactory videoTrackWithSource:videoSource trackId:trackUUID];

    CaptureController *captureController = captureControllerCreator(videoSource);
    videoTrack.captureController = captureController;
    [captureController startCapture];

    return videoTrack;
#endif
}
/**
 * Initializes a new {@link RTCMediaTrack} with the given tracks.
 *
 * @return An array with the mediaStreamId in index 0, and track infos in index 1.
 */
- (NSArray *)createMediaStream:(NSArray<RTCMediaStreamTrack *> *)tracks {
#if TARGET_OS_TV
    return nil;
#else
    NSString *mediaStreamId = [[NSUUID UUID] UUIDString];
    RTCMediaStream *mediaStream = [self.peerConnectionFactory mediaStreamWithStreamId:mediaStreamId];
    NSMutableArray<NSDictionary *> *trackInfos = [NSMutableArray array];

    for (RTCMediaStreamTrack *track in tracks) {
        if ([track.kind isEqualToString:@"audio"]) {
            [mediaStream addAudioTrack:(RTCAudioTrack *)track];
        } else if ([track.kind isEqualToString:@"video"]) {
            [mediaStream addVideoTrack:(RTCVideoTrack *)track];
        }

        NSString *trackId = track.trackId;

        self.localTracks[trackId] = track;

        NSDictionary *settings = @{};
        if ([track.kind isEqualToString:@"video"]) {
            RTCVideoTrack *videoTrack = (RTCVideoTrack *)track;
            if ([videoTrack.captureController isKindOfClass:[CaptureController class]]) {
                settings = [videoTrack.captureController getSettings];
            }
        } else if ([track.kind isEqualToString:@"audio"]) {
            settings = @{
                @"deviceId" : @"audio",
                @"groupId" : @"",
            };
        }

        [trackInfos addObject:@{
            @"enabled" : @(track.isEnabled),
            @"id" : trackId,
            @"kind" : track.kind,
            @"readyState" : @"live",
            @"remote" : @(NO),
            @"settings" : settings
        }];
    }

    self.localStreams[mediaStreamId] = mediaStream;
    return @[ mediaStreamId, trackInfos ];
#endif
}

/**
 * Initializes a new {@link RTCVideoTrack} which satisfies the given constraints.
 */
- (RTCVideoTrack *)createVideoTrack:(NSDictionary *)constraints {
#if TARGET_OS_TV
    return nil;
#else
    RTCVideoSource *videoSource = [self.peerConnectionFactory videoSource];

    NSString *trackUUID = [[NSUUID UUID] UUIDString];
    RTCVideoTrack *videoTrack = [self.peerConnectionFactory videoTrackWithSource:videoSource trackId:trackUUID];

    BOOL hasRuntimeVideoDevice = YES;
#if TARGET_IPHONE_SIMULATOR
    // On simulator, a runtime-provided video source may exist (e.g. virtual camera),
    // so only skip camera capture setup when no runtime video device is available.
    hasRuntimeVideoDevice = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo] != nil;
#endif

    if (hasRuntimeVideoDevice) {
        RTCCameraVideoCapturer *videoCapturer = [[RTCCameraVideoCapturer alloc] initWithDelegate:videoSource];
        VideoCaptureController *videoCaptureController =
            [[VideoCaptureController alloc] initWithCapturer:videoCapturer andConstraints:constraints[@"video"]];
        videoCaptureController.enableMultitaskingCameraAccess =
            [WebRTCModuleOptions sharedInstance].enableMultitaskingCameraAccess;
        videoTrack.captureController = videoCaptureController;
        [videoCaptureController startCapture];
    }

    return videoTrack;
#endif
}

RCT_EXPORT_METHOD(getDisplayMedia
                  : (NSDictionary *)constraints resolver
                  : (RCTPromiseResolveBlock)resolve rejecter
                  : (RCTPromiseRejectBlock)reject) {
#if TARGET_OS_TV
    reject(@"unsupported_platform", @"tvOS is not supported", nil);
    return;
#else

#if TARGET_IPHONE_SIMULATOR || TARGET_OS_OSX || TARGET_OS_TV
    reject(@"DOMException", @"AbortError", nil);
    return;
#endif

    RTCVideoSource *videoSource = [self.peerConnectionFactory videoSourceForScreenCast:YES];

    NSString *trackUUID = [[NSUUID UUID] UUIDString];
    RTCVideoTrack *videoTrack = [self.peerConnectionFactory videoTrackWithSource:videoSource trackId:trackUUID];

    ScreenCapturer *screenCapturer = [[ScreenCapturer alloc] initWithDelegate:videoSource];
    ScreenCaptureController *screenCaptureController =
        [[ScreenCaptureController alloc] initWithCapturer:screenCapturer];

    TrackCapturerEventsEmitter *emitter = [[TrackCapturerEventsEmitter alloc] initWith:trackUUID webRTCModule:self];
    screenCaptureController.eventsDelegate = emitter;
    videoTrack.captureController = screenCaptureController;
    [screenCaptureController startCapture];

    if (@available(iOS 12, *)) {
        [self.bridge.uiManager
            addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
                NSError *pickerError = nil;
                if (![BroadcastPickerHelper presentSystemPickerWithError:&pickerError]) {
                    RCTLogError(@"Failed to present broadcast picker: %@", pickerError.localizedDescription);
                }
            }];
    } else {
        RCTLogError(@"showPicker requires iOS 12 or later");
        return;
    }

    __weak __typeof__(self) weakSelf = self;
    screenCaptureController.onCaptureReady = ^{
        [weakSelf.bridge.uiManager
            addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
                NSString *mediaStreamId = [[NSUUID UUID] UUIDString];
                RTCMediaStream *mediaStream = [weakSelf.peerConnectionFactory mediaStreamWithStreamId:mediaStreamId];
                [mediaStream addVideoTrack:videoTrack];

                NSString *trackId = videoTrack.trackId;
                weakSelf.localTracks[trackId] = videoTrack;

                NSDictionary *trackInfo = @{
                    @"enabled" : @(videoTrack.isEnabled),
                    @"id" : videoTrack.trackId,
                    @"kind" : videoTrack.kind,
                    @"readyState" : @"live",
                    @"remote" : @(NO)
                };

                weakSelf.localStreams[mediaStreamId] = mediaStream;
                resolve(@{@"streamId" : mediaStreamId, @"track" : trackInfo});
            }];
    };
#endif
}

#pragma mark - Custom video track (app-rendered, IOSurface-backed frames)

/**
 * Creates a custom video track whose frames are supplied by the app. Mirrors
 * getDisplayMedia in that it resolves a {streamId, track} pair. Two modes,
 * selected by the presence of a poolId:
 *
 *   * Pooled — init[@"poolId"] names a CustomVideoBufferPool previously allocated
 *     via createCustomVideoBufferPool. The app renders into that pool's IOSurfaces
 *     and pushes frames by index (per frame, via the JSI channel installed by
 *     installCustomVideoJSI). A pool binds to exactly one track.
 *   * Forwarding — no poolId. The app forwards finished native CVPixelBufferRefs.
 *
 * @param init dictionary with an optional {poolId}.
 */
RCT_EXPORT_METHOD(createCustomVideoTrack
                  : (NSDictionary *)init resolver
                  : (RCTPromiseResolveBlock)resolve rejecter
                  : (RCTPromiseRejectBlock)reject) {
#if TARGET_OS_TV || TARGET_OS_OSX
    reject(@"E_UNSUPPORTED_PLATFORM", @"createCustomVideoTrack is only supported on iOS and Android.", nil);
    return;
#else
    NSString *poolId = init[@"poolId"];

    RTCVideoSource *videoSource = [self.peerConnectionFactory videoSource];

    NSString *trackUUID = [[NSUUID UUID] UUIDString];
    RTCVideoTrack *videoTrack = [self.peerConnectionFactory videoTrackWithSource:videoSource trackId:trackUUID];

    CustomVideoCaptureController *captureController = nil;
    if (poolId != nil && poolId.length > 0) {
        // Pooled: bind to the app-allocated buffer pool (1<->1 with a track).
        CustomVideoBufferPool *pool = [self registeredCustomVideoBufferPoolForPoolId:poolId];
        if (pool == nil) {
            reject(
                @"E_CUSTOM_VIDEO_TRACK_FAILED", @"No custom video buffer pool registered for the given poolId.", nil);
            return;
        }
        if (pool.attached) {
            reject(
                @"E_CUSTOM_VIDEO_POOL_IN_USE", @"This custom video buffer pool is already attached to a track.", nil);
            return;
        }
        pool.attached = YES;
        captureController = [[CustomVideoCaptureController alloc] initPooledWithVideoSource:videoSource pool:pool];
        // Let releaseCustomVideoBufferPool refuse disposal while this track is
        // still live (its in-flight deliveries reference the pool's buffers).
        pool.attachedController = captureController;
    } else {
        // Forwarding: no pool; frames arrive as finished native buffers.
        captureController = [[CustomVideoCaptureController alloc] initForwardingWithVideoSource:videoSource];
    }

    videoTrack.captureController = captureController;
    // Register before startCapture so the per-frame deliver callback can resolve
    // the controller via the lock-guarded registry instead of racing on
    // self.localTracks from the JS thread. The registry holds it weakly, so it
    // clears itself when the track/controller is released.
    [self registerCustomVideoController:captureController forTrackId:trackUUID];
    [captureController startCapture];

    NSString *mediaStreamId = [[NSUUID UUID] UUIDString];
    RTCMediaStream *mediaStream = [self.peerConnectionFactory mediaStreamWithStreamId:mediaStreamId];
    [mediaStream addVideoTrack:videoTrack];

    self.localTracks[trackUUID] = videoTrack;
    self.localStreams[mediaStreamId] = mediaStream;

    resolve(@{
        @"streamId" : mediaStreamId,
        @"track" : @{
            @"id" : trackUUID,
            @"kind" : videoTrack.kind,
            @"readyState" : @"live",
            @"remote" : @(NO),
            @"enabled" : @(videoTrack.isEnabled),
            // Same shape as the getUserMedia path, so track.getSettings() reports
            // the real dimensions (pool size for pooled tracks; 0x0 for forwarding
            // tracks, whose buffers carry their own size per frame).
            @"settings" : [captureController getSettings],
        },
    });
#endif
}

/**
 * Presents the iOS system `RPSystemBroadcastPickerView` programmatically.
 * When no broadcast is active, this opens the extension picker. When a
 * broadcast is active, it opens the system "Stop Broadcast" sheet — letting
 * the user end the broadcast via `broadcastFinished()` instead of the
 * host-initiated socket close that forces the extension to call
 * `finishBroadcastWithError(_:)` and surface an error dialog.
 */
RCT_EXPORT_METHOD(presentBroadcastPicker : (RCTPromiseResolveBlock)resolve rejecter : (RCTPromiseRejectBlock)reject) {
#if TARGET_OS_TV || TARGET_OS_OSX
    reject(@"unsupported_platform", @"presentBroadcastPicker is not supported on this platform", nil);
    return;
#else

#if TARGET_IPHONE_SIMULATOR
    reject(@"unsupported_platform", @"presentBroadcastPicker is not supported on the simulator", nil);
    return;
#endif

    if (@available(iOS 12, *)) {
        [self.bridge.uiManager
            addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
                NSError *pickerError = nil;
                if ([BroadcastPickerHelper presentSystemPickerWithError:&pickerError]) {
                    resolve(nil);
                } else {
                    reject(@"picker_button_not_found", pickerError.localizedDescription, pickerError);
                }
            }];
    } else {
        reject(@"unsupported_version", @"presentBroadcastPicker requires iOS 12 or later", nil);
    }
#endif
}

/**
 * Presents the system broadcast picker for the standalone livestream extension
 * (reads the Info.plist key `RTCLivestreamExtension`). The livestream extension owns the
 * whole WebRTC pipeline in-process so the stream survives the app being backgrounded.
 */
RCT_EXPORT_METHOD(presentLivestreamBroadcastPicker
                  : (RCTPromiseResolveBlock)resolve rejecter
                  : (RCTPromiseRejectBlock)reject) {
#if TARGET_OS_TV || TARGET_OS_OSX
    reject(@"unsupported_platform", @"presentLivestreamBroadcastPicker is not supported on this platform", nil);
    return;
#else

#if TARGET_IPHONE_SIMULATOR
    reject(@"unsupported_platform", @"presentLivestreamBroadcastPicker is not supported on the simulator", nil);
    return;
#endif

    if (@available(iOS 12, *)) {
        [self.bridge.uiManager
            addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
                NSError *pickerError = nil;
                if ([BroadcastPickerHelper presentLivestreamSystemPickerWithError:&pickerError]) {
                    resolve(nil);
                } else {
                    reject(@"picker_button_not_found", pickerError.localizedDescription, pickerError);
                }
            }];
    } else {
        reject(@"unsupported_version", @"presentLivestreamBroadcastPicker requires iOS 12 or later", nil);
    }
#endif
}

/**
 * Writes the WHIP credentials ({@code whipUrl}, {@code token}) into the shared App Group
 * UserDefaults so the livestream broadcast extension can read them on broadcastStarted.
 * The App Group id is resolved from the host app's Info.plist key `RTCAppGroupIdentifier`,
 * the same key the in-call extension uses to locate the shared container.
 */
RCT_EXPORT_METHOD(writeLivestreamCredentials
                  : (NSDictionary *)credentials resolver
                  : (RCTPromiseResolveBlock)resolve rejecter
                  : (RCTPromiseRejectBlock)reject) {
    NSString *whipUrl = credentials[@"whipUrl"];
    NSString *token = credentials[@"token"];

    if (![whipUrl isKindOfClass:[NSString class]] || whipUrl.length == 0 || ![token isKindOfClass:[NSString class]] ||
        token.length == 0) {
        reject(@"invalid_credentials", @"writeLivestreamCredentials requires non-empty whipUrl and token", nil);
        return;
    }

    NSString *appGroupIdentifier = [[NSBundle mainBundle] infoDictionary][@"RTCAppGroupIdentifier"];
    if (![appGroupIdentifier isKindOfClass:[NSString class]] || appGroupIdentifier.length == 0) {
        reject(@"missing_app_group",
               @"RTCAppGroupIdentifier is not set in Info.plist. Enable livestream screensharing in the config plugin.",
               nil);
        return;
    }

    NSUserDefaults *defaults = [[NSUserDefaults alloc] initWithSuiteName:appGroupIdentifier];
    if (defaults == nil) {
        reject(@"invalid_app_group", @"Could not open UserDefaults for the configured App Group", nil);
        return;
    }

    [defaults setObject:whipUrl forKey:@"livestreamWhipUrl"];
    [defaults setObject:token forKey:@"livestreamToken"];
    // Force a flush so the extension (a separate process) sees the values immediately
    // when it launches right after the broadcast picker is presented.
    [defaults synchronize];
    resolve(nil);
}

#pragma mark - Livestream status channel

// Mirrors the keys/notification the livestream broadcast extension writes/posts.
static NSString *const kLivestreamStatusDarwinNotification = @"iOS_LivestreamStatusChanged";
static NSString *const kLivestreamStatusKey = @"livestreamStatus";
static NSString *const kLivestreamErrorKey = @"livestreamErrorMessage";
// Per-instance flag (via associated object) tracking whether this module registered the Darwin
// observer. Tied to the instance — not process-static — so it is correctly torn down and re-created
// across bridge reloads. See removeLivestreamStatusObserver / -dealloc in WebRTCModule.m.
static void *LivestreamStatusObserverKey = &LivestreamStatusObserverKey;

// Darwin notifications carry no payload — on the signal we read the latest status from the
// shared App Group UserDefaults and emit it to JS. `observer` is the WebRTCModule instance.
static void LivestreamStatusDarwinCallback(CFNotificationCenterRef center,
                                           void *observer,
                                           CFStringRef name,
                                           const void *object,
                                           CFDictionaryRef userInfo) {
    WebRTCModule *module = (__bridge WebRTCModule *)observer;
    [module emitLivestreamStatus];
}

- (NSDictionary *)currentLivestreamStatus {
    NSString *appGroupIdentifier = [[NSBundle mainBundle] infoDictionary][@"RTCAppGroupIdentifier"];
    NSString *status = @"idle";
    NSString *errorMessage = nil;
    if ([appGroupIdentifier isKindOfClass:[NSString class]] && appGroupIdentifier.length > 0) {
        NSUserDefaults *defaults = [[NSUserDefaults alloc] initWithSuiteName:appGroupIdentifier];
        NSString *stored = [defaults stringForKey:kLivestreamStatusKey];
        if (stored.length > 0) {
            status = stored;
        }
        if ([status isEqualToString:@"failed"]) {
            errorMessage = [defaults stringForKey:kLivestreamErrorKey];
        }
    }
    return @{@"status" : status, @"error" : errorMessage ?: (id)[NSNull null]};
}

- (void)emitLivestreamStatus {
    // The Darwin callback fires on an arbitrary run loop; marshal onto the module worker queue so
    // event emission follows the same threading as the rest of the module's event delegates.
    dispatch_async(self.workerQueue, ^{
        [self sendEventWithName:kEventLivestreamStatusChanged body:[self currentLivestreamStatus]];
    });
}

// Removes the Darwin observer registered by startLivestreamStatusObserver, if any. Must run before
// the module deallocs, otherwise the notify center keeps a dangling pointer to a freed instance and
// fires into it on the next status change (use-after-free). Wired into -dealloc in WebRTCModule.m.
- (void)removeLivestreamStatusObserver {
    if (objc_getAssociatedObject(self, LivestreamStatusObserverKey)) {
        CFNotificationCenterRemoveObserver(CFNotificationCenterGetDarwinNotifyCenter(),
                                           (__bridge const void *)self,
                                           (__bridge CFStringRef)kLivestreamStatusDarwinNotification,
                                           NULL);
        objc_setAssociatedObject(self, LivestreamStatusObserverKey, nil, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    }
}

// Registers (idempotently, per instance) the Darwin observer for livestream status changes and
// returns the current status so the caller can initialise its state.
RCT_EXPORT_METHOD(startLivestreamStatusObserver
                  : (RCTPromiseResolveBlock)resolve rejecter
                  : (RCTPromiseRejectBlock)reject) {
    if (!objc_getAssociatedObject(self, LivestreamStatusObserverKey)) {
        CFNotificationCenterAddObserver(CFNotificationCenterGetDarwinNotifyCenter(),
                                        (__bridge const void *)self,
                                        LivestreamStatusDarwinCallback,
                                        (__bridge CFStringRef)kLivestreamStatusDarwinNotification,
                                        NULL,
                                        CFNotificationSuspensionBehaviorDeliverImmediately);
        objc_setAssociatedObject(self, LivestreamStatusObserverKey, @YES, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    }
    resolve([self currentLivestreamStatus]);
}

RCT_EXPORT_METHOD(getLivestreamStatus : (RCTPromiseResolveBlock)resolve rejecter : (RCTPromiseRejectBlock)reject) {
    resolve([self currentLivestreamStatus]);
}

/**
 * Implements {@code getUserMedia}. Note that at this point constraints have
 * been normalized and permissions have been granted. The constraints only
 * contain keys for which permissions have already been granted, that is,
 * if audio permission was not granted, there will be no "audio" key in
 * the constraints dictionary.
 */
RCT_EXPORT_METHOD(getUserMedia
                  : (NSDictionary *)constraints successCallback
                  : (RCTResponseSenderBlock)successCallback errorCallback
                  : (RCTResponseSenderBlock)errorCallback) {
#if TARGET_OS_TV
    errorCallback(@[ @"PlatformNotSupported", @"getUserMedia is not supported on tvOS." ]);
    return;
#else
    RTCAudioTrack *audioTrack = nil;
    RTCVideoTrack *videoTrack = nil;

    if (constraints[@"audio"]) {
        audioTrack = [self createAudioTrack:constraints];
    }
    if (constraints[@"video"]) {
        videoTrack = [self createVideoTrack:constraints];
    }

    if (audioTrack == nil && videoTrack == nil) {
        // Fail with DOMException with name AbortError as per:
        // https://www.w3.org/TR/mediacapture-streams/#dom-mediadevices-getusermedia
        errorCallback(@[ @"DOMException", @"AbortError" ]);
        return;
    }

    NSString *mediaStreamId = [[NSUUID UUID] UUIDString];
    RTCMediaStream *mediaStream = [self.peerConnectionFactory mediaStreamWithStreamId:mediaStreamId];
    NSMutableArray *tracks = [NSMutableArray array];
    NSMutableArray *tmp = [NSMutableArray array];
    if (audioTrack)
        [tmp addObject:audioTrack];
    if (videoTrack)
        [tmp addObject:videoTrack];

    for (RTCMediaStreamTrack *track in tmp) {
        if ([track.kind isEqualToString:@"audio"]) {
            [mediaStream addAudioTrack:(RTCAudioTrack *)track];
        } else if ([track.kind isEqualToString:@"video"]) {
            [mediaStream addVideoTrack:(RTCVideoTrack *)track];
        }

        NSString *trackId = track.trackId;

        self.localTracks[trackId] = track;

        NSDictionary *settings = @{};
        if ([track.kind isEqualToString:@"video"]) {
            RTCVideoTrack *videoTrack = (RTCVideoTrack *)track;
            if ([videoTrack.captureController isKindOfClass:[CaptureController class]]) {
                settings = [videoTrack.captureController getSettings];
            }
        } else if ([track.kind isEqualToString:@"audio"]) {
            settings = @{
                @"deviceId" : @"audio",
                @"groupId" : @"",
            };
        }

        [tracks addObject:@{
            @"enabled" : @(track.isEnabled),
            @"id" : trackId,
            @"kind" : track.kind,
            @"readyState" : @"live",
            @"remote" : @(NO),
            @"settings" : settings
        }];
    }

    self.localStreams[mediaStreamId] = mediaStream;
    successCallback(@[ mediaStreamId, tracks ]);
#endif
}

#pragma mark - Other stream related APIs

RCT_EXPORT_METHOD(enumerateDevices : (RCTResponseSenderBlock)callback) {
#if TARGET_OS_TV
    callback(@[]);
#else
    NSMutableArray *devices = [NSMutableArray array];
    NSMutableArray *deviceTypes = [NSMutableArray array];
    [deviceTypes addObjectsFromArray:@[
        AVCaptureDeviceTypeBuiltInWideAngleCamera,
        AVCaptureDeviceTypeBuiltInUltraWideCamera,
        AVCaptureDeviceTypeBuiltInTelephotoCamera,
        AVCaptureDeviceTypeBuiltInDualCamera,
        AVCaptureDeviceTypeBuiltInDualWideCamera,
        AVCaptureDeviceTypeBuiltInTripleCamera
    ]];
    if (@available(macos 14.0, ios 17.0, tvos 17.0, *)) {
        [deviceTypes addObject:AVCaptureDeviceTypeExternal];
    }
    AVCaptureDeviceDiscoverySession *videoDevicesSession =
        [AVCaptureDeviceDiscoverySession discoverySessionWithDeviceTypes:deviceTypes
                                                               mediaType:AVMediaTypeVideo
                                                                position:AVCaptureDevicePositionUnspecified];
    for (AVCaptureDevice *device in videoDevicesSession.devices) {
        if (device.uniqueID == nil) {
            continue;
        }
        NSString *position = @"unknown";
        if (device.position == AVCaptureDevicePositionBack) {
            position = @"environment";
        } else if (device.position == AVCaptureDevicePositionFront) {
            position = @"front";
        }
        NSString *label = @"Unknown video device";
        if (device.localizedName != nil) {
            label = device.localizedName;
        }

        [devices addObject:@{
            @"facing" : position,
            @"deviceId" : device.uniqueID,
            @"groupId" : @"",
            @"label" : label,
            @"kind" : @"videoinput",
        }];
    }

    AVCaptureDeviceDiscoverySession *audioDevicesSession =
        [AVCaptureDeviceDiscoverySession discoverySessionWithDeviceTypes:@[ AVCaptureDeviceTypeBuiltInMicrophone ]
                                                               mediaType:AVMediaTypeAudio
                                                                position:AVCaptureDevicePositionUnspecified];
    for (AVCaptureDevice *device in audioDevicesSession.devices) {
        if (device.uniqueID == nil) {
            continue;
        }
        NSString *label = @"Unknown audio device";
        if (device.localizedName != nil) {
            label = device.localizedName;
        }
        [devices addObject:@{
            @"deviceId" : device.uniqueID,
            @"groupId" : @"",
            @"label" : label,
            @"kind" : @"audioinput",
        }];
    }
    callback(@[ devices ]);
#endif
}

RCT_EXPORT_METHOD(mediaStreamCreate : (nonnull NSString *)streamID) {
    RTCMediaStream *mediaStream = [self.peerConnectionFactory mediaStreamWithStreamId:streamID];
    self.localStreams[streamID] = mediaStream;
}

RCT_EXPORT_METHOD(mediaStreamAddTrack
                  : (nonnull NSString *)streamID
                  : (nonnull NSNumber *)pcId
                  : (nonnull NSString *)trackID) {
    RTCMediaStream *mediaStream = self.localStreams[streamID];
    if (mediaStream == nil) {
        return;
    }

    RTCMediaStreamTrack *track = [self trackForId:trackID pcId:pcId];
    if (track == nil) {
        return;
    }

    if ([track.kind isEqualToString:@"audio"]) {
        [mediaStream addAudioTrack:(RTCAudioTrack *)track];
    } else if ([track.kind isEqualToString:@"video"]) {
        [mediaStream addVideoTrack:(RTCVideoTrack *)track];
        [[NSNotificationCenter defaultCenter] postNotificationName:kMediaStreamVideoTracksChangedNotification
                                                            object:nil
                                                          userInfo:@{@"streamId" : streamID}];
    }
}

RCT_EXPORT_METHOD(mediaStreamRemoveTrack
                  : (nonnull NSString *)streamID
                  : (nonnull NSNumber *)pcId
                  : (nonnull NSString *)trackID) {
    RTCMediaStream *mediaStream = self.localStreams[streamID];
    if (mediaStream == nil) {
        return;
    }

    RTCMediaStreamTrack *track = [self trackForId:trackID pcId:pcId];
    if (track == nil) {
        return;
    }

    if ([track.kind isEqualToString:@"audio"]) {
        [mediaStream removeAudioTrack:(RTCAudioTrack *)track];
    } else if ([track.kind isEqualToString:@"video"]) {
        [mediaStream removeVideoTrack:(RTCVideoTrack *)track];
        [[NSNotificationCenter defaultCenter] postNotificationName:kMediaStreamVideoTracksChangedNotification
                                                            object:nil
                                                          userInfo:@{@"streamId" : streamID}];
    }
}

RCT_EXPORT_METHOD(mediaStreamRelease : (nonnull NSString *)streamID) {
    RTCMediaStream *stream = self.localStreams[streamID];
    if (stream) {
        [self.localStreams removeObjectForKey:streamID];
    }
}

RCT_EXPORT_METHOD(mediaStreamTrackRelease : (nonnull NSString *)trackID) {
#if TARGET_OS_TV
    return;
#else

    RTCMediaStreamTrack *track = self.localTracks[trackID];
    if (track) {
        track.isEnabled = NO;
#if !TARGET_OS_OSX
        if ([track.captureController isKindOfClass:[CustomVideoCaptureController class]]) {
            CustomVideoCaptureController *customController = (CustomVideoCaptureController *)track.captureController;
            [customController stopCapture];
            [customController releaseCaptureResources];
        } else {
            [track.captureController stopCapture];
        }
#else
        [track.captureController stopCapture];
#endif
        [self.localTracks removeObjectForKey:trackID];
    }
#endif
}

RCT_EXPORT_METHOD(mediaStreamTrackSetEnabled : (nonnull NSNumber *)pcId : (nonnull NSString *)trackID : (BOOL)enabled) {
    RTCMediaStreamTrack *track = [self trackForId:trackID pcId:pcId];
    if (track == nil) {
        return;
    }

    track.isEnabled = enabled;
#if !TARGET_OS_TV
    if (track.captureController) {  // It could be a remote track!
        if (enabled) {
            [track.captureController startCapture];
        } else {
            [track.captureController stopCapture];
        }
    }
#endif
}

RCT_EXPORT_METHOD(mediaStreamTrackApplyConstraints
                  : (nonnull NSString *)trackID
                  : (NSDictionary *)constraints
                  : (RCTPromiseResolveBlock)resolve
                  : (RCTPromiseRejectBlock)reject) {
#if TARGET_OS_TV
    reject(@"unsupported_platform", @"tvOS is not supported", nil);
    return;
#else
    RTCMediaStreamTrack *track = self.localTracks[trackID];
    if (track) {
        if ([track.kind isEqualToString:@"video"]) {
            RTCVideoTrack *videoTrack = (RTCVideoTrack *)track;
            if ([videoTrack.captureController isKindOfClass:[CaptureController class]]) {
                CaptureController *vcc = (CaptureController *)videoTrack.captureController;
                NSError *error = nil;
                [vcc applyConstraints:constraints error:&error];
                if (error) {
                    reject(@"E_INVALID", error.localizedDescription, error);
                } else {
                    resolve([vcc getSettings]);
                }
            }
        } else {
            RCTLogWarn(@"mediaStreamTrackApplyConstraints() track is not video");
            reject(@"E_INVALID", @"Can't apply constraints on audio tracks", nil);
        }
    } else {
        RCTLogWarn(@"mediaStreamTrackApplyConstraints() track is null");
        reject(@"E_INVALID", @"Could not get track", nil);
    }
#endif
}

RCT_EXPORT_METHOD(mediaStreamTrackSetVolume : (nonnull NSNumber *)pcId : (nonnull NSString *)trackID : (double)volume) {
    RTCMediaStreamTrack *track = [self trackForId:trackID pcId:pcId];
    if (track && [track.kind isEqualToString:@"audio"]) {
        RTCAudioTrack *audioTrack = (RTCAudioTrack *)track;
        audioTrack.source.volume = volume;
    }
}

RCT_EXPORT_METHOD(mediaStreamTrackSetVideoEffects
                  : (nonnull NSString *)trackID names
                  : (nonnull NSArray<NSString *> *)names) {
    RTCMediaStreamTrack *track = self.localTracks[trackID];
    if (track == nil) {
        return;
    }

    RTCVideoTrack *videoTrack = (RTCVideoTrack *)track;
    RTCVideoSource *videoSource = videoTrack.source;

    NSMutableArray *processors = [[NSMutableArray alloc] init];
    for (NSString *name in names) {
        NSObject<VideoFrameProcessorDelegate> *processor = [ProcessorProvider getProcessor:name];
        if (processor != nil) {
            [processors addObject:processor];
        }
    }

    self.videoEffectProcessor = [[VideoEffectProcessor alloc] initWithProcessors:processors videoSource:videoSource];

    VideoCaptureController *vcc = (VideoCaptureController *)videoTrack.captureController;
    RTCVideoCapturer *capturer = vcc.capturer;

    capturer.delegate = self.videoEffectProcessor;
}

#pragma mark - Helpers

- (RTCMediaStreamTrack *)trackForId:(nonnull NSString *)trackId pcId:(nonnull NSNumber *)pcId {
    if ([pcId isEqualToNumber:[NSNumber numberWithInt:-1]]) {
        return self.localTracks[trackId];
    }

    RTCPeerConnection *peerConnection = self.peerConnections[pcId];
    if (peerConnection == nil) {
        return nil;
    }

    return peerConnection.remoteTracks[trackId];
}

@end
