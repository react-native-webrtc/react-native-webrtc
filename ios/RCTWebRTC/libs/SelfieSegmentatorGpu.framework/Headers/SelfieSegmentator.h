//
//  FaceDetectionViewController.h
//  Mediapipe
//
//  Created by sidal on 6/13/22.
//

#import <Foundation/Foundation.h>
#import <CoreVideo/CoreVideo.h>

@class SelfieSegmentator;

@protocol SelfieSegmentatorDelegate <NSObject>

// Will be invoked after selfie segmentation processing is complete
- (void)selfieSegmentator: (SelfieSegmentator*)selfieSegmentator didOutputPixelBuffer: (CVPixelBufferRef)pixelBuffer;
@end


@interface SelfieSegmentator : NSObject
- (instancetype)init;
- (void)startGraph;
- (void)processVideoFrame:(CVPixelBufferRef)imageBuffer;
@property (weak, nonatomic) id <SelfieSegmentatorDelegate> delegate;
@end
