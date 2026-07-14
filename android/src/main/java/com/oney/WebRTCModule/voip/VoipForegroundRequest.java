package com.oney.WebRTCModule.voip;

/**
 * Immutable snapshot of the call-driven foreground-service requirements: which CallStyle
 * notification variant to show and which media types the call itself needs (as opposed to
 * room camera/microphone usage requested from JS). Built by VoipForegroundServiceController
 * and handed to {@code ForegroundServiceController}, which owns it and merges it with
 * room/screen-share requirements before starting {@code WebRTCForegroundService}.
 */
public final class VoipForegroundRequest {
    public static final VoipForegroundRequest INACTIVE = new VoipForegroundRequest(false, false, false, "", false, 0L);

    private final boolean active;
    private final boolean connecting;
    private final boolean held;
    private final String displayName;
    private final boolean video;
    private final long connectedAtMs;

    private VoipForegroundRequest(
            boolean active, boolean connecting, boolean held, String displayName, boolean video, long connectedAtMs) {
        this.active = active;
        this.connecting = connecting;
        this.held = held;
        this.displayName = displayName;
        this.video = video;
        this.connectedAtMs = connectedAtMs;
    }

    public static VoipForegroundRequest connecting(String displayName, boolean video) {
        return new VoipForegroundRequest(true, true, false, displayName, video, 0L);
    }

    public static VoipForegroundRequest connected(String displayName, boolean video, long connectedAtMs) {
        return new VoipForegroundRequest(true, false, false, displayName, video, connectedAtMs);
    }

    public VoipForegroundRequest withHeld(boolean held) {
        return new VoipForegroundRequest(active, connecting, held, displayName, video, connectedAtMs);
    }

    public boolean isActive() {
        return active;
    }

    public boolean isConnecting() {
        return connecting;
    }

    public boolean isHeld() {
        return held;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getConnectedAtMs() {
        return connectedAtMs;
    }

    public boolean needsCamera() {
        return active && video;
    }

    public boolean needsMicrophone() {
        return active;
    }
}
