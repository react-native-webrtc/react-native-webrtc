#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * Plays the outgoing-call ringback ("dialtone") while an outgoing call is
 * connecting, until `stop` is called.
 *
 * The tone is synthesized in memory (no bundled asset) and played through the
 * CallKit-activated audio session, so it follows the call route. Start it from
 * `provider:didActivateAudioSession:` while the outgoing call is still connecting.
 */
@interface DialtonePlayer : NSObject

+ (instancetype)shared;

/** Starts looping the ringback. No-op if already playing. */
- (void)play;

/** Stops the ringback. Safe to call when nothing is playing. */
- (void)stop;

@end

NS_ASSUME_NONNULL_END
