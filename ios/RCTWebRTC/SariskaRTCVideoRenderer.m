//
//  SariskaVideoRederer.m
//  RCTWebRTC
//
//  Created by Dipak Sisodiya on 13/08/22.
//

#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import <CoreGraphics/CGImage.h>
#import <WebRTC/RTCVideoFrameBuffer.h>
#import <objc/runtime.h>
#import <WebRTC/RTCVideoRenderer.h>
#import <WebRTC/RTCVideoFrame.h>
#import "SariskaRTCVideoRenderer.h"


@implementation SariskaRTCVideoRenderer{
    CGSize _renderSize;
    CGSize _frameSize;
    CVPixelBufferRef _pixelBufferRef;
    RTCVideoRotation _rotation;
}

-(instancetype)initWithSize:(CGSize)renderSize{
    self = [super init];
    if (self){
            _renderSize = renderSize;
            _pixelBufferRef = nil;
            _rotation  = -1;
        }
    return self;
}

-(void)dealloc {
    if(_pixelBufferRef){
        CVBufferRelease(_pixelBufferRef);
    }
}

- (CVPixelBufferRef)copyPixelBuffer {
    if(_pixelBufferRef != nil){
        CVBufferRetain(_pixelBufferRef);
        return _pixelBufferRef;
    }
    return nil;
}

-(void)dispose{
    // Do nothing for now
}

- (void)setVideoTrack:(RTCVideoTrack *)videoTrack {
   // Do nothing for now
}

#pragma mark - RTCVideoRenderer methods

-(void) renderFrame:(RTCVideoFrame *)frame{
    NSLog(@"We got the frame bro");
}


- (void)setSize:(CGSize)size {
    // Do nothing for now
}

@end

