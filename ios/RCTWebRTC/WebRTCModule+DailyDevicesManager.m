//
//  WebRTCModule+DailyDevicesManager.m
//  react-native-webrtc
//
//  Created by Filipi Fuchter on 08/03/22.
//

#import <objc/runtime.h>
#import "WebRTCModule.h"
#import "WebRTCModule+DailyDevicesManager.h"
#import "WebRTCModule+DailyAudioMode.h"

@implementation WebRTCModule (DailyDevicesManager)

static NSString const *DEVICE_KIND_VIDEO_INPUT = @"videoinput";
static NSString const *DEVICE_KIND_AUDIO = @"audio";

- (void)setUserSelectedDevice:(NSString *)userSelectedDevice {
  objc_setAssociatedObject(self,
                           @selector(userSelectedDevice),
                           userSelectedDevice,
                           OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (NSString *)userSelectedDevice {
  return  objc_getAssociatedObject(self, @selector(userSelectedDevice));
}

RCT_EXPORT_METHOD(enumerateDevices:(RCTResponseSenderBlock)callback)
{
    NSLog(@"[Daily] enumerateDevice from DailyDevicesManager");
    NSMutableArray *devices = [NSMutableArray array];
    
    [self fillVideoInputDevices:devices];
    [self fillAudioDevices:devices];
    
    callback(@[devices]);
}

// Whenever any headphones plugged in, it becomes the default audio route even if there is also bluetooth device.
// And it overwrites the handset(iPhone) option, which means you cannot change to the handset(iPhone).
- (void)fillVideoInputDevices:(NSMutableArray *)devices {
    AVCaptureDeviceDiscoverySession *videoevicesSession
        = [AVCaptureDeviceDiscoverySession discoverySessionWithDeviceTypes:@[ AVCaptureDeviceTypeBuiltInWideAngleCamera ]
                                                                 mediaType:AVMediaTypeVideo
                                                                  position:AVCaptureDevicePositionUnspecified];
    for (AVCaptureDevice *device in videoevicesSession.devices) {
        NSString *position = @"unknown";
        if (device.position == AVCaptureDevicePositionBack) {
            position = @"environment";
        } else if (device.position == AVCaptureDevicePositionFront) {
            position = @"user";
        }
        NSString *label = @"Unknown video device";
        if (device.localizedName != nil) {
            label = device.localizedName;
        }
        [devices addObject:@{
                             @"facing": position,
                             @"deviceId": device.uniqueID,
                             @"groupId": @"",
                             @"label": label,
                             @"kind": DEVICE_KIND_VIDEO_INPUT,
                             }];
    }
}

- (void)fillAudioDevices:(NSMutableArray *)devices {
    NSString * wiredOrEarpieceLabel = self.hasWiredHeadset ? @"Wired headset" : @"Phone earpiece";
    [devices addObject:@{
                         @"deviceId": WIRED_OR_EARPIECE_DEVICE_ID,
                         @"groupId": @"",
                         @"label": wiredOrEarpieceLabel,
                         @"kind": DEVICE_KIND_AUDIO,
                         }];
    
    [devices addObject:@{
                         @"deviceId": SPEAKERPHONE_DEVICE_ID,
                         @"groupId": @"",
                         @"label": @"Speakerphone",
                         @"kind": DEVICE_KIND_AUDIO,
                         }];
    
    if(self.hasBluetoothDevice && !self.hasWiredHeadset){
        [devices addObject:@{
                         @"deviceId": BLUETOOTH_DEVICE_ID,
                         @"groupId": @"",
                         @"label": @"Bluetooth",
                         @"kind": DEVICE_KIND_AUDIO,
                         }];
    }
}

- (BOOL)hasWiredHeadset {
    AVAudioSession *audioSession = AVAudioSession.sharedInstance;
    NSArray<AVAudioSessionPortDescription *> *availableInputs = [audioSession availableInputs];
    for (AVAudioSessionPortDescription *device in availableInputs) {
        NSString* portType = device.portType;
        if([portType isEqualToString:AVAudioSessionPortHeadphones] ||
           [portType isEqualToString:AVAudioSessionPortHeadsetMic] ){
            return true;
        }
    }
    return false;
}
    
- (BOOL)hasBluetoothDevice {
    AVAudioSession *audioSession = AVAudioSession.sharedInstance;

    NSArray<AVAudioSessionPortDescription *> *availableInputs = [audioSession availableInputs];
    for (AVAudioSessionPortDescription *device in availableInputs) {
        if([self isBluetoothDevice:[device portType]]){
            return true;
        }
    }

    NSArray<AVAudioSessionPortDescription *> *outputs = [[audioSession currentRoute] outputs];
    for (AVAudioSessionPortDescription *device in outputs) {
        if([self isBluetoothDevice:[device portType]]){
            return true;
        }
    }
    return false;
}

- (BOOL)isBluetoothDevice:(NSString*)portType {
    BOOL isBluetooth;
    isBluetooth = ([portType isEqualToString:AVAudioSessionPortBluetoothA2DP] ||
                   [portType isEqualToString:AVAudioSessionPortBluetoothHFP]);
    if (@available(iOS 7.0, *)) {
        isBluetooth |= [portType isEqualToString:AVAudioSessionPortBluetoothLE];
    }
    return isBluetooth;
}

- (BOOL)isBuiltInSpeaker:(NSString*)portType {
    return [portType isEqualToString:AVAudioSessionPortBuiltInSpeaker];
}

- (BOOL)isBuiltInEarpieceHeadset:(NSString*)portType {
    return ([portType isEqualToString:AVAudioSessionPortBuiltInReceiver] ||
            [portType isEqualToString:AVAudioSessionPortHeadphones] ||
            [portType isEqualToString:AVAudioSessionPortHeadsetMic] );
}

- (BOOL)isBuiltInMic:(NSString*)portType {
    return ([portType isEqualToString:AVAudioSessionPortBuiltInMic]);
}


RCT_EXPORT_METHOD(getAudioDevice: (RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[Daily] getAudioDevice");
    AVAudioSession *audioSession = AVAudioSession.sharedInstance;
    NSArray<AVAudioSessionPortDescription *> *currentRoutes = [[audioSession currentRoute] outputs];
    if([currentRoutes count] > 0){
        NSString* currentPortType = [currentRoutes[0] portType];
        NSLog(@"[Daily] currentPortType: %@", currentPortType);
        if([self isBluetoothDevice:currentPortType]){
            return resolve(BLUETOOTH_DEVICE_ID);
        } else if([self isBuiltInSpeaker:currentPortType]){
            return resolve(SPEAKERPHONE_DEVICE_ID);
        } else if([self isBuiltInEarpieceHeadset:currentPortType]){
            return resolve(WIRED_OR_EARPIECE_DEVICE_ID);
        }
    }
    return resolve(SPEAKERPHONE_DEVICE_ID);
}

// Some reference links explaining how the audio from IOs works and sample code
// https://stephen-chen.medium.com/how-to-add-audio-device-action-sheet-to-your-ios-app-e6bc401ccdbc
// https://github.com/xialin/AudioSessionManager/blob/master/AudioSessionManager.m
RCT_EXPORT_METHOD(setAudioDevice:(NSString*)deviceId) {
    NSLog(@"[Daily] setAudioDevice: %@", deviceId);
    
    [self setAudioMode: AUDIO_MODE_USER_SPECIFIED_ROUTE];
    self.userSelectedDevice = deviceId;
    
    // Ducking other apps' audio implicitly enables allowing mixing audio with
    // other apps, which allows this app to stay alive in the backgrounnd during
    // a call (assuming it has the voip background mode set).
    // After iOS 16, we must also always keep the bluetooth option here, otherwise
    // we are not able to see the bluetooth devices on the list
    AVAudioSessionCategoryOptions categoryOptions = (AVAudioSessionCategoryOptionAllowBluetooth |
                                                     AVAudioSessionCategoryOptionMixWithOthers);
    NSString *mode = AVAudioSessionModeVoiceChat;

    // Earpiece: is default route for a call.
    // Speaker: the speaker is the default output audio for like music, video, ring tone.
    // Bluetooth: whenever a bluetooth device connected, the bluetooth device will become the default audio route.
    // Headphones: whenever any headphones plugged in, it becomes the default audio route even there is also bluetooth device.
    //  And it overwrites the handset(iPhone) option, which means you cannot change to the earpiece, bluetooth.
    if([SPEAKERPHONE_DEVICE_ID isEqualToString: deviceId]){
        NSLog(@"[Daily] configuring output to SPEAKER");
        categoryOptions |= AVAudioSessionCategoryOptionDefaultToSpeaker;
        mode = AVAudioSessionModeVideoChat;
    }

    AVAudioSession *audioSession = AVAudioSession.sharedInstance;
    [self configureAudioSession:audioSession audioMode:mode categoryOptions:categoryOptions];

    // Force to speaker. We only need to do that the cases a wired headset is connected, but we still want to force to speaker
    if([SPEAKERPHONE_DEVICE_ID isEqualToString: deviceId]){
        [audioSession overrideOutputAudioPort: AVAudioSessionPortOverrideSpeaker error: nil];
    } else if([WIRED_OR_EARPIECE_DEVICE_ID isEqualToString: deviceId]){
        [audioSession overrideOutputAudioPort: AVAudioSessionPortOverrideNone error: nil];
        NSArray<AVAudioSessionPortDescription *> *availableInputs = [audioSession availableInputs];
        for (AVAudioSessionPortDescription *device in availableInputs) {
            if([self isBuiltInMic:[device portType]]){
                NSLog(@"[Daily] forcing preferred input to built in device");
                [audioSession setPreferredInput:device error:nil];
                return;
            }
        }
    }
}

- (void)configureAudioSession:(nonnull AVAudioSession *)audioSession
              audioMode:(nonnull NSString *)mode
              categoryOptions: (AVAudioSessionCategoryOptions) categoryOptions
{
    // We need to set the mode before set the category, because when setting the node It can automatically change the categories.
    // This way we can enforce the categories that we want later.
    [self audioSessionSetMode:mode toSession:audioSession];
    [self audioSessionSetCategory:AVAudioSessionCategoryPlayAndRecord toSession:audioSession options:categoryOptions];
}

- (void)audioSessionSetCategory:(NSString *)audioCategory
                      toSession:(AVAudioSession *)audioSession
                        options:(AVAudioSessionCategoryOptions)options
{
  @try {
    [audioSession setCategory:audioCategory
                  withOptions:options
                        error:nil];
    NSLog(@"[Daily] audioSession.setCategory: %@, withOptions: %lu success", audioCategory, (unsigned long)options);
  } @catch (NSException *e) {
    NSLog(@"[Daily] audioSession.setCategory: %@, withOptions: %lu fail: %@", audioCategory, (unsigned long)options, e.reason);
  }
}

- (void)audioSessionSetMode:(NSString *)audioMode
                  toSession:(AVAudioSession *)audioSession
{
  @try {
    [audioSession setMode:audioMode error:nil];
    NSLog(@"[Daily] audioSession.setMode(%@) success", audioMode);
  } @catch (NSException *e) {
    NSLog(@"[Daily] audioSession.setMode(%@) fail: %@", audioMode, e.reason);
  }
}

- (void)devicesChanged:(NSNotification *)notification {
    // Possible change reasons: AVAudioSessionRouteChangeReasonOldDeviceUnavailable AVAudioSessionRouteChangeReasonNewDeviceAvailable
    NSInteger changeReason = [[notification.userInfo objectForKey:AVAudioSessionRouteChangeReasonKey] integerValue];
    NSLog(@"[Daily] devicesChanged %zd", changeReason);
    
    // AVAudioSessionRouteDescription *oldRoute = [notification.userInfo objectForKey:AVAudioSessionRouteChangePreviousRouteKey];
    // NSString *oldOutput = [[oldRoute.outputs objectAtIndex:0] portType];
    // AVAudioSessionRouteDescription *newRoute = [audioSession currentRoute];
    // NSString *newOutput = [[newRoute.outputs objectAtIndex:0] portType];
    
    [self sendEventWithName:kEventMediaDevicesOnDeviceChange body:@{}];
}

RCT_EXPORT_METHOD(startMediaDevicesEventMonitor) {
    NSLog(@"[Daily] startMediaDevicesEventMonitor");
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(devicesChanged:) name:AVAudioSessionRouteChangeNotification object:nil];
}

@end
