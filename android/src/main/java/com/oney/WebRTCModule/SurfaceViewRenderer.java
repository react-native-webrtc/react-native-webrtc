/*
 *  Copyright 2015 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

// Retrieved from upstream's master on August 26, 2016.

package com.oney.WebRTCModule;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.graphics.Point;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.concurrent.CountDownLatch;

import javax.microedition.khronos.egl.EGLContext;

import org.webrtc.EglBase;
import org.webrtc.GlRectDrawer;
import org.webrtc.GlUtil;
import org.webrtc.Logging;
import org.webrtc.RendererCommon;
import org.webrtc.ThreadUtils;
import org.webrtc.VideoRenderer;

/**
 * Implements org.webrtc.VideoRenderer.Callbacks by displaying the video stream on a SurfaceView.
 * renderFrame() is asynchronous to avoid blocking the calling thread.
 * This class is thread safe and handles access from potentially four different threads:
 * Interaction from the main app in init, release, setMirror, and setScalingtype.
 * Interaction from C++ rtc::VideoSinkInterface in renderFrame.
 * Interaction from the Activity lifecycle in surfaceCreated, surfaceChanged, and surfaceDestroyed.
 * Interaction with the layout framework in onMeasure and onSizeChanged.
 */
public class SurfaceViewRenderer extends SurfaceView
    implements SurfaceHolder.Callback, VideoRenderer.Callbacks {
  private static final String TAG = "SurfaceViewRenderer";

  // Dedicated render thread.
  private HandlerThread renderThread;
  // |renderThreadHandler| is a handler for communicating with |renderThread|, and is synchronized
  // on |handlerLock|.
  private final Object handlerLock = new Object();
  private Handler renderThreadHandler;

  // EGL and GL resources for drawing YUV/OES textures. After initilization, these are only accessed
  // from the render thread.
  private EglBase eglBase;
  private final RendererCommon.YuvUploader yuvUploader = new RendererCommon.YuvUploader();
  private RendererCommon.GlDrawer drawer;
  // Texture ids for YUV frames. Allocated on first arrival of a YUV frame.
  private int[] yuvTextures = null;

  // Pending frame to render. Serves as a queue with size 1. Synchronized on |frameLock|.
  private final Object frameLock = new Object();
  private VideoRenderer.I420Frame pendingFrame;

  // These variables are synchronized on |layoutLock|.
  private final Object layoutLock = new Object();
  // These dimension values are used to keep track of the state in these functions: onMeasure(),
  // onLayout(), and surfaceChanged(). A new layout is triggered with requestLayout(). This happens
  // internally when the incoming frame size changes. requestLayout() can also be triggered
  // externally. The layout change is a two pass process: first onMeasure() is called in a top-down
  // traversal of the View tree, followed by an onLayout() pass that is also top-down. During the
  // onLayout() pass, each parent is responsible for positioning its children using the sizes
  // computed in the measure pass.
  // |desiredLayoutsize| is the layout size we have requested in onMeasure() and are waiting for to
  // take effect.
  private Point desiredLayoutSize = new Point();
  // |layoutSize|/|surfaceSize| is the actual current layout/surface size. They are updated in
  // onLayout() and surfaceChanged() respectively.
  private final Point layoutSize = new Point();
  // TODO(magjed): Enable hardware scaler with SurfaceHolder.setFixedSize(). This will decouple
  // layout and surface size.
  private final Point surfaceSize = new Point();
  // |isSurfaceCreated| keeps track of the current status in surfaceCreated()/surfaceDestroyed().
  private boolean isSurfaceCreated;
  // Last rendered frame dimensions, or 0 if no frame has been rendered yet.
  private int frameWidth;
  private int frameHeight;
  private int frameRotation;
  // |scalingType| determines how the video will fill the allowed layout area in onMeasure().
  private RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_BALANCED;
  // If true, mirrors the video stream horizontally.
  private boolean mirror;
  // Callback for reporting renderer events.
  private RendererCommon.RendererEvents rendererEvents;

  // These variables are synchronized on |statisticsLock|.
  private final Object statisticsLock = new Object();
  // Total number of video frames received in renderFrame() call.
  private int framesReceived;
  // Number of video frames dropped by renderFrame() because previous frame has not been rendered
  // yet.
  private int framesDropped;
  // Number of rendered video frames.
  private int framesRendered;
  // Time in ns when the first video frame was rendered.
  private long firstFrameTimeNs;
  // Time in ns spent in renderFrameOnRenderThread() function.
  private long renderTimeNs;

  // Runnable for posting frames to render thread.
  private final Runnable renderFrameRunnable = new Runnable() {
    @Override public void run() {
      renderFrameOnRenderThread();
    }
  };
  // Runnable for clearing Surface to black.
  private final Runnable makeBlackRunnable = new Runnable() {
    @Override public void run() {
      makeBlack();
    }
  };

  /**
   * Standard View constructor. In order to render something, you must first call init().
   */
  public SurfaceViewRenderer(Context context) {
    super(context);
    getHolder().addCallback(this);
  }

  /**
   * Standard View constructor. In order to render something, you must first call init().
   */
  public SurfaceViewRenderer(Context context, AttributeSet attrs) {
    super(context, attrs);
    getHolder().addCallback(this);
  }

  /**
   * Initialize this class, sharing resources with |sharedContext|. It is allowed to call init() to
   * reinitialize the renderer after a previous init()/release() cycle.
   */
  public void init(
      EglBase.Context sharedContext, RendererCommon.RendererEvents rendererEvents) {
    init(sharedContext, rendererEvents, EglBase.CONFIG_PLAIN, new GlRectDrawer());
  }

  /**
   * Initialize this class, sharing resources with |sharedContext|. The custom |drawer| will be used
   * for drawing frames on the EGLSurface. This class is responsible for calling release() on
   * |drawer|. It is allowed to call init() to reinitialize the renderer after a previous
   * init()/release() cycle.
   */
  public void init(
      final EglBase.Context sharedContext, RendererCommon.RendererEvents rendererEvents,
      final int[] configAttributes, RendererCommon.GlDrawer drawer) {
    synchronized (handlerLock) {
      if (renderThreadHandler != null) {
        throw new IllegalStateException(getResourceName() + "Already initialized");
      }
      Logging.d(TAG, getResourceName() + "Initializing.");
      this.rendererEvents = rendererEvents;
      this.drawer = drawer;
      renderThread = new HandlerThread(TAG);
      renderThread.start();
      renderThreadHandler = new Handler(renderThread.getLooper());
      // Create EGL context on the newly created render thread. It should be possibly to create the
      // context on this thread and make it current on the render thread, but this causes failure on
      // some Marvel based JB devices. https://bugs.chromium.org/p/webrtc/issues/detail?id=6350.
      ThreadUtils.invokeAtFrontUninterruptibly(renderThreadHandler, new Runnable() {
        @Override
        public void run() {
          eglBase = EglBase.create(sharedContext, configAttributes);
        }
      });
    }
    tryCreateEglSurface();
  }

  /**
   * Create and make an EGLSurface current if both init() and surfaceCreated() have been called.
   */
  public void tryCreateEglSurface() {
    // |renderThreadHandler| is only created after |eglBase| is created in init(), so the
    // following code will only execute if eglBase != null.
    runOnRenderThread(new Runnable() {
      @Override
      public void run() {
        synchronized (layoutLock) {
          if (eglBase != null && isSurfaceCreated && !eglBase.hasSurface()) {
            eglBase.createSurface(getHolder().getSurface());
            eglBase.makeCurrent();
            // Necessary for YUV frames with odd width.
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
          }
        }

        // XXX by Saúl Ibarra Corretgé <saghul@gmail.com>: Until an actual frame
        // is available to render, draw black; otherwise, this SurfaceView will
        // appear transparent.
        makeBlack();
      }
    });
  }

  /**
   * Block until any pending frame is returned and all GL resources released, even if an interrupt
   * occurs. If an interrupt occurs during release(), the interrupt flag will be set. This function
   * should be called before the Activity is destroyed and the EGLContext is still valid. If you
   * don't call this function, the GL resources might leak.
   */
  public void release() {
    final CountDownLatch eglCleanupBarrier = new CountDownLatch(1);
    synchronized (handlerLock) {
      if (renderThreadHandler == null) {
        Logging.d(TAG, getResourceName() + "Already released");
        return;
      }
      // Release EGL and GL resources on render thread.
      // TODO(magjed): This might not be necessary - all OpenGL resources are automatically deleted
      // when the EGL context is lost. It might be dangerous to delete them manually in
      // Activity.onDestroy().
      renderThreadHandler.postAtFrontOfQueue(new Runnable() {
        @Override public void run() {
          drawer.release();
          drawer = null;
          if (yuvTextures != null) {
            GLES20.glDeleteTextures(3, yuvTextures, 0);
            yuvTextures = null;
          }
          // Clear last rendered image to black.
          makeBlack();
          eglBase.release();
          eglBase = null;
          eglCleanupBarrier.countDown();
        }
      });
      // Don't accept any more frames or messages to the render thread.
      renderThreadHandler = null;
    }
    // Make sure the EGL/GL cleanup posted above is executed.
    ThreadUtils.awaitUninterruptibly(eglCleanupBarrier);
    renderThread.quit();
    synchronized (frameLock) {
      if (pendingFrame != null) {
        VideoRenderer.renderFrameDone(pendingFrame);
        pendingFrame = null;
      }
    }
    // The |renderThread| cleanup is not safe to cancel and we need to wait until it's done.
    ThreadUtils.joinUninterruptibly(renderThread);
    renderThread = null;
    // Reset statistics and event reporting.
    synchronized (layoutLock) {
      frameWidth = 0;
      frameHeight = 0;
      frameRotation = 0;
      rendererEvents = null;
    }
    resetStatistics();
  }

  /**
   * Reset statistics. This will reset the logged statistics in logStatistics(), and
   * RendererEvents.onFirstFrameRendered() will be called for the next frame.
   */
  public void resetStatistics() {
    synchronized (statisticsLock) {
      framesReceived = 0;
      framesDropped = 0;
      framesRendered = 0;
      firstFrameTimeNs = 0;
      renderTimeNs = 0;
    }
  }

  /**
   * Set if the video stream should be mirrored or not.
   */
  public void setMirror(final boolean mirror) {
    synchronized (layoutLock) {
      this.mirror = mirror;
    }
  }

  /**
   * Set how the video will fill the allowed layout area.
   */
  public void setScalingType(RendererCommon.ScalingType scalingType) {
    synchronized (layoutLock) {
      this.scalingType = scalingType;
    }
  }

  // VideoRenderer.Callbacks interface.
  @Override
  public void renderFrame(VideoRenderer.I420Frame frame) {
    synchronized (statisticsLock) {
      ++framesReceived;
    }
    synchronized (handlerLock) {
      if (renderThreadHandler == null) {
        Logging.d(TAG, getResourceName()
            + "Dropping frame - Not initialized or already released.");
        VideoRenderer.renderFrameDone(frame);
        return;
      }
      synchronized (frameLock) {
        if (pendingFrame != null) {
          // Drop old frame.
          synchronized (statisticsLock) {
            ++framesDropped;
          }
          VideoRenderer.renderFrameDone(pendingFrame);
        }
        pendingFrame = frame;
        renderThreadHandler.post(renderFrameRunnable);
      }
    }
  }

  // Returns desired layout size given current measure specification and video aspect ratio.
  private Point getDesiredLayoutSize(int widthSpec, int heightSpec) {
    synchronized (layoutLock) {
      final int maxWidth = getDefaultSize(Integer.MAX_VALUE, widthSpec);
      final int maxHeight = getDefaultSize(Integer.MAX_VALUE, heightSpec);
      final Point size =
          RendererCommon.getDisplaySize(scalingType, frameAspectRatio(), maxWidth, maxHeight);
      if (MeasureSpec.getMode(widthSpec) == MeasureSpec.EXACTLY) {
        size.x = maxWidth;
      }
      if (MeasureSpec.getMode(heightSpec) == MeasureSpec.EXACTLY) {
        size.y = maxHeight;
      }
      return size;
    }
  }

  // View layout interface.
  @Override
  protected void onMeasure(int widthSpec, int heightSpec) {
    final boolean isNewSize;
    synchronized (layoutLock) {
      if (frameWidth == 0 || frameHeight == 0) {
        super.onMeasure(widthSpec, heightSpec);
        return;
      }
      desiredLayoutSize = getDesiredLayoutSize(widthSpec, heightSpec);
      isNewSize = (desiredLayoutSize.x != getMeasuredWidth()
          || desiredLayoutSize.y != getMeasuredHeight());
      setMeasuredDimension(desiredLayoutSize.x, desiredLayoutSize.y);
    }
    if (isNewSize) {
      // Clear the surface asap before the layout change to avoid stretched video and other
      // render artifacs. Don't wait for it to finish because the IO thread should never be
      // blocked, so it's a best-effort attempt.
      synchronized (handlerLock) {
        if (renderThreadHandler != null) {
          renderThreadHandler.postAtFrontOfQueue(makeBlackRunnable);
        }
      }
    }
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    synchronized (layoutLock) {
      layoutSize.x = right - left;
      layoutSize.y = bottom - top;
    }
    // Might have a pending frame waiting for a layout of correct size.
    runOnRenderThread(renderFrameRunnable);
  }

  // SurfaceHolder.Callback interface.
  @Override
  public void surfaceCreated(final SurfaceHolder holder) {
    Logging.d(TAG, getResourceName() + "Surface created.");
    synchronized (layoutLock) {
      isSurfaceCreated = true;
    }
    tryCreateEglSurface();
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    Logging.d(TAG, getResourceName() + "Surface destroyed.");
    synchronized (layoutLock) {
      isSurfaceCreated = false;
      surfaceSize.x = 0;
      surfaceSize.y = 0;
    }
    runOnRenderThread(new Runnable() {
      @Override
      public void run() {
        if (eglBase != null) {
          eglBase.detachCurrent();
          eglBase.releaseSurface();
        }
      }
    });
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    Logging.d(TAG, getResourceName() + "Surface changed: " + width + "x" + height);
    synchronized (layoutLock) {
      surfaceSize.x = width;
      surfaceSize.y = height;
    }
    // Might have a pending frame waiting for a surface of correct size.
    runOnRenderThread(renderFrameRunnable);
  }

  /**
   * Private helper function to post tasks safely.
   */
  private void runOnRenderThread(Runnable runnable) {
    synchronized (handlerLock) {
      if (renderThreadHandler != null) {
        renderThreadHandler.post(runnable);
      }
    }
  }

  private String getResourceName() {
    try {
      return getResources().getResourceEntryName(getId()) + ": ";
    } catch (NotFoundException e) {
      return "";
    }
  }

  private void makeBlack() {
    if (Thread.currentThread() != renderThread) {
      throw new IllegalStateException(getResourceName() + "Wrong thread.");
    }
    if (eglBase != null && eglBase.hasSurface()) {
      GLES20.glClearColor(0, 0, 0, 0);
      GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
      eglBase.swapBuffers();
    }
  }

  /**
   * Requests new layout if necessary. Returns true if layout and surface size are consistent.
   */
  private boolean checkConsistentLayout() {
    if (Thread.currentThread() != renderThread) {
      throw new IllegalStateException(getResourceName() + "Wrong thread.");
    }
    synchronized (layoutLock) {
      // Return false while we are in the middle of a layout change.
      // XXX by Lyubomir Marinov <lyubomir.marinov@jitsi.org>: Do not wait for
      // layoutSize to become equal to desiredLayoutSize because that may never
      // happen: desiredLayoutSize expresses the desire of this View computed
      // during onMeasure but the final decision on layoutSize belongs to the
      // ViewParent of this View taken around the execution time of onLayout. If
      // this instance waits for the condition and its ViewParent does not
      // satisfy the request for desiredLayoutSize, this SurfaceViewRenderer
      // will render black.
      return /* layoutSize.equals(desiredLayoutSize) && */ surfaceSize.equals(layoutSize);
    }
  }

  /**
   * Renders and releases |pendingFrame|.
   */
  private void renderFrameOnRenderThread() {
    if (Thread.currentThread() != renderThread) {
      throw new IllegalStateException(getResourceName() + "Wrong thread.");
    }
    // Fetch and render |pendingFrame|.
    final VideoRenderer.I420Frame frame;
    synchronized (frameLock) {
      if (pendingFrame == null) {
        return;
      }
      frame = pendingFrame;
      pendingFrame = null;
    }
    updateFrameDimensionsAndReportEvents(frame);
    if (eglBase == null || !eglBase.hasSurface()) {
      Logging.d(TAG, getResourceName() + "No surface to draw on");
      VideoRenderer.renderFrameDone(frame);
      return;
    }
    if (!checkConsistentLayout()) {
      // Output intermediate black frames while the layout is updated.
      makeBlack();
      VideoRenderer.renderFrameDone(frame);
      return;
    }
    // After a surface size change, the EGLSurface might still have a buffer of the old size in the
    // pipeline. Querying the EGLSurface will show if the underlying buffer dimensions haven't yet
    // changed. Such a buffer will be rendered incorrectly, so flush it with a black frame.
    synchronized (layoutLock) {
      if (eglBase.surfaceWidth() != surfaceSize.x || eglBase.surfaceHeight() != surfaceSize.y) {
        makeBlack();
      }
    }

    final long startTimeNs = System.nanoTime();
    final float[] texMatrix;
    synchronized (layoutLock) {
      final float[] rotatedSamplingMatrix =
          RendererCommon.rotateTextureMatrix(frame.samplingMatrix, frame.rotationDegree);
      final float[] layoutMatrix = RendererCommon.getLayoutMatrix(
          mirror, frameAspectRatio(), (float) layoutSize.x / layoutSize.y);
      texMatrix = RendererCommon.multiplyMatrices(rotatedSamplingMatrix, layoutMatrix);
    }

    // TODO(magjed): glClear() shouldn't be necessary since every pixel is covered anyway, but it's
    // a workaround for bug 5147. Performance will be slightly worse.
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    if (frame.yuvFrame) {
      // Make sure YUV textures are allocated.
      if (yuvTextures == null) {
        yuvTextures = new int[3];
        for (int i = 0; i < 3; i++)  {
          yuvTextures[i] = GlUtil.generateTexture(GLES20.GL_TEXTURE_2D);
        }
      }
      yuvUploader.uploadYuvData(
          yuvTextures, frame.width, frame.height, frame.yuvStrides, frame.yuvPlanes);
      drawer.drawYuv(yuvTextures, texMatrix, frame.rotatedWidth(), frame.rotatedHeight(),
          0, 0, surfaceSize.x, surfaceSize.y);
    } else {
      drawer.drawOes(frame.textureId, texMatrix, frame.rotatedWidth(), frame.rotatedHeight(),
          0, 0, surfaceSize.x, surfaceSize.y);
    }

    eglBase.swapBuffers();
    VideoRenderer.renderFrameDone(frame);
    synchronized (statisticsLock) {
      if (framesRendered == 0) {
        firstFrameTimeNs = startTimeNs;
        synchronized (layoutLock) {
          Logging.d(TAG, getResourceName() + "Reporting first rendered frame.");
          if (rendererEvents != null) {
            rendererEvents.onFirstFrameRendered();
          }
        }
      }
      ++framesRendered;
      renderTimeNs += (System.nanoTime() - startTimeNs);
      if (framesRendered % 300 == 0) {
        logStatistics();
      }
    }
  }

  // Return current frame aspect ratio, taking rotation into account.
  private float frameAspectRatio() {
    synchronized (layoutLock) {
      if (frameWidth == 0 || frameHeight == 0) {
        return 0.0f;
      }
      return (frameRotation % 180 == 0) ? (float) frameWidth / frameHeight
                                        : (float) frameHeight / frameWidth;
    }
  }

  // Update frame dimensions and report any changes to |rendererEvents|.
  private void updateFrameDimensionsAndReportEvents(VideoRenderer.I420Frame frame) {
    synchronized (layoutLock) {
      if (frameWidth != frame.width || frameHeight != frame.height
          || frameRotation != frame.rotationDegree) {
        Logging.d(TAG, getResourceName() + "Reporting frame resolution changed to "
            + frame.width + "x" + frame.height + " with rotation " + frame.rotationDegree);
        if (rendererEvents != null) {
          rendererEvents.onFrameResolutionChanged(frame.width, frame.height, frame.rotationDegree);
        }
        frameWidth = frame.width;
        frameHeight = frame.height;
        frameRotation = frame.rotationDegree;
        post(new Runnable() {
          @Override public void run() {
            requestLayout();
          }
        });
      }
    }
  }

  private void logStatistics() {
    synchronized (statisticsLock) {
      Logging.d(TAG, getResourceName() + "Frames received: "
          + framesReceived + ". Dropped: " + framesDropped + ". Rendered: " + framesRendered);
      if (framesReceived > 0 && framesRendered > 0) {
        final long timeSinceFirstFrameNs = System.nanoTime() - firstFrameTimeNs;
        Logging.d(TAG, getResourceName() + "Duration: " + (int) (timeSinceFirstFrameNs / 1e6) +
            " ms. FPS: " + framesRendered * 1e9 / timeSinceFirstFrameNs);
        Logging.d(TAG, getResourceName() + "Average render time: "
            + (int) (renderTimeNs / (1000 * framesRendered)) + " us.");
      }
    }
  }
}
