/*
 * libjingle
 * Copyright 2014 Google Inc.
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
#if TARGET_OS_IPHONE
#import <GLKit/GLKit.h>
#else
#import <AppKit/NSOpenGL.h>
#endif

@class RTCI420Frame;

// RTCOpenGLVideoRenderer issues appropriate OpenGL commands to draw a frame to
// the currently bound framebuffer. Supports OpenGL 3.2 and OpenGLES 2.0. OpenGL
// framebuffer creation and management should be handled elsewhere using the
// same context used to initialize this class.
@interface RTCOpenGLVideoRenderer : NSObject

// The last successfully drawn frame. Used to avoid drawing frames unnecessarily
// hence saving battery life by reducing load.
@property(nonatomic, readonly) RTCI420Frame* lastDrawnFrame;

#if TARGET_OS_IPHONE
- (instancetype)initWithContext:(EAGLContext*)context;
#else
- (instancetype)initWithContext:(NSOpenGLContext*)context;
#endif

// Draws |frame| onto the currently bound OpenGL framebuffer. |setupGL| must be
// called before this function will succeed.
- (BOOL)drawFrame:(RTCI420Frame*)frame;

// The following methods are used to manage OpenGL resources. On iOS
// applications should release resources when placed in background for use in
// the foreground application. In fact, attempting to call OpenGLES commands
// while in background will result in application termination.

// Sets up the OpenGL state needed for rendering.
- (void)setupGL;
// Tears down the OpenGL state created by |setupGL|.
- (void)teardownGL;

#ifndef DOXYGEN_SHOULD_SKIP_THIS
// Disallow init and don't add to documentation
- (id)init __attribute__((
    unavailable("init is not a supported initializer for this class.")));
#endif /* DOXYGEN_SHOULD_SKIP_THIS */

@end
