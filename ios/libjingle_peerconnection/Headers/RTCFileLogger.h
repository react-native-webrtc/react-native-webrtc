/*
 * libjingle
 * Copyright 2015 Google Inc.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#import <Foundation/Foundation.h>

// TODO(tkchin): Move this to a common location.
#ifndef NS_DESIGNATED_INITIALIZER
#define NS_DESIGNATED_INITIALIZER
#endif

typedef NS_ENUM(NSUInteger, RTCFileLoggerSeverity) {
  kRTCFileLoggerSeverityVerbose,
  kRTCFileLoggerSeverityInfo,
  kRTCFileLoggerSeverityWarning,
  kRTCFileLoggerSeverityError
};

typedef NS_ENUM(NSUInteger, RTCFileLoggerRotationType) {
  kRTCFileLoggerTypeCall,
  kRTCFileLoggerTypeApp,
};

// This class intercepts WebRTC logs and saves them to a file. The file size
// will not exceed the given maximum bytesize. When the maximum bytesize is
// reached, logs are rotated according to the rotationType specified.
// For kRTCFileLoggerTypeCall, logs from the beginning and the end
// are preserved while the middle section is overwritten instead.
// For kRTCFileLoggerTypeApp, the oldest log is overwritten.
// This class is not threadsafe.
@interface RTCFileLogger : NSObject

// The severity level to capture. The default is kRTCFileLoggerSeverityInfo.
@property(nonatomic, assign) RTCFileLoggerSeverity severity;

// The rotation type for this file logger. The default is
// kRTCFileLoggerTypeCall.
@property(nonatomic, readonly) RTCFileLoggerRotationType rotationType;

// Disables buffering disk writes. Should be set before |start|. Buffering
// is enabled by default for performance.
@property(nonatomic, assign) BOOL shouldDisableBuffering;

// Default constructor provides default settings for dir path, file size and
// rotation type.
- (instancetype)init;

// Create file logger with default rotation type.
- (instancetype)initWithDirPath:(NSString *)dirPath
                    maxFileSize:(NSUInteger)maxFileSize;

- (instancetype)initWithDirPath:(NSString *)dirPath
                    maxFileSize:(NSUInteger)maxFileSize
                   rotationType:(RTCFileLoggerRotationType)rotationType
    NS_DESIGNATED_INITIALIZER;

// Starts writing WebRTC logs to disk if not already started. Overwrites any
// existing file(s).
- (void)start;

// Stops writing WebRTC logs to disk. This method is also called on dealloc.
- (void)stop;

// Returns the current contents of the logs, or nil if start has been called
// without a stop.
- (NSData *)logData;

@end
