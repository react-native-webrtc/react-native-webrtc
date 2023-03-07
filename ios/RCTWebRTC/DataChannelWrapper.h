#import <Foundation/Foundation.h>
#import <WebRTC/RTCDataChannel.h>

NS_ASSUME_NONNULL_BEGIN

@class DataChannelWrapper;

@protocol DataChannelWrapperDelegate<NSObject>

- (void)dataChannelDidChangeState:(DataChannelWrapper *)dataChannelWrapper;
- (void)dataChannel:(DataChannelWrapper *)dataChannelWrapper didReceiveMessageWithBuffer:(RTCDataBuffer *)buffer;
- (void)dataChannel:(DataChannelWrapper *)dataChannelWrapper didChangeBufferedAmount:(uint64_t)amount;

@end

@interface DataChannelWrapper : NSObject

- (instancetype)initWithChannel:(RTCDataChannel *)channel reactTag:(NSString *)tag;

@property(nonatomic, nonnull, copy) NSNumber *pcId;
@property(nonatomic, nonnull, readonly) RTCDataChannel *channel;
@property(nonatomic, nonnull, readonly) NSString *reactTag;
@property(nonatomic, nullable, weak) id<DataChannelWrapperDelegate> delegate;

@end

NS_ASSUME_NONNULL_END
