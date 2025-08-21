#if !TARGET_OS_TV

#include <mach/mach_time.h>

#import <ReplayKit/ReplayKit.h>
#import <WebRTC/RTCVideoFrameBuffer.h>

#import "ImageCapturer.h"

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

void maybeLoadBuffer(UIImage *image, SuccessBlock _Nullable success, FailureBlock _Nullable failure) {
    RTCCVPixelBuffer *buffer = rtcPixelBufferFromUIImage(image);
    if (buffer != nil) {
        dispatch_async(dispatch_get_main_queue(), ^{ if (success) success(buffer); });
    } else {
        dispatch_async(dispatch_get_main_queue(), ^{ if (failure) failure(@"could not make buffer from image"); });
    }
}

void imageFromAsset(NSString *asset, SuccessBlock _Nullable success, FailureBlock _Nullable failure) {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        UIImage *localImage = [UIImage imageNamed:asset];
        if (localImage != nil) {
            maybeLoadBuffer(localImage, success, failure);
            return;
        }
        NSURL *url = [NSURL URLWithString: asset];
        if (url == nil) {
            dispatch_async(dispatch_get_main_queue(), ^{ if (failure) failure(@"invalid url"); });
            return;
        }
        SDWebImageManager *imageManager = [SDWebImageManager sharedManager];
        [imageManager loadImageWithURL:url
                        options:0
                        progress:nil
                        completed:^(UIImage * _Nullable image, NSData * _Nullable data, NSError * _Nullable error, SDImageCacheType cacheType, BOOL finished, NSURL * _Nullable imageURL) {
                          if (image && finished) {
                            maybeLoadBuffer(image, success, failure);
                          } else {
                            dispatch_async(dispatch_get_main_queue(), ^{ if (failure) failure(@"failed to load image"); });
                          }
        }];
    });
}

@implementation ImageCapturer {
    mach_timebase_info_data_t _timebaseInfo;
    int64_t _startTimeStampNs;
    NSTimer *_timer;
    RTCCVPixelBuffer *_rtcPixelBuffer;
    BOOL _bursting;
    BOOL _active;
}

- (instancetype)initWithDelegate:(__weak id<RTCVideoCapturerDelegate>)delegate image:(RTCCVPixelBuffer *)image {
    self = [super initWithDelegate:delegate];
    if (self) {
        mach_timebase_info(&_timebaseInfo);
    }
    _rtcPixelBuffer = image;
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
