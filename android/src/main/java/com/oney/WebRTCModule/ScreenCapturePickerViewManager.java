package com.oney.WebRTCModule;

import android.view.View;

import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;

public class ScreenCapturePickerViewManager extends SimpleViewManager<View> {
    private static final String REACT_CLASS = "ScreenCapturePickerView";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public View createViewInstance(ThemedReactContext context) {
        return new View(context);
    }
}
