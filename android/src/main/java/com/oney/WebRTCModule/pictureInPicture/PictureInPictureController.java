package com.oney.WebRTCModule.pictureInPicture;

import android.app.Activity;
import android.util.Rational;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import java.util.ArrayList;

public class PictureInPictureController implements PictureInPictureHelperListener {
    @Nullable
    ReactContext reactContext;

    @Nullable
    Activity currentActivity;

    @Nullable
    ViewGroup rootViewGroup;

    PictureInPictureDelegate delegate;

    /**
     * Helper tag to safely attach and detach PictureInPictureHelperFragment
     */
    @Nullable
    private String pictureInPictureHelperTag;

    /**
     * Save the rootView's children original visibility state.
     */
    private final ArrayList<Integer> rootViewChildrenOriginalVisibility = new ArrayList<>();

    /**
     * Event name to send to onPictureInPictureChange callback.
     */
    public static String onPictureInPictureChangeEventName = "onPictureInPictureChange";

    public PictureInPictureController(PictureInPictureDelegate delegate) {
        this.delegate = delegate;
        this.reactContext = (ReactContext) delegate.getContext();
        this.currentActivity = reactContext.getCurrentActivity();

        if (this.currentActivity != null) {
            this.rootViewGroup = this.currentActivity.getWindow().getDecorView().findViewById(android.R.id.content);
        }

        attachPictureInPictureHelperFragment();
    }

    public void setPIPOptions(ReadableMap map) {
        boolean startAutomatically = true;

        if (map.hasKey("startAutomatically")) {
            startAutomatically = map.getBoolean("startAutomatically");
        }

        setAutoEnter(startAutomatically);
        setPreferredSize(map.getMap("preferredSize"));
    }

    void setAutoEnter(Boolean autoEnter) {
        PictureInPictureUtils.applyAutoEnter(currentActivity, autoEnter);
    }

    void setDefaultAspectRatio() {
        PictureInPictureUtils.applyAspectRatio(currentActivity, new Rational(150, 200));
    }

    void setPreferredSize(@Nullable ReadableMap size) {
        if (size == null) {
            setDefaultAspectRatio();
            return;
        }

        if (!size.hasKey("width") || size.isNull("width") || !size.hasKey("height") || size.isNull("height")) {
            setDefaultAspectRatio();
            return;
        }

        Rational aspectRatio = new Rational(size.getInt("width"), size.getInt("height"));

        if (aspectRatio.isNaN()) {
            setDefaultAspectRatio();
            return;
        }

        PictureInPictureUtils.applyAspectRatio(currentActivity, aspectRatio);
    }

    public void enterPictureInPicture() {
        if (currentActivity == null) return;
        PictureInPictureUtils.safeEnterPictureInPicture(currentActivity);
    }

    protected void layoutForPipEnter() {
        if (rootViewGroup == null) return;

        delegate.getVideoContainer().removeView(delegate.getVideoRenderer());

        for (int i = 0; i < rootViewGroup.getChildCount(); i++) {
            View child = rootViewGroup.getChildAt(i);
            if (child != delegate.getVideoRenderer()) {
                rootViewChildrenOriginalVisibility.add(child.getVisibility());
                child.setVisibility(View.GONE);
            }
        }

        rootViewGroup.addView(delegate.getVideoRenderer(),
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void layoutForPipExit() {
        if (rootViewGroup == null) return;

        rootViewGroup.removeView(delegate.getVideoRenderer());

        for (int i = 0; i < rootViewGroup.getChildCount(); i++) {
            rootViewGroup.getChildAt(i).setVisibility(rootViewChildrenOriginalVisibility.get(i));
        }

        rootViewChildrenOriginalVisibility.clear();

        delegate.getVideoContainer().addView(delegate.getVideoRenderer(),
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        delegate.requestVideoRenderUpdate();
    }

    public void attachPictureInPictureHelperFragment() {
        if (currentActivity == null) return;

        if (currentActivity instanceof FragmentActivity) {
            FragmentActivity fragmentActivity = (FragmentActivity) currentActivity;
            PictureInPictureHelperFragment fragment = new PictureInPictureHelperFragment();
            pictureInPictureHelperTag = fragment.id;
            fragment.setListener(this);
            fragmentActivity.getSupportFragmentManager().beginTransaction().add(fragment, fragment.id).commit();
        }
    }

    public void detachPictureInPictureHelperFragment() {
        if (currentActivity == null) return;

        if (currentActivity instanceof FragmentActivity) {
            FragmentActivity fragmentActivity = (FragmentActivity) currentActivity;
            Fragment fragment =
                    fragmentActivity.getSupportFragmentManager().findFragmentByTag(pictureInPictureHelperTag);
            if (fragment != null) {
                fragmentActivity.getSupportFragmentManager()
                        .beginTransaction()
                        .remove(fragment)
                        .commitAllowingStateLoss();
            }
        }
        PictureInPictureUtils.applyAutoEnter(currentActivity, false);
    }

    private void sendPictureInPictureModeChangeEvent(Boolean isInPictureInPictureMode) {
        if (reactContext == null) return;

        WritableMap event = Arguments.createMap();
        event.putBoolean("isInPictureInPicture", isInPictureInPictureMode);

        reactContext.getJSModule(RCTEventEmitter.class)
                .receiveEvent(delegate.getVideoContainer().getId(), onPictureInPictureChangeEventName, event);
    }

    @Override
    public void onPictureInPictureModeChange(Boolean isInPictureInPictureMode) {
        sendPictureInPictureModeChangeEvent(isInPictureInPictureMode);

        if (isInPictureInPictureMode) {
            layoutForPipEnter();
        } else {
            layoutForPipExit();
        }
    }
}