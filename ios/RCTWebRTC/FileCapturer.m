#if !TARGET_OS_TV

#include <mach/mach_time.h>

#import <ReplayKit/ReplayKit.h>
#import <WebRTC/RTCCVPixelBuffer.h>
#import <WebRTC/RTCVideoFrameBuffer.h>

#import "FileCapturer.h"

#import <SDWebImage/SDWebImage.h>

static const NSTimeInterval kBurstInterval = 0.033;
static const NSTimeInterval kSteadyInterval = 1.5;
static const int64_t kBurstDuration = 3LL * NSEC_PER_SEC;

RTCCVPixelBuffer *rtcPixelBufferFromUIImage(UIImage *image) {
  if (!image) {
    return nil;
  }

  CGSize imageSize = image.size;
  
  NSDictionary *options = @{
      (NSString *)kCVPixelBufferCGImageCompatibilityKey: @YES,
      (NSString *)kCVPixelBufferCGBitmapContextCompatibilityKey: @YES,
      (NSString *)kCVPixelBufferIOSurfacePropertiesKey: @{}
  };
  
  CVPixelBufferRef pixelBuffer = NULL;
  CVReturn status = CVPixelBufferCreate(kCFAllocatorDefault,
                                        (int)imageSize.width,
                                        (int)imageSize.height,
                                        kCVPixelFormatType_32BGRA,
                                        (__bridge CFDictionaryRef)options,
                                        &pixelBuffer);
  
  if (status != kCVReturnSuccess) {
      return nil;
  }
  
  CVPixelBufferLockBaseAddress(pixelBuffer, 0);
  
  void *baseAddress = CVPixelBufferGetBaseAddress(pixelBuffer);
  size_t width = CVPixelBufferGetWidth(pixelBuffer);
  size_t height = CVPixelBufferGetHeight(pixelBuffer);
  size_t bytesPerRow = CVPixelBufferGetBytesPerRow(pixelBuffer);
    
  CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
  CGBitmapInfo bitmapInfo = kCGBitmapByteOrder32Little | kCGImageAlphaPremultipliedFirst;
  
  CGContextRef context = CGBitmapContextCreate(baseAddress,
                                                width,
                                                height,
                                                8,
                                                bytesPerRow,
                                                colorSpace,
                                                bitmapInfo);
  
  if (!context) {
      CGColorSpaceRelease(colorSpace);
      CVPixelBufferUnlockBaseAddress(pixelBuffer, 0);
      CVPixelBufferRelease(pixelBuffer);
      return nil;
  }
  
  CGContextSetInterpolationQuality(context, kCGInterpolationHigh);
  
  CGRect drawRect = CGRectMake(0, 0, width, height);
  CGContextDrawImage(context, drawRect, image.CGImage);
  
  CGColorSpaceRelease(colorSpace);
  CGContextRelease(context);
  
  CVPixelBufferUnlockBaseAddress(pixelBuffer, 0);

  RTCCVPixelBuffer *rtcPixelBuffer = [[RTCCVPixelBuffer alloc] initWithPixelBuffer:pixelBuffer];
  CVPixelBufferRelease(pixelBuffer);

  return rtcPixelBuffer;
}

@implementation FileCapturer {
    mach_timebase_info_data_t _timebaseInfo;
    int64_t _startTimeStampNs;
    NSTimer *_timer;
    RTCCVPixelBuffer *_rtcPixelBuffer;
    SDWebImageCombinedOperation *_imageOperation;
    BOOL _bursting;
    BOOL _active;
}

- (instancetype)initWithDelegate:(__weak id<RTCVideoCapturerDelegate>)delegate asset:(NSString *)asset {
    self = [super initWithDelegate:delegate];
    if (self) {
        mach_timebase_info(&_timebaseInfo);
    }

    UIImage *localImage = [UIImage imageNamed:asset];
    if (localImage != nil) {
        _rtcPixelBuffer = rtcPixelBufferFromUIImage(localImage);
    } else {
      NSURL *url = [NSURL URLWithString: asset];
      if (url != nil) {
        SDWebImageManager *imageManager = [SDWebImageManager sharedManager];
        __weak __typeof__(self) weakSelf = self;
        _imageOperation = [imageManager loadImageWithURL:url
                        options:0
                        progress:nil
                        completed:^(UIImage * _Nullable image, NSData * _Nullable data, NSError * _Nullable error, SDImageCacheType cacheType, BOOL finished, NSURL * _Nullable imageURL) {
                          __strong __typeof__(self) strongSelf = weakSelf;
                          if (!strongSelf) {
                            return;
                          }
                          if (image && finished) {
                              strongSelf->_rtcPixelBuffer = rtcPixelBufferFromUIImage(image);
                              if (strongSelf->_active) {
                                [strongSelf startRenderLoop];
                              }
                          }
        }];
      }
    }

    return self;
}

- (void)startCapture {
    _active = YES;
    if (_rtcPixelBuffer != nil) {
        [self startRenderLoop];
    }
}

- (void)stopCapture {
    _active = NO;
    [_timer invalidate];
    _timer = nil;
}

- (void)dealloc {
  _active = NO;
  [_timer invalidate];
  _timer = nil;
  [_imageOperation cancel];
  _imageOperation = nil;
}

- (void)startTimerWithInterval:(NSTimeInterval)interval {
    [_timer invalidate];
    _timer = [NSTimer scheduledTimerWithTimeInterval:interval
                                              target:self
                                            selector:@selector(render)
                                            userInfo:nil
                                             repeats:YES];
    [[NSRunLoop mainRunLoop] addTimer:_timer forMode:NSRunLoopCommonModes];
}

- (void)startRenderLoop {
    __weak __typeof__(self) weakSelf = self;
    dispatch_async(dispatch_get_main_queue(), ^{
        __strong __typeof__(self) strongSelf = weakSelf;
        if (!strongSelf || !strongSelf->_active) {
            return;
        }
        [strongSelf startTimerWithInterval:kBurstInterval];
    });
    self->_startTimeStampNs = -1;
    self->_bursting = YES;
    [self render];
}

- (void)render {
    if (!self || !_active || !_rtcPixelBuffer) {
        return;
    }

    int64_t currentTime = mach_absolute_time();
    int64_t currentTimeStampNs = currentTime * _timebaseInfo.numer / _timebaseInfo.denom;
    
    if (_startTimeStampNs < 0) {
        _startTimeStampNs = currentTimeStampNs;
    }
    
    int64_t frameTimeStampNs = currentTimeStampNs - _startTimeStampNs;

    if (_bursting && frameTimeStampNs >= kBurstDuration) {
        _bursting = NO;
        [self startTimerWithInterval:kSteadyInterval];
    }

    RTCVideoFrame *videoFrame = [[RTCVideoFrame alloc] initWithBuffer:_rtcPixelBuffer
                                                             rotation:RTCVideoRotation_0
                                                          timeStampNs:frameTimeStampNs];
    
    [self.delegate capturer:self didCaptureVideoFrame:videoFrame];    
}

@end

#endif
