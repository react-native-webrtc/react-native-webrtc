package com.oney.WebRTCModule;

import android.content.Context;
import android.opengl.GLSurfaceView;

import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

public class WebRTCView extends GLSurfaceView {
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer mVideoRenderer;
    private MediaStream mMediaStream;

    public WebRTCView(Context context) {
        super(context);

        VideoRendererGui.setView(this, new Runnable() {
            @Override
            public void run() {
            }
        });

        RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL;
        localRender = VideoRendererGui.create(0, 0, 100, 100, scalingType, false);
    }

    public void setStream(MediaStream mediaStream) {
        if (mMediaStream != null) {
            mMediaStream.videoTracks.get(0).removeRenderer(mVideoRenderer);
        }
        mMediaStream = mediaStream;

        RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL;

        mVideoRenderer = new VideoRenderer(localRender);
        mediaStream.videoTracks.get(0).addRenderer(mVideoRenderer);
        VideoRendererGui.update(localRender, 0, 0, 100, 100, scalingType, false);
    }
}
