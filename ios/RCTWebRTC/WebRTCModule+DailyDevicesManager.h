//
//  WebRTCModule+DevicesManager.m
//  react-native-webrtc
//
//  Created by Filipi Fuchter on 01/04/22.
//

#import "WebRTCModule.h"

// audio devices routes
static NSString * _Nonnull const WIRED_OR_EARPIECE_DEVICE_ID = @"WIRED_OR_EARPIECE";
static NSString * _Nonnull const SPEAKERPHONE_DEVICE_ID = @"SPEAKERPHONE";
static NSString * _Nonnull const BLUETOOTH_DEVICE_ID = @"BLUETOOTH";

@interface WebRTCModule (DailyDevicesManager)

@property (nonatomic, strong) NSString *userSelectedDevice;

- (BOOL)hasBluetoothDevice;
- (BOOL)hasWiredHeadset;
- (void)setAudioDevice:(nonnull NSString*)deviceId;
- (void)configureAudioSession:(nonnull AVAudioSession *)audioSession
              audioMode:(nonnull NSString *)mode
              categoryOptions: (AVAudioSessionCategoryOptions) categoryOptions;

@end
