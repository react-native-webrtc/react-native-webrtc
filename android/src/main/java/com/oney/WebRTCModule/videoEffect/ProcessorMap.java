package com.oney.WebRTCModule.videoEffect;

import java.util.HashMap;
import java.util.Map;

public class ProcessorMap {
    private static Map<String, VideoFrameProcessorFactoryInterface> methodMap = new HashMap<String, VideoFrameProcessorFactoryInterface>();

    public static VideoFrameProcessor getProcessor(String name) {
        return methodMap.get(name).build();
    }

    public static void addProcessor(String name,
            VideoFrameProcessorFactoryInterface videoFrameProcessorFactoryInterface) {
        methodMap.put(name, videoFrameProcessorFactoryInterface);
    }

    public static void removeProcessor(String name) {
        methodMap.remove(name);
    }
}
