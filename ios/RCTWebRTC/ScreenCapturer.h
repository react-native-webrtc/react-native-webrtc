//
//  ScreenCapturer.h
//  RCTWebRTC
//
//  Created by Alex-Dan Bumbu on 06/01/2021.
//

#import <AVFoundation/AVFoundation.h>
#import <WebRTC/RTCVideoCapturer.h>

NS_ASSUME_NONNULL_BEGIN

@class SocketConnection;

@interface ScreenCapturer: RTCVideoCapturer

- (instancetype)initWithDelegate:(__weak id<RTCVideoCapturerDelegate>)delegate;
- (void)startCaptureWithConnection:(nonnull SocketConnection *)connection;
- (void)stopCapture;

@end

NS_ASSUME_NONNULL_END
