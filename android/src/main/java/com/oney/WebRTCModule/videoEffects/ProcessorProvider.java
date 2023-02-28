package com.oney.WebRTCModule.videoEffects;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages VideoFrameProcessorFactoryInterfaces corresponding to name using hashmap, and provides
 * get, add and remove functionality.
 */
public class ProcessorProvider {
    private static Map<String, VideoFrameProcessorFactoryInterface> methodMap =
            new HashMap<String, VideoFrameProcessorFactoryInterface>();

    public static VideoFrameProcessor getProcessor(String name) {
        if (methodMap.containsKey(name)) {
            return methodMap.get(name).build();
        } else {
            return null;
        }
    }

    public static void addProcessor(
            String name, VideoFrameProcessorFactoryInterface videoFrameProcessorFactoryInterface) {
        if (name != null && videoFrameProcessorFactoryInterface != null) {
            methodMap.put(name, videoFrameProcessorFactoryInterface);
        } else {
            throw new NullPointerException("Name or VideoFrameProcessorFactry can not be null");
        }
    }

    public static void removeProcessor(String name) {
        if (name != null && methodMap.containsKey(name)) {
            methodMap.remove(name);
        } else {
            throw new RuntimeException("VideoFrameProcessorFactry with " + name + " does not exist");
        }
    }
}
