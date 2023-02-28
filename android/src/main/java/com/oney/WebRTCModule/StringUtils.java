package com.oney.WebRTCModule;

import android.util.Log;

import org.webrtc.PeerConnection;
import org.webrtc.RTCStats;
import org.webrtc.RTCStatsReport;

import java.util.Map;

public class StringUtils {
    private static final String TAG = StringUtils.class.getSimpleName();

    /**
     * Constructs a JSON <tt>String</tt> representation of a specific array of
     * <tt>RTCStatsReport</tt>s (produced by {@link PeerConnection#getStats}).
     * <p>
     * @param report the <tt>RTCStatsReport</tt>s to represent in JSON
     *               format
     * @return a <tt>String</tt> which represents the specified <tt>report</tt>
     * in JSON format
     */
    public static String statsToJSON(RTCStatsReport report) {
        StringBuilder builder = new StringBuilder("[");

        boolean firstKey = true;

        Map<String, RTCStats> statsMap = report.getStatsMap();

        for (String key : report.getStatsMap().keySet()) {
            if (firstKey) {
                firstKey = false;
            } else {
                builder.append(",");
            }

            builder.append("[\"").append(key).append("\",{");

            RTCStats stats = statsMap.get(key);
            builder.append("\"timestamp\":")
                    .append(stats.getTimestampUs() / 1000.0)
                    .append(",\"type\":\"")
                    .append(stats.getType())
                    .append("\",\"id\":\"")
                    .append(stats.getId())
                    .append("\"");

            for (Map.Entry<String, Object> entry : stats.getMembers().entrySet()) {
                builder.append(",").append("\"").append(entry.getKey()).append("\":");
                appendValue(builder, entry.getValue());
            }

            builder.append("}]");
        }

        builder.append("]");

        return builder.toString();
    }

    private static void appendValue(StringBuilder builder, Object value) {
        if (value instanceof Object[]) {
            Object[] arrayValue = (Object[]) value;
            builder.append("[");

            for (int i = 0; i < arrayValue.length; ++i) {
                if (i != 0) {
                    builder.append(",");
                }

                appendValue(builder, arrayValue[i]);
            }

            builder.append("]");
        } else if (value instanceof Map) {
            try {
                Map<String, Object> mapValue = (Map) value;

                boolean firstKey = true;
                builder.append("{");

                for (Map.Entry<String, Object> entry : mapValue.entrySet()) {
                    if (firstKey) {
                        firstKey = false;
                    } else {
                        builder.append(",");
                    }
                    builder.append("\"").append(entry.getKey()).append("\":");
                    appendValue(builder, entry.getValue());
                }
                builder.append("}");
            } catch (ClassCastException e) {
                Log.e(TAG, "Error parsing stats value " + value);
            }
        } else if (value instanceof String) {
            builder.append("\"").append(value).append("\"");
        } else {
            builder.append(value);
        }
    }
}