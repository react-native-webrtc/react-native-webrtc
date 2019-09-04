
#import "VideoCaptureController.h"

#import <React/RCTLog.h>


@implementation VideoCaptureController {
    RTCCameraVideoCapturer *_capturer;
    NSString *_deviceId;
    BOOL _usingFrontCamera;
    int _width;
    int _height;
    int _fps;
}

-(instancetype)initWithCapturer:(RTCCameraVideoCapturer *)capturer
                 andConstraints:(NSDictionary *)constraints {
    self = [super init];
    if (self) {
        _capturer = capturer;

        // Default to the front camera.
        _usingFrontCamera = YES;

        _deviceId = constraints[@"deviceId"];
        _width = [constraints[@"width"] intValue];
        _height = [constraints[@"height"] intValue];
        _fps = [constraints[@"frameRate"] intValue];

        id facingMode = constraints[@"facingMode"];

        if (facingMode && [facingMode isKindOfClass:[NSString class]]) {
            AVCaptureDevicePosition position;
            if ([facingMode isEqualToString:@"environment"]) {
                position = AVCaptureDevicePositionBack;
            } else if ([facingMode isEqualToString:@"user"]) {
                position = AVCaptureDevicePositionFront;
            } else {
                // If the specified facingMode value is not supported, fall back
                // to the front camera.
                position = AVCaptureDevicePositionFront;
            }

            _usingFrontCamera = position == AVCaptureDevicePositionFront;
        }
    }

    return self;
}

-(void)startCapture {
    AVCaptureDevice *device;
    if (_deviceId) {
        device = [AVCaptureDevice deviceWithUniqueID:_deviceId];
    }
    if (!device) {
        AVCaptureDevicePosition position
            = _usingFrontCamera
                ? AVCaptureDevicePositionFront
                : AVCaptureDevicePositionBack;
        device = [self findDeviceForPosition:position];
    }

    AVCaptureDeviceFormat *format
        = [self selectFormatForDevice:device
                      withTargetWidth:_width
                     withTargetHeight:_height];
    if (!format) {
        RCTLogWarn(@"[VideoCaptureController] No valid formats for device %@", device);

        return;
    }

    [_capturer startCaptureWithDevice:device format:format fps:_fps];

    RCTLog(@"[VideoCaptureController] Capture started");
}

-(void)stopCapture {
    [_capturer stopCapture];

    RCTLog(@"[VideoCaptureController] Capture stopped");
}

-(void)switchCamera {
    _usingFrontCamera = !_usingFrontCamera;

    [self startCapture];
}

#pragma mark Private

- (AVCaptureDevice *)findDeviceForPosition:(AVCaptureDevicePosition)position {
    NSArray<AVCaptureDevice *> *captureDevices = [RTCCameraVideoCapturer captureDevices];
    for (AVCaptureDevice *device in captureDevices) {
        if (device.position == position) {
            return device;
        }
    }

    return captureDevices[0];
}

- (AVCaptureDeviceFormat *)selectFormatForDevice:(AVCaptureDevice *)device
                                 withTargetWidth:(int)targetWidth
                                withTargetHeight:(int)targetHeight {
    NSArray<AVCaptureDeviceFormat *> *formats =
    [RTCCameraVideoCapturer supportedFormatsForDevice:device];
    AVCaptureDeviceFormat *selectedFormat = nil;
    int currentDiff = INT_MAX;

    for (AVCaptureDeviceFormat *format in formats) {
        CMVideoDimensions dimension = CMVideoFormatDescriptionGetDimensions(format.formatDescription);
        FourCharCode pixelFormat = CMFormatDescriptionGetMediaSubType(format.formatDescription);
        int diff = abs(targetWidth - dimension.width) + abs(targetHeight - dimension.height);
        if (diff < currentDiff) {
            selectedFormat = format;
            currentDiff = diff;
        } else if (diff == currentDiff && pixelFormat == [_capturer preferredOutputPixelFormat]) {
            selectedFormat = format;
        }
    }

    return selectedFormat;
}

@end
