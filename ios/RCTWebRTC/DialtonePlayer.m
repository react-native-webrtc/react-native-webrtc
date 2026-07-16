#import "DialtonePlayer.h"

#import <AVFoundation/AVFoundation.h>
#import <math.h>

@interface DialtonePlayer ()
@property(nonatomic, strong, nullable) AVAudioPlayer *player;
@end

@implementation DialtonePlayer

+ (instancetype)shared {
    static DialtonePlayer *instance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        instance = [[DialtonePlayer alloc] init];
    });
    return instance;
}

/**
 * Synthesizes one loopable cycle of the North-American ringback (440 Hz + 480 Hz,
 * 2 s on / 4 s off) as a 16 kHz mono 16-bit PCM WAV. Cached after first build.
 */
+ (NSData *)ringbackData {
    static NSData *data = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        const double sr = 16000.0;
        const double f1 = 440.0, f2 = 480.0;
        const double amp = 0.28;
        const int onN = (int)(sr * 2.0);   // 2 s ring
        const int offN = (int)(sr * 4.0);  // 4 s silence
        const int fade = (int)(sr * 0.008); // 8 ms edge fades (avoid loop clicks)
        const int total = onN + offN;

        NSMutableData *pcm = [NSMutableData dataWithCapacity:total * 2];
        for (int i = 0; i < total; i++) {
            double s = 0.0;
            if (i < onN) {
                double t = i / sr;
                s = amp * (sin(2.0 * M_PI * f1 * t) + sin(2.0 * M_PI * f2 * t)) / 2.0;
                if (i < fade) s *= (double)i / fade;
                if (i > onN - fade) s *= (double)(onN - i) / fade;
            }
            double clamped = fmax(-1.0, fmin(1.0, s));
            int16_t v = (int16_t)(clamped * 32767.0);
            [pcm appendBytes:&v length:sizeof(v)];
        }

        // Minimal 44-byte WAV header (little-endian, matches ARM byte order).
        uint32_t dataLen = (uint32_t)pcm.length;
        uint32_t sampleRate = (uint32_t)sr;
        uint16_t channels = 1, bitsPerSample = 16;
        uint32_t byteRate = sampleRate * channels * bitsPerSample / 8;
        uint16_t blockAlign = channels * bitsPerSample / 8;
        uint16_t audioFormat = 1; // PCM
        uint32_t fmtChunkLen = 16;
        uint32_t riffLen = 36 + dataLen;

        NSMutableData *wav = [NSMutableData data];
        [wav appendBytes:"RIFF" length:4];
        [wav appendBytes:&riffLen length:4];
        [wav appendBytes:"WAVE" length:4];
        [wav appendBytes:"fmt " length:4];
        [wav appendBytes:&fmtChunkLen length:4];
        [wav appendBytes:&audioFormat length:2];
        [wav appendBytes:&channels length:2];
        [wav appendBytes:&sampleRate length:4];
        [wav appendBytes:&byteRate length:4];
        [wav appendBytes:&blockAlign length:2];
        [wav appendBytes:&bitsPerSample length:2];
        [wav appendBytes:"data" length:4];
        [wav appendBytes:&dataLen length:4];
        [wav appendData:pcm];

        data = [wav copy];
    });
    return data;
}

- (void)play {
    @synchronized(self) {
        if (self.player != nil) {
            return;
        }

        NSError *error = nil;
        AVAudioPlayer *p = [[AVAudioPlayer alloc] initWithData:[[self class] ringbackData] error:&error];
        if (p == nil) {
            NSLog(@"[DialtonePlayer] Failed to create player: %@", error.localizedDescription);
            return;
        }

        p.numberOfLoops = -1; // loop until stopped
        p.volume = 1.0;
        self.player = p;
        [p prepareToPlay];
        [p play];
    }
}

- (void)stop {
    @synchronized(self) {
        if (self.player == nil) {
            return;
        }
        [self.player stop];
        self.player = nil;
    }
}

@end
