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

// Subset of rtc::LoggingSeverity.
typedef NS_ENUM(NSInteger, RTCLoggingSeverity) {
  kRTCLoggingSeverityVerbose,
  kRTCLoggingSeverityInfo,
  kRTCLoggingSeverityWarning,
  kRTCLoggingSeverityError,
};

#if defined(__cplusplus)
extern "C" void RTCLogEx(RTCLoggingSeverity severity, NSString* logString);
extern "C" void RTCSetMinDebugLogLevel(RTCLoggingSeverity severity);
extern "C" NSString* RTCFileName(const char* filePath);
#else

// Wrapper for C++ LOG(sev) macros.
// Logs the log string to the webrtc logstream for the given severity.
extern void RTCLogEx(RTCLoggingSeverity severity, NSString* logString);

// Wrapper for rtc::LogMessage::LogToDebug.
// Sets the minimum severity to be logged to console.
extern void RTCSetMinDebugLogLevel(RTCLoggingSeverity severity);

// Returns the filename with the path prefix removed.
extern NSString* RTCFileName(const char* filePath);

#endif

// Some convenience macros.

#define RTCLogString(format, ...)                    \
  [NSString stringWithFormat:@"(%@:%d %s): " format, \
      RTCFileName(__FILE__),                         \
      __LINE__,                                      \
      __FUNCTION__,                                  \
      ##__VA_ARGS__]

#define RTCLogFormat(severity, format, ...)                    \
  do {                                                         \
    NSString *logString = RTCLogString(format, ##__VA_ARGS__); \
    RTCLogEx(severity, logString);                             \
  } while (false)

#define RTCLogVerbose(format, ...)                                \
  RTCLogFormat(kRTCLoggingSeverityVerbose, format, ##__VA_ARGS__) \

#define RTCLogInfo(format, ...)                                   \
  RTCLogFormat(kRTCLoggingSeverityInfo, format, ##__VA_ARGS__)    \

#define RTCLogWarning(format, ...)                                \
  RTCLogFormat(kRTCLoggingSeverityWarning, format, ##__VA_ARGS__) \

#define RTCLogError(format, ...)                                  \
  RTCLogFormat(kRTCLoggingSeverityError, format, ##__VA_ARGS__)   \

#ifdef _DEBUG
#define RTCLogDebug(format, ...) RTCLogInfo(format, ##__VA_ARGS__)
#else
#define RTCLogDebug(format, ...) \
  do {                           \
  } while (false)
#endif

#define RTCLog(format, ...) RTCLogInfo(format, ##__VA_ARGS__)
