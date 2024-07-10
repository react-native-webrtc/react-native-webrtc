package com.oney.rnwebrtc;

import androidx.annotation.Nullable;

import com.facebook.react.TurboReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.react.module.model.ReactModuleInfo;
import com.facebook.react.module.model.ReactModuleInfoProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebRTCModulePackage extends TurboReactPackage {

    @Nullable
    @Override
    public NativeModule getModule(String name, ReactApplicationContext context) {
        if (name.equals(WebRTCModuleImpl.NAME)) {
            return new WebRTCModule(context);
        } else {
            return null;
        }
    }

    @Override
    public ReactModuleInfoProvider getReactModuleInfoProvider() {
        return () -> {
            final Map<String, ReactModuleInfo> moduleInfos = new HashMap<>();
            boolean isTurboModule = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED;

            ReactModuleInfo moduleInfo = new ReactModuleInfo(
                WebRTCModuleImpl.NAME,
                WebRTCModuleImpl.NAME,
                false, // canOverrideExistingModule
                false, // needsEagerInit
                true, // hasConstants
                false, // isCxxModule
                isTurboModule // isTurboModule
            );

            moduleInfos.put(WebRTCModuleImpl.NAME, moduleInfo);
            return moduleInfos;
        };
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext context) {
        List<ViewManager> viewManagers = new ArrayList<>();

        viewManagers.add(new RTCVideoViewManager(context));
        return viewManagers;
    }
}
