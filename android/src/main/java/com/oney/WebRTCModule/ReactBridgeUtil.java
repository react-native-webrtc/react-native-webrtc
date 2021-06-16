package com.oney.WebRTCModule;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;

public class ReactBridgeUtil {

    /**
     * Reads a value from given <tt>ReadableMap</tt> and returns it as
     * a <tt>String</tt>. Note that integer value is converted to double, before
     * it gets converted to a string.
     * @param map the <tt>ReadableMap</tt> from which the value will be obtained
     * @param key the map's key under which the value has been mapped.
     * @return a <tt>String</tt> representation of the value if exists or
     * <tt>null</tt> if there is no value mapped for given <tt>key</tt>.
     */
    public static String getMapStrValue(ReadableMap map, String key) {
        if(!map.hasKey(key)){
            return null;
        }
        ReadableType type = map.getType(key);
        switch (type) {
            case Boolean:
                return String.valueOf(map.getBoolean(key));
            case Number:
                // Don't know how to distinguish between Int and Double from
                // ReadableType.Number. 'getInt' will fail on double value,
                // while 'getDouble' works for both.
                // return String.valueOf(map.getInt(key));
                return String.valueOf(map.getDouble(key));
            case String:
                return map.getString(key);
            default:
                return null;
        }
    }
}