/*
 *  Copyright 2016 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

#import <UIKit/UIKit.h>

typedef NS_ENUM(NSInteger, RTCDeviceType) {
  RTCDeviceTypeUnknown,
  RTCDeviceTypeIPhone1G,
  RTCDeviceTypeIPhone3G,
  RTCDeviceTypeIPhone3GS,
  RTCDeviceTypeIPhone4,
  RTCDeviceTypeIPhone4Verizon,
  RTCDeviceTypeIPhone4S,
  RTCDeviceTypeIPhone5GSM,
  RTCDeviceTypeIPhone5GSM_CDMA,
  RTCDeviceTypeIPhone5CGSM,
  RTCDeviceTypeIPhone5CGSM_CDMA,
  RTCDeviceTypeIPhone5SGSM,
  RTCDeviceTypeIPhone5SGSM_CDMA,
  RTCDeviceTypeIPhone6Plus,
  RTCDeviceTypeIPhone6,
  RTCDeviceTypeIPhone6S,
  RTCDeviceTypeIPhone6SPlus,
  RTCDeviceTypeIPodTouch1G,
  RTCDeviceTypeIPodTouch2G,
  RTCDeviceTypeIPodTouch3G,
  RTCDeviceTypeIPodTouch4G,
  RTCDeviceTypeIPodTouch5G,
  RTCDeviceTypeIPad,
  RTCDeviceTypeIPad2Wifi,
  RTCDeviceTypeIPad2GSM,
  RTCDeviceTypeIPad2CDMA,
  RTCDeviceTypeIPad2Wifi2,
  RTCDeviceTypeIPadMiniWifi,
  RTCDeviceTypeIPadMiniGSM,
  RTCDeviceTypeIPadMiniGSM_CDMA,
  RTCDeviceTypeIPad3Wifi,
  RTCDeviceTypeIPad3GSM_CDMA,
  RTCDeviceTypeIPad3GSM,
  RTCDeviceTypeIPad4Wifi,
  RTCDeviceTypeIPad4GSM,
  RTCDeviceTypeIPad4GSM_CDMA,
  RTCDeviceTypeIPadAirWifi,
  RTCDeviceTypeIPadAirCellular,
  RTCDeviceTypeIPadMini2GWifi,
  RTCDeviceTypeIPadMini2GCellular,
  RTCDeviceTypeSimulatori386,
  RTCDeviceTypeSimulatorx86_64,
};

@interface UIDevice (RTCDevice)

+ (RTCDeviceType)deviceType;
+ (NSString *)stringForDeviceType:(RTCDeviceType)deviceType;
+ (BOOL)isIOS9OrLater;
+ (BOOL)isIOS11OrLater;

@end
