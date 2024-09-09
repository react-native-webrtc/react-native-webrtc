//
//  I420Converter.h
//  VideoSampleCaptureRender
//
//  Adapted from: https://github.com/twilio/video-ios-affectiva/blob/ed2e864324c40ad25e5a06cc2b05298b03caed09/EmoCall/I420Converter.h
//  Created by Boisy Pitre on 5/21/16.
//  Copyright © 2016 Twilio. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <Accelerate/Accelerate.h>
#import <WebRTC/WebRTC.h>

@interface I420Converter : NSObject

- (vImage_Error)prepareForAccelerateConversion;
- (void)unprepareForAccelerateConversion;
- (CVPixelBufferRef)convertI420ToPixelBuffer:(RTCI420Buffer *)buffer;
- (void)createPixelBufferPoolWithWidth:(size_t)width height:(size_t)height;

@end
