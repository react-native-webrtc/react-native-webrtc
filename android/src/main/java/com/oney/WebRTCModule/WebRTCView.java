package com.oney.WebRTCModule;

import android.content.Context;
import android.opengl.GLSurfaceView;

import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

public class WebRTCView extends GLSurfaceView {
    /**
     * The scaling type to be utilized by default.
     *
     * The default value is in accord with
     * https://www.w3.org/TR/html5/embedded-content-0.html#the-video-element:
     *
     * In the absence of style rules to the contrary, video content should be
     * rendered inside the element's playback area such that the video content
     * is shown centered in the playback area at the largest possible size that
     * fits completely within it, with the video content's aspect ratio being
     * preserved. Thus, if the aspect ratio of the playback area does not match
     * the aspect ratio of the video, the video will be shown letterboxed or
     * pillarboxed. Areas of the element's playback area that do not contain the
     * video represent nothing.
     */
    private static final RendererCommon.ScalingType DEFAULT_SCALING_TYPE
        = RendererCommon.ScalingType.SCALE_ASPECT_FIT;

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

        RendererCommon.ScalingType scalingType = DEFAULT_SCALING_TYPE;
        // TODO In order to achieve compliance with
        // https://www.w3.org/TR/html5/embedded-content-0.html#the-video-element
        // and the Web and iOS implementations, have a translucent background:
        //
        // Areas of the element's playback area that do not contain the video
        // represent nothing.
        localRender = VideoRendererGui.create(0, 0, 100, 100, scalingType, false);
    }

    public void setStream(MediaStream mediaStream) {
        if (mMediaStream != null) {
            mMediaStream.videoTracks.get(0).removeRenderer(mVideoRenderer);
        }
        mMediaStream = mediaStream;

        mVideoRenderer = new VideoRenderer(localRender);
        mediaStream.videoTracks.get(0).addRenderer(mVideoRenderer);

        RendererCommon.ScalingType scalingType = DEFAULT_SCALING_TYPE;
        VideoRendererGui.update(localRender, 0, 0, 100, 100, scalingType, false);
    }
}
