//
//  WebRTCFilterDelegate.m
//  react-native-webrtc
//
//  Created by Dipesh Dulal on 20/04/2021.
//

#import "WebRTCFilter.h"

@implementation WebRTCFilter

- (CVPixelBufferRef)getNewPixelRef:(CMSampleBufferRef)sampleBuffer{
    CVImageBufferRef videoFrameBuffer = CMSampleBufferGetImageBuffer(sampleBuffer);
    CIImage *ciInput = [[CIImage alloc] initWithCVImageBuffer: videoFrameBuffer];
    CIImage *processed = [self grayscaleImage:ciInput];
    
    if(_context == nil){
        _context = [CIContext contextWithOptions:nil];
    }
    
    [_context clearCaches];
    [_context render:processed toCVPixelBuffer:videoFrameBuffer];
    
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

@end
