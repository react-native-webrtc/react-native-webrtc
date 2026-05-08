#import "FishjamRTCAudioDevice.h"

#if TARGET_OS_IOS

#import <AVFoundation/AVFoundation.h>
#import <AudioToolbox/AudioToolbox.h>

static const int kMaxNumberOfAudioUnitInitializeAttempts = 5;
static const AudioUnitElement kInputBus = 1;
static const AudioUnitElement kOutputBus = 0;
static const UInt32 kBytesPerSample = 2;
static const int kPreferredNumberOfChannels = 1;
static const double kHighPerformanceSampleRate = 48000.0;
static const double kHighPerformanceIOBufferDuration = 0.02;

typedef NS_ENUM(NSInteger, FishjamAudioUnitState) {
    FishjamAudioUnitStateInitRequired,
    FishjamAudioUnitStateUninitialized,
    FishjamAudioUnitStateInitialized,
    FishjamAudioUnitStateStarted,
};

@interface FishjamRTCAudioDevice () {
    AudioUnit _vpioUnit;
    FishjamAudioUnitState _state;
    Float64 _configuredSampleRate;
    double _reportedInputSampleRate;
    double _reportedOutputSampleRate;
}

@property(nonatomic, weak) id<RTCAudioDeviceDelegate> delegate;
@property(nonatomic, assign) BOOL shouldPlay;
@property(nonatomic, assign) BOOL shouldRecord;
@property(nonatomic, assign) BOOL interrupted;

@end

@implementation FishjamRTCAudioDevice

- (instancetype)init {
    self = [super init];
    if (self) {
        _vpioUnit = NULL;
        _state = FishjamAudioUnitStateInitRequired;
        _configuredSampleRate = 0.0;
        _reportedInputSampleRate = 0.0;
        _reportedOutputSampleRate = 0.0;
    }
    return self;
}

- (void)dealloc {
    [self disposeAudioUnit];
}

- (AVAudioSession *)audioSession {
    return [AVAudioSession sharedInstance];
}

#pragma mark - RTCAudioDevice properties

- (double)deviceInputSampleRate {
    return _reportedInputSampleRate > 0 ? _reportedInputSampleRate : self.audioSession.sampleRate;
}

- (double)deviceOutputSampleRate {
    return _reportedOutputSampleRate > 0 ? _reportedOutputSampleRate : self.audioSession.sampleRate;
}

- (NSTimeInterval)inputIOBufferDuration {
    return self.audioSession.IOBufferDuration;
}

- (NSTimeInterval)outputIOBufferDuration {
    return self.audioSession.IOBufferDuration;
}

- (NSInteger)inputNumberOfChannels {
    return kPreferredNumberOfChannels;
}

- (NSInteger)outputNumberOfChannels {
    return kPreferredNumberOfChannels;
}

- (NSTimeInterval)inputLatency {
    return self.audioSession.inputLatency;
}

- (NSTimeInterval)outputLatency {
    return self.audioSession.outputLatency;
}

#pragma mark - RTCAudioDevice lifecycle

- (BOOL)isInitialized {
    return self.delegate != nil && _vpioUnit != NULL;
}

- (BOOL)initializeWithDelegate:(id<RTCAudioDeviceDelegate>)delegate {
    if (self.delegate != nil) {
        return NO;
    }
    if (![self createAudioUnit]) {
        return NO;
    }
    self.delegate = delegate;
    [self subscribeToNotifications];
    return YES;
}

- (BOOL)terminateDevice {
    [self unsubscribeFromNotifications];
    self.shouldPlay = NO;
    self.shouldRecord = NO;
    [self disposeAudioUnit];
    _reportedInputSampleRate = 0.0;
    _reportedOutputSampleRate = 0.0;
    self.delegate = nil;
    return YES;
}

- (BOOL)isPlayoutInitialized {
    return self.isInitialized;
}

- (BOOL)initializePlayout {
    return self.isPlayoutInitialized;
}

- (BOOL)isPlaying {
    return self.shouldPlay;
}

- (BOOL)startPlayout {
    self.shouldPlay = YES;
    [self updateAudioUnit];
    return YES;
}

- (BOOL)stopPlayout {
    self.shouldPlay = NO;
    [self updateAudioUnit];
    return YES;
}

- (BOOL)isRecordingInitialized {
    return self.isInitialized;
}

- (BOOL)initializeRecording {
    return self.isRecordingInitialized;
}

- (BOOL)isRecording {
    return self.shouldRecord;
}

- (BOOL)startRecording {
    self.shouldRecord = YES;
    [self updateAudioUnit];
    return YES;
}

- (BOOL)stopRecording {
    self.shouldRecord = NO;
    [self updateAudioUnit];
    return YES;
}

#pragma mark - Audio Session configuration

- (void)configureAudioSession {
    NSError *error = nil;
    AVAudioSession *session = self.audioSession;

    NSString *targetCategory;
    NSString *targetMode;
    AVAudioSessionCategoryOptions targetOptions;

    if (self.shouldRecord) {
        targetCategory = AVAudioSessionCategoryPlayAndRecord;
        targetMode = AVAudioSessionModeVoiceChat;
        targetOptions = AVAudioSessionCategoryOptionAllowBluetooth | AVAudioSessionCategoryOptionDefaultToSpeaker;
    } else {
        targetCategory = AVAudioSessionCategoryPlayback;
        targetMode = AVAudioSessionModeDefault;
        targetOptions = 0;
    }

    if (![session.category isEqualToString:targetCategory] || session.categoryOptions != targetOptions) {
        if (![session setCategory:targetCategory withOptions:targetOptions error:&error]) {
            NSLog(@"[FishjamRTCAudioDevice] Failed to set category %@: %@", targetCategory, error);
            error = nil;
        }
    }

    if (![session.mode isEqualToString:targetMode]) {
        if (![session setMode:targetMode error:&error]) {
            NSLog(@"[FishjamRTCAudioDevice] Failed to set mode %@: %@", targetMode, error);
            error = nil;
        }
    }

    if (session.preferredSampleRate != kHighPerformanceSampleRate) {
        if (![session setPreferredSampleRate:kHighPerformanceSampleRate error:&error]) {
            NSLog(@"[FishjamRTCAudioDevice] Failed to set preferred sample rate: %@", error);
            error = nil;
        }
    }

    if (session.preferredIOBufferDuration != kHighPerformanceIOBufferDuration) {
        if (![session setPreferredIOBufferDuration:kHighPerformanceIOBufferDuration error:&error]) {
            NSLog(@"[FishjamRTCAudioDevice] Failed to set preferred IO buffer duration: %@", error);
            error = nil;
        }
    }

    if (![session setActive:YES error:&error]) {
        NSLog(@"[FishjamRTCAudioDevice] Failed to activate audio session: %@", error);
    }
}

#pragma mark - AudioUnit format

- (AudioStreamBasicDescription)streamFormatForSampleRate:(Float64)sampleRate {
    AudioStreamBasicDescription format;
    memset(&format, 0, sizeof(format));
    format.mSampleRate = sampleRate;
    format.mFormatID = kAudioFormatLinearPCM;
    format.mFormatFlags = kLinearPCMFormatFlagIsSignedInteger | kLinearPCMFormatFlagIsPacked;
    format.mBytesPerPacket = kBytesPerSample;
    format.mFramesPerPacket = 1;
    format.mBytesPerFrame = kBytesPerSample;
    format.mChannelsPerFrame = kPreferredNumberOfChannels;
    format.mBitsPerChannel = 8 * kBytesPerSample;
    return format;
}

#pragma mark - C AudioUnit callbacks

static OSStatus FishjamOnGetPlayoutData(void *inRefCon,
                                        AudioUnitRenderActionFlags *ioActionFlags,
                                        const AudioTimeStamp *inTimeStamp,
                                        UInt32 inBusNumber,
                                        UInt32 inNumberFrames,
                                        AudioBufferList *ioData) {
    FishjamRTCAudioDevice *device = (__bridge FishjamRTCAudioDevice *)inRefCon;
    return [device notifyGetPlayoutData:ioActionFlags
                              timestamp:inTimeStamp
                              busNumber:inBusNumber
                             frameCount:inNumberFrames
                                 ioData:ioData];
}

static OSStatus FishjamOnDeliverRecordedData(void *inRefCon,
                                             AudioUnitRenderActionFlags *ioActionFlags,
                                             const AudioTimeStamp *inTimeStamp,
                                             UInt32 inBusNumber,
                                             UInt32 inNumberFrames,
                                             AudioBufferList *ioData) {
    FishjamRTCAudioDevice *device = (__bridge FishjamRTCAudioDevice *)inRefCon;
    return [device notifyDeliverRecordedData:ioActionFlags
                                   timestamp:inTimeStamp
                                   busNumber:inBusNumber
                                  frameCount:inNumberFrames
                                      ioData:ioData];
}

- (OSStatus)notifyGetPlayoutData:(AudioUnitRenderActionFlags *)ioActionFlags
                       timestamp:(const AudioTimeStamp *)inTimeStamp
                       busNumber:(UInt32)inBusNumber
                      frameCount:(UInt32)inNumberFrames
                          ioData:(AudioBufferList *)ioData {
    id<RTCAudioDeviceDelegate> delegate = self.delegate;
    if (delegate == nil || !self.shouldPlay) {
        *ioActionFlags |= kAudioUnitRenderAction_OutputIsSilence;
        if (ioData != NULL) {
            for (UInt32 i = 0; i < ioData->mNumberBuffers; i++) {
                if (ioData->mBuffers[i].mData != NULL) {
                    memset(ioData->mBuffers[i].mData, 0, ioData->mBuffers[i].mDataByteSize);
                }
            }
        }
        return noErr;
    }
    return delegate.getPlayoutData(ioActionFlags, inTimeStamp, (NSInteger)inBusNumber, inNumberFrames, ioData);
}

- (OSStatus)notifyDeliverRecordedData:(AudioUnitRenderActionFlags *)ioActionFlags
                            timestamp:(const AudioTimeStamp *)inTimeStamp
                            busNumber:(UInt32)inBusNumber
                           frameCount:(UInt32)inNumberFrames
                               ioData:(AudioBufferList *)ioData {
    id<RTCAudioDeviceDelegate> delegate = self.delegate;
    if (delegate == nil || !self.shouldRecord || _vpioUnit == NULL) {
        return noErr;
    }

    AudioUnit vpioUnit = _vpioUnit;
    RTCAudioDeviceRenderRecordedDataBlock renderBlock = ^OSStatus(AudioUnitRenderActionFlags *_Nonnull actionFlags,
                                                                  const AudioTimeStamp *_Nonnull timestamp,
                                                                  NSInteger inputBusNumber,
                                                                  UInt32 frameCount,
                                                                  AudioBufferList *_Nonnull abl,
                                                                  void *_Nullable renderContext) {
        return AudioUnitRender(vpioUnit, actionFlags, timestamp, (UInt32)inputBusNumber, frameCount, abl);
    };

    OSStatus status = delegate.deliverRecordedData(
        ioActionFlags, inTimeStamp, (NSInteger)inBusNumber, inNumberFrames, NULL, NULL, renderBlock);
    if (status != noErr) {
        NSLog(@"[FishjamRTCAudioDevice] Failed to deliver recorded data: %d", (int)status);
    }
    return status;
}

#pragma mark - AudioUnit lifecycle (C API, mirrors voice_processing_audio_unit.mm)

- (BOOL)createAudioUnit {
    if (_vpioUnit != NULL) {
        return YES;
    }

    BOOL inputEnabled = self.shouldRecord;

    AudioComponentDescription vpioDescription;
    vpioDescription.componentType = kAudioUnitType_Output;
    vpioDescription.componentSubType = kAudioUnitSubType_VoiceProcessingIO;
    vpioDescription.componentManufacturer = kAudioUnitManufacturer_Apple;
    vpioDescription.componentFlags = 0;
    vpioDescription.componentFlagsMask = 0;

    AudioComponent foundVpioRef = AudioComponentFindNext(NULL, &vpioDescription);
    if (foundVpioRef == NULL) {
        NSLog(@"[FishjamRTCAudioDevice] AudioComponentFindNext failed.");
        return NO;
    }

    OSStatus result = AudioComponentInstanceNew(foundVpioRef, &_vpioUnit);
    if (result != noErr) {
        _vpioUnit = NULL;
        NSLog(@"[FishjamRTCAudioDevice] AudioComponentInstanceNew failed. Error=%d.", (int)result);
        return NO;
    }

    UInt32 enableInput = inputEnabled ? 1 : 0;
    result = AudioUnitSetProperty(_vpioUnit,
                                  kAudioOutputUnitProperty_EnableIO,
                                  kAudioUnitScope_Input,
                                  kInputBus,
                                  &enableInput,
                                  sizeof(enableInput));
    if (result != noErr) {
        [self disposeAudioUnit];
        NSLog(@"[FishjamRTCAudioDevice] Failed to configure input enable=%u. Error=%d.",
              (unsigned)enableInput,
              (int)result);
        return NO;
    }

    UInt32 enableOutput = 1;
    result = AudioUnitSetProperty(_vpioUnit,
                                  kAudioOutputUnitProperty_EnableIO,
                                  kAudioUnitScope_Output,
                                  kOutputBus,
                                  &enableOutput,
                                  sizeof(enableOutput));
    if (result != noErr) {
        [self disposeAudioUnit];
        NSLog(@"[FishjamRTCAudioDevice] Failed to enable output. Error=%d.", (int)result);
        return NO;
    }

    AURenderCallbackStruct renderCallback;
    renderCallback.inputProc = FishjamOnGetPlayoutData;
    renderCallback.inputProcRefCon = (__bridge void *)self;
    result = AudioUnitSetProperty(_vpioUnit,
                                  kAudioUnitProperty_SetRenderCallback,
                                  kAudioUnitScope_Input,
                                  kOutputBus,
                                  &renderCallback,
                                  sizeof(renderCallback));
    if (result != noErr) {
        [self disposeAudioUnit];
        NSLog(@"[FishjamRTCAudioDevice] Failed to set render callback. Error=%d.", (int)result);
        return NO;
    }

    if (inputEnabled) {
        UInt32 flag = 0;
        result = AudioUnitSetProperty(
            _vpioUnit, kAudioUnitProperty_ShouldAllocateBuffer, kAudioUnitScope_Output, kInputBus, &flag, sizeof(flag));
        if (result != noErr) {
            [self disposeAudioUnit];
            NSLog(@"[FishjamRTCAudioDevice] Failed to disable buffer allocation. Error=%d.", (int)result);
            return NO;
        }

        AURenderCallbackStruct inputCallback;
        inputCallback.inputProc = FishjamOnDeliverRecordedData;
        inputCallback.inputProcRefCon = (__bridge void *)self;
        result = AudioUnitSetProperty(_vpioUnit,
                                      kAudioOutputUnitProperty_SetInputCallback,
                                      kAudioUnitScope_Global,
                                      kInputBus,
                                      &inputCallback,
                                      sizeof(inputCallback));
        if (result != noErr) {
            [self disposeAudioUnit];
            NSLog(@"[FishjamRTCAudioDevice] Failed to set input callback. Error=%d.", (int)result);
            return NO;
        }
    }

    _state = FishjamAudioUnitStateUninitialized;
    return YES;
}

- (BOOL)initializeAudioUnitWithSampleRate:(Float64)sampleRate {
    if (_vpioUnit == NULL || _state < FishjamAudioUnitStateUninitialized) {
        return NO;
    }

    AudioStreamBasicDescription format = [self streamFormatForSampleRate:sampleRate];
    UInt32 size = sizeof(format);

    OSStatus result = AudioUnitSetProperty(
        _vpioUnit, kAudioUnitProperty_StreamFormat, kAudioUnitScope_Output, kInputBus, &format, size);
    if (result != noErr) {
        NSLog(@"[FishjamRTCAudioDevice] Failed to set format on output scope of input bus. Error=%d.", (int)result);
        return NO;
    }

    result = AudioUnitSetProperty(
        _vpioUnit, kAudioUnitProperty_StreamFormat, kAudioUnitScope_Input, kOutputBus, &format, size);
    if (result != noErr) {
        NSLog(@"[FishjamRTCAudioDevice] Failed to set format on input scope of output bus. Error=%d.", (int)result);
        return NO;
    }

    int failedInitializeAttempts = 0;
    result = AudioUnitInitialize(_vpioUnit);
    while (result != noErr) {
        NSLog(@"[FishjamRTCAudioDevice] Failed to initialize audio unit. Error=%d.", (int)result);
        ++failedInitializeAttempts;
        if (failedInitializeAttempts == kMaxNumberOfAudioUnitInitializeAttempts) {
            NSLog(@"[FishjamRTCAudioDevice] Too many initialization attempts.");
            return NO;
        }
        [NSThread sleepForTimeInterval:0.1];
        result = AudioUnitInitialize(_vpioUnit);
    }

    if (self.shouldRecord) {
        UInt32 agcEnabled = 0;
        UInt32 agcSize = sizeof(agcEnabled);
        OSStatus agcResult = AudioUnitGetProperty(_vpioUnit,
                                                  kAUVoiceIOProperty_VoiceProcessingEnableAGC,
                                                  kAudioUnitScope_Global,
                                                  kInputBus,
                                                  &agcEnabled,
                                                  &agcSize);
        if (agcResult == noErr && !agcEnabled) {
            UInt32 enableAgc = 1;
            AudioUnitSetProperty(_vpioUnit,
                                 kAUVoiceIOProperty_VoiceProcessingEnableAGC,
                                 kAudioUnitScope_Global,
                                 kInputBus,
                                 &enableAgc,
                                 sizeof(enableAgc));
        }
    }

    _configuredSampleRate = sampleRate;
    _state = FishjamAudioUnitStateInitialized;
    return YES;
}

- (BOOL)startAudioUnit {
    if (_vpioUnit == NULL || _state != FishjamAudioUnitStateInitialized) {
        return NO;
    }
    OSStatus result = AudioOutputUnitStart(_vpioUnit);
    if (result != noErr) {
        NSLog(@"[FishjamRTCAudioDevice] Failed to start audio unit. Error=%d.", (int)result);
        return NO;
    }
    _state = FishjamAudioUnitStateStarted;
    return YES;
}

- (BOOL)stopAudioUnit {
    if (_vpioUnit == NULL || _state != FishjamAudioUnitStateStarted) {
        return YES;
    }
    OSStatus result = AudioOutputUnitStop(_vpioUnit);
    if (result != noErr) {
        NSLog(@"[FishjamRTCAudioDevice] Failed to stop audio unit. Error=%d.", (int)result);
        return NO;
    }
    _state = FishjamAudioUnitStateInitialized;
    return YES;
}

- (BOOL)uninitializeAudioUnit {
    if (_vpioUnit == NULL || _state < FishjamAudioUnitStateInitialized) {
        return YES;
    }
    if (_state == FishjamAudioUnitStateStarted) {
        [self stopAudioUnit];
    }
    OSStatus result = AudioUnitUninitialize(_vpioUnit);
    if (result != noErr) {
        NSLog(@"[FishjamRTCAudioDevice] Failed to uninitialize audio unit. Error=%d.", (int)result);
        return NO;
    }
    _state = FishjamAudioUnitStateUninitialized;
    return YES;
}

- (void)disposeAudioUnit {
    if (_vpioUnit == NULL) {
        _state = FishjamAudioUnitStateInitRequired;
        return;
    }

    if (_state == FishjamAudioUnitStateStarted) {
        [self stopAudioUnit];
    }
    if (_state == FishjamAudioUnitStateInitialized) {
        [self uninitializeAudioUnit];
    }

    OSStatus result = AudioComponentInstanceDispose(_vpioUnit);
    if (result != noErr) {
        NSLog(@"[FishjamRTCAudioDevice] AudioComponentInstanceDispose failed. Error=%d.", (int)result);
    }
    _vpioUnit = NULL;
    _state = FishjamAudioUnitStateInitRequired;
    _configuredSampleRate = 0.0;
}

#pragma mark - Top-level update (mirrors audio_device_ios.mm UpdateAudioUnit)

- (BOOL)audioUnitNeedsRecreate {
    if (_vpioUnit == NULL) {
        return YES;
    }
    UInt32 currentInputEnabled = 0;
    UInt32 size = sizeof(currentInputEnabled);
    OSStatus result = AudioUnitGetProperty(
        _vpioUnit, kAudioOutputUnitProperty_EnableIO, kAudioUnitScope_Input, kInputBus, &currentInputEnabled, &size);
    if (result != noErr) {
        return YES;
    }
    return (currentInputEnabled != 0) != self.shouldRecord;
}

- (void)updateAudioUnit {
    id<RTCAudioDeviceDelegate> delegate = self.delegate;
    BOOL shouldBeActive = (delegate != nil) && (self.shouldPlay || self.shouldRecord) && !self.interrupted;

    if (!shouldBeActive) {
        if (_vpioUnit != NULL) {
            [self stopAudioUnit];
            [self uninitializeAudioUnit];
            if (delegate) {
                [delegate notifyAudioInputInterrupted];
                [delegate notifyAudioOutputInterrupted];
            }
        }
        return;
    }

    [self configureAudioSession];

    Float64 targetSampleRate = self.audioSession.sampleRate;
    if (targetSampleRate <= 0) {
        targetSampleRate = kHighPerformanceSampleRate;
    }

    if (_vpioUnit != NULL && _state >= FishjamAudioUnitStateInitialized && _configuredSampleRate != targetSampleRate) {
        [self uninitializeAudioUnit];
    }

    if ([self audioUnitNeedsRecreate]) {
        [self disposeAudioUnit];
        if (![self createAudioUnit]) {
            return;
        }
    }

    if (_state == FishjamAudioUnitStateUninitialized) {
        if (![self initializeAudioUnitWithSampleRate:targetSampleRate]) {
            return;
        }
    }

    if (_state == FishjamAudioUnitStateInitialized) {
        if (![self startAudioUnit]) {
            return;
        }
    }

    [self notifySampleRateChangeIfNeeded];
}

- (void)notifySampleRateChangeIfNeeded {
    id<RTCAudioDeviceDelegate> delegate = self.delegate;
    if (delegate == nil || _configuredSampleRate <= 0) {
        return;
    }

    BOOL inputChanged = self.shouldRecord && _reportedInputSampleRate != _configuredSampleRate;
    BOOL outputChanged = self.shouldPlay && _reportedOutputSampleRate != _configuredSampleRate;
    if (!inputChanged && !outputChanged) {
        return;
    }

    if (inputChanged) {
        _reportedInputSampleRate = _configuredSampleRate;
    }
    if (outputChanged) {
        _reportedOutputSampleRate = _configuredSampleRate;
    }

    __weak typeof(self) weakSelf = self;
    [delegate dispatchAsync:^{
        id<RTCAudioDeviceDelegate> strongDelegate = weakSelf.delegate;
        if (strongDelegate == nil) {
            return;
        }
        if (inputChanged) {
            [strongDelegate notifyAudioInputParametersChange];
        }
        if (outputChanged) {
            [strongDelegate notifyAudioOutputParametersChange];
        }
    }];
}

#pragma mark - Audio Session Notifications

- (void)subscribeToNotifications {
    NSNotificationCenter *center = [NSNotificationCenter defaultCenter];
    [center addObserver:self
               selector:@selector(handleInterruption:)
                   name:AVAudioSessionInterruptionNotification
                 object:nil];
    [center addObserver:self
               selector:@selector(handleRouteChange:)
                   name:AVAudioSessionRouteChangeNotification
                 object:nil];
    [center addObserver:self
               selector:@selector(handleMediaServerReset:)
                   name:AVAudioSessionMediaServicesWereResetNotification
                 object:nil];
}

- (void)unsubscribeFromNotifications {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (void)handleInterruption:(NSNotification *)notification {
    NSUInteger type = [notification.userInfo[AVAudioSessionInterruptionTypeKey] unsignedIntegerValue];
    self.interrupted = (type == AVAudioSessionInterruptionTypeBegan);

    id<RTCAudioDeviceDelegate> delegate = self.delegate;
    if (delegate) {
        __weak typeof(self) weakSelf = self;
        [delegate dispatchAsync:^{
            [weakSelf updateAudioUnit];
        }];
    }
}

- (void)handleRouteChange:(NSNotification *)notification {
    id<RTCAudioDeviceDelegate> delegate = self.delegate;
    if (delegate) {
        __weak typeof(self) weakSelf = self;
        [delegate dispatchAsync:^{
            [weakSelf updateAudioUnit];
        }];
    }
}

- (void)handleMediaServerReset:(NSNotification *)notification {
    id<RTCAudioDeviceDelegate> delegate = self.delegate;
    if (delegate) {
        __weak typeof(self) weakSelf = self;
        [delegate dispatchAsync:^{
            [weakSelf disposeAudioUnit];
            [weakSelf createAudioUnit];
            [weakSelf updateAudioUnit];
        }];
    }
}

@end

#else  // !TARGET_OS_IOS

@implementation FishjamRTCAudioDevice

- (double)deviceInputSampleRate {
    return 0;
}
- (double)deviceOutputSampleRate {
    return 0;
}
- (NSTimeInterval)inputIOBufferDuration {
    return 0;
}
- (NSTimeInterval)outputIOBufferDuration {
    return 0;
}
- (NSInteger)inputNumberOfChannels {
    return 0;
}
- (NSInteger)outputNumberOfChannels {
    return 0;
}
- (NSTimeInterval)inputLatency {
    return 0;
}
- (NSTimeInterval)outputLatency {
    return 0;
}
- (BOOL)isInitialized {
    return NO;
}
- (BOOL)initializeWithDelegate:(id<RTCAudioDeviceDelegate>)delegate {
    return NO;
}
- (BOOL)terminateDevice {
    return NO;
}
- (BOOL)isPlayoutInitialized {
    return NO;
}
- (BOOL)initializePlayout {
    return NO;
}
- (BOOL)isPlaying {
    return NO;
}
- (BOOL)startPlayout {
    return NO;
}
- (BOOL)stopPlayout {
    return NO;
}
- (BOOL)isRecordingInitialized {
    return NO;
}
- (BOOL)initializeRecording {
    return NO;
}
- (BOOL)isRecording {
    return NO;
}
- (BOOL)startRecording {
    return NO;
}
- (BOOL)stopRecording {
    return NO;
}

@end

#endif
