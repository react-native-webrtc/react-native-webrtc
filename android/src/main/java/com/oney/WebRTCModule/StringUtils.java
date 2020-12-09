package com.oney.WebRTCModule;

import org.webrtc.PeerConnection;
import org.webrtc.RTCStats;
import org.webrtc.RTCStatsReport;

import java.util.Map;

public class StringUtils {
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
            builder
                .append("\"timestamp\":\"")
                .append(stats.getTimestampUs())
                .append("\",\"type\":\"")
                .append(stats.getType())
                .append("\",\"id\":\"")
                .append(stats.getId())
                .append("\"");

            for (Map.Entry<String, Object> entry : stats.getMembers().entrySet()) {
                builder
                    .append(",")
                    .append("\"")
                    .append(entry.getKey())
                    .append("\":");
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
        } else if (value instanceof String) {
            builder.append("\"").append(value).append("\"");
        } else {
            builder.append(value);
        }
    }
}