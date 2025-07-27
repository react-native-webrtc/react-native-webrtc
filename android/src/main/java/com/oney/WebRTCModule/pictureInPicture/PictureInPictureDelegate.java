package com.oney.WebRTCModule.pictureInPicture;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

public interface PictureInPictureDelegate {
    View getVideoRenderer();
    ViewGroup getVideoContainer();
    void requestVideoRenderUpdate();
    Context getContext();
}