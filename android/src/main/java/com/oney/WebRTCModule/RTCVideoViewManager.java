package com.oney.WebRTCModule;

import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.opengl.GLSurfaceView;

import com.facebook.csslayout.CSSNode;
import com.facebook.csslayout.MeasureOutput;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewProps;
import android.util.Log;
import android.view.View;
import javax.annotation.Nullable;

import org.webrtc.*;

public class RTCVideoViewManager extends SimpleViewManager<WebRTCView> {
  private final static String TAG = RTCVideoViewManager.class.getCanonicalName();

  public static final String REACT_CLASS = "RTCVideoView";
  public ThemedReactContext mContext;
  private VideoRenderer.Callbacks localRender;

  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @Override
  public WebRTCView createViewInstance(ThemedReactContext context) {
    mContext = context;
    WebRTCView view = new WebRTCView(context);
    // view.setPreserveEGLContextOnPause(true);
    // view.setKeepScreenOn(true);
    return view;
  }
  @ReactProp(name = "streamURL", defaultInt = -1)
  public void setStreamURL(WebRTCView view, int streamURL) {
    if (streamURL >= 0) {
      WebRTCModule module = mContext.getNativeModule(WebRTCModule.class);
      MediaStream mediaStream = module.mMediaStreams.get(streamURL);
      view.setStream(mediaStream);
    }
  }
}
