package com.oney.WebRTCModule;

import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.opengl.GLSurfaceView;

import com.facebook.csslayout.CSSNode;
import com.facebook.csslayout.MeasureOutput;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.uimanager.*;
import com.facebook.react.uimanager.ReactProp;
import android.util.Log;
import android.view.View;
import javax.annotation.Nullable;

import org.webrtc.*;

public class RTCVideoViewManager extends SimpleViewManager<GLSurfaceView> {
  private final static String TAG = RTCVideoViewManager.class.getCanonicalName();

  public static final String REACT_CLASS = "RTCVideoView";
  public ThemedReactContext mContext;
  private VideoRenderer.Callbacks localRender;

  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @UIProp(UIProp.Type.NUMBER)
  public static final String PROP_STREAM_URL = "streamURL";

  @Override
  public GLSurfaceView createViewInstance(ThemedReactContext context) {
    mContext = context;
    GLSurfaceView view = new GLSurfaceView(context);
    // view.setPreserveEGLContextOnPause(true);
    // view.setKeepScreenOn(true);
    VideoRendererGui.setView(view, new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "runwqrqewrrunnn");
        }
    });

    RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL;
    localRender = VideoRendererGui.create(
                    0, 0,
                    50, 50, scalingType, false);
    return view;
  }
  @Override
  public void updateView(final GLSurfaceView view,
                         final CatalystStylesDiffMap props) {
    // super.updateView(view, props);

    if (props.hasKey(PROP_STREAM_URL)) {
      int streamId = props.getInt(PROP_STREAM_URL, -1);
      Log.d(TAG, "werjiljiljlijiljlij: " + props.toString());
      if (streamId >= 0) {
        WebRTCModule module = mContext.getNativeModule(WebRTCModule.class);
        MediaStream mediaStream = module.mMediaStreams.get(streamId);
        Log.d(TAG, "jilijqwerdsfsd: " + (mediaStream == null ? "null" : "nonull"));

        RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL;

        Log.d(TAG, "jwelrjidsdksfk: " + mediaStream.videoTracks.size());
        mediaStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
        VideoRendererGui.update(localRender,
                0, 0,
                50, 50, scalingType, false);
      }
    }
  }
}
