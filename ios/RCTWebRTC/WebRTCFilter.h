//
//  WebRTCFilterDelegate.h
//  Pods
//
//  Created by Dipesh Dulal on 20/04/2021.
//

#ifndef WebRTCFilterDelegate_h
#define WebRTCFilterDelegate_h

#import <Foundation/Foundation.h>
#import <WebRTC/RTCCameraVideoCapturer.h>
#import <React/RCTBridgeModule.h>

@interface WebRTCFilter: RTCCameraVideoCapturer
@property (retain, nonatomic) CIContext* context;
@end

@interface RCTWebRTCFilterModule: NSObject<RCTBridgeModule>

- (BOOL)isFilterEnabled;

@end
#endif /* WebRTCFilterDelegate_h */
