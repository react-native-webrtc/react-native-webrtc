//
//  WebRTCModule.m
//
//  Created by one on 2015/9/24.
//  Copyright © 2015 One. All rights reserved.
//

#import <UIKit/UIKit.h>

#import <React/RCTBridge.h>
#import <React/RCTEventDispatcher.h>
#import <React/RCTUtils.h>

#import <WebRTC/RTCDefaultVideoDecoderFactory.h>
#import <WebRTC/RTCDefaultVideoEncoderFactory.h>
#import <WebRTC/RTCFieldTrials.h>

#import "WebRTCModule.h"
#import "WebRTCModule+RTCPeerConnection.h"

@interface WebRTCModule ()

@property(nonatomic, strong) dispatch_queue_t workerQueue;

@end

@implementation WebRTCModule

@synthesize bridge = _bridge;

+ (BOOL)requiresMainQueueSetup
{
    return NO;
}

- (void)dealloc
{
  [_localTracks removeAllObjects];
  _localTracks = nil;
  [_localStreams removeAllObjects];
  _localStreams = nil;

  for (NSNumber *peerConnectionId in _peerConnections) {
    RTCPeerConnection *peerConnection = _peerConnections[peerConnectionId];
    peerConnection.delegate = nil;
    [peerConnection close];
  }
  [_peerConnections removeAllObjects];

  _peerConnectionFactory = nil;
}

- (instancetype)init
{
    return [self initWithEncoderFactory:nil decoderFactory:nil];
}

- (instancetype)initWithEncoderFactory:(nullable id<RTCVideoEncoderFactory>)encoderFactory
                        decoderFactory:(nullable id<RTCVideoDecoderFactory>)decoderFactory
{
  self = [super init];
  if (self) {
    // Initialize field trial for solving audio issues after hold when using CallKit.
    // See: https://bugs.chromium.org/p/webrtc/issues/detail?id=8126#c35
    NSDictionary *fieldTrials = @{ @"WebRTC-Audio-iOS-Holding" : kRTCFieldTrialEnabledValue };
    RTCInitFieldTrialDictionary(fieldTrials);

    if (encoderFactory == nil) {
      encoderFactory = [[RTCDefaultVideoEncoderFactory alloc] init];
    }
    if (decoderFactory == nil) {
      decoderFactory = [[RTCDefaultVideoDecoderFactory alloc] init];
    }
    _peerConnectionFactory
      = [[RTCPeerConnectionFactory alloc] initWithEncoderFactory:encoderFactory
                                                  decoderFactory:decoderFactory];

    _peerConnections = [NSMutableDictionary new];
    _localStreams = [NSMutableDictionary new];
    _localTracks = [NSMutableDictionary new];

    dispatch_queue_attr_t attributes =
    dispatch_queue_attr_make_with_qos_class(DISPATCH_QUEUE_SERIAL,
                                            QOS_CLASS_USER_INITIATED, -1);
    _workerQueue = dispatch_queue_create("WebRTCModule.queue", attributes);
  }
  return self;
}

- (RTCMediaStream*)streamForReactTag:(NSString*)reactTag
{
  RTCMediaStream *stream = _localStreams[reactTag];
  if (!stream) {
    for (NSNumber *peerConnectionId in _peerConnections) {
      RTCPeerConnection *peerConnection = _peerConnections[peerConnectionId];
      stream = peerConnection.remoteStreams[reactTag];
      if (stream) {
        break;
      }
    }
  }
  return stream;
}

/**
 * Parses the constraint keys and values of a specific JavaScript object into
 * a specific <tt>NSMutableDictionary</tt> in a format suitable for the
 * initialization of a <tt>RTCMediaConstraints</tt> instance.
 *
 * @param src The JavaScript object which defines constraint keys and values and
 * which is to be parsed into the specified <tt>dst</tt>.
 * @param dst The <tt>NSMutableDictionary</tt> into which the constraint keys
 * and values defined by <tt>src</tt> are to be written in a format suitable for
 * the initialization of a <tt>RTCMediaConstraints</tt> instance.
 */
- (void)parseJavaScriptConstraints:(NSDictionary *)src
             intoWebRTCConstraints:(NSMutableDictionary<NSString *, NSString *> *)dst {

  for (id srcKey in src) {
    id srcValue = src[srcKey];
    NSString *dstValue;

    if ([srcValue isKindOfClass:[NSNumber class]]) {
      dstValue = [srcValue boolValue] ? @"true" : @"false";
    } else {
      dstValue = [srcValue description];
    }
    NSString *dstKey = [srcKey description];
    dst[dstKey] = dstValue;
  }
}

/**
 * Parses a JavaScript object into a new <tt>RTCMediaConstraints</tt> instance.
 *
 * @param constraints The JavaScript object to parse into a new
 * <tt>RTCMediaConstraints</tt> instance.
 * @returns A new <tt>RTCMediaConstraints</tt> instance initialized with the
 * mandatory and optional constraint keys and values specified by
 * <tt>constraints</tt>.
 */
- (RTCMediaConstraints *)parseMediaConstraints:(NSDictionary *)constraints {
  NSMutableDictionary<NSString *, NSString *> *mandatory = [NSMutableDictionary new];
  if (constraints != nil) {
    [self parseJavaScriptConstraints:constraints intoWebRTCConstraints:mandatory];
  }

  NSMutableDictionary<NSString *, NSString *> *optional
    = [NSMutableDictionary new];

  return [[RTCMediaConstraints alloc] initWithMandatoryConstraints:mandatory
                                               optionalConstraints:optional];
}


RCT_EXPORT_MODULE();

- (dispatch_queue_t)methodQueue
{
  return _workerQueue;
}

@end
