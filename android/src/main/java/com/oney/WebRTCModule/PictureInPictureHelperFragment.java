package com.oney.WebRTCModule;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.UUID;

public class PictureInPictureHelperFragment extends Fragment {
    String id = "PictureInPictureHelperFragment"+ UUID.randomUUID().toString();

    @Nullable
    private PictureInPictureHelperListener listener;

    void setListener(PictureInPictureHelperListener listener){
        this.listener = listener;
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        if(listener!= null){
            listener.onPictureInPictureModeChange(isInPictureInPictureMode);
        }
    }
}


interface PictureInPictureHelperListener {
    void onPictureInPictureModeChange(Boolean isInPictureInPictureMode);
}