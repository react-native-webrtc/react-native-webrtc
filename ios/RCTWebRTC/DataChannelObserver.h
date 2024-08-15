#import <Foundation/Foundation.h>
#import <WebRTC/RTCDataChannel.h>

NS_ASSUME_NONNULL_BEGIN

@class DataChannelObserver;

@protocol DataChannelObserverDelegate<NSObject>

- (void)dataChannelDidChangeState:(DataChannelObserver *)DataChannelObserver;
- (void)dataChannel:(DataChannelObserver *)DataChannelObserver didReceiveMessageWithBuffer:(RTCDataBuffer *)buffer;
- (void)dataChannel:(DataChannelObserver *)DataChannelObserver didChangeBufferedAmount:(uint64_t)amount;

@end

@interface DataChannelObserver : NSObject

- (instancetype)initWithChannel:(RTCDataChannel *)channel reactTag:(NSString *)tag;

@property(nonatomic, nonnull, copy) NSNumber *peerConnectionId;
@property(nonatomic, nonnull, readonly) RTCDataChannel *channel;
@property(nonatomic, nonnull, readonly) NSString *reactTag;
@property(nonatomic, nullable, weak) id<DataChannelObserverDelegate> delegate;

@end

NS_ASSUME_NONNULL_END
