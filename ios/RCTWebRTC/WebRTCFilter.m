//
//  WebRTCFilterDelegate.m
//  react-native-webrtc
//
//  Created by Dipesh Dulal on 20/04/2021.
//

#import "WebRTCFilter.h"


BOOL WEBRTC_FILTER_ENABLED = NO;
@implementation RCTWebRTCFilterModule

-(BOOL) isFilterEnabled {
    return WEBRTC_FILTER_ENABLED;
}

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(setFilterEnabled:(BOOL *)enabled){
    WEBRTC_FILTER_ENABLED = enabled;
}

@end

@implementation WebRTCFilter

- (CVPixelBufferRef)getNewPixelRef:(CMSampleBufferRef)sampleBuffer{
    CVImageBufferRef videoFrameBuffer = CMSampleBufferGetImageBuffer(sampleBuffer);
    
    if (WEBRTC_FILTER_ENABLED) {
        CIImage *ciInput = [[CIImage alloc] initWithCVImageBuffer: videoFrameBuffer];
        CIImage *processed = [self highPassSmoothing:ciInput];
        
        if(_context == nil){
            _context = [CIContext contextWithOptions:nil];
        }
        
        [_context clearCaches];
        [_context render:processed toCVPixelBuffer:videoFrameBuffer];
    }
    
    return videoFrameBuffer;
}

- (CIImage*) grayscaleImage: (CIImage*) inputImage {
    CIFilter *grayFilter = [CIFilter filterWithName:@"CIColorControls"];
    [grayFilter setValue:@0.0 forKey:kCIInputBrightnessKey];
    [grayFilter setValue:@0.0 forKey:kCIInputSaturationKey];
    [grayFilter setValue:@1.1 forKey:kCIInputContrastKey];
    [grayFilter setValue:inputImage forKey:kCIInputImageKey];
    return grayFilter.outputImage;
}

- (CIImage*) highPassSmoothing: (CIImage*) inputImage {
    CIFilter *smoothFilter = [CIFilter filterWithName:@"YUCIHighPassSkinSmoothing"];
    [smoothFilter setValue:inputImage forKey:kCIInputImageKey];
    [smoothFilter setValue:@0 forKey:@"inputAmount"];
    return smoothFilter.outputImage;
}

@end
