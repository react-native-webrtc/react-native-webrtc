package com.oney.WebRTCModule.voip

/**
 * Process-wide bridge between [PushNotificationService] (which can run with no
 * React instance alive, e.g. a push received while the app is killed) and the
 * JS layer.
 */
object VoipPushRegistry {
    data class Incoming(val roomName: String, val displayName: String, val isVideo: Boolean)

    interface Listener {
        fun onVoipToken(token: String)
        fun onVoipIncoming(incoming: Incoming)
    }

    @Volatile
    var token: String? = null
        private set

    @Volatile
    private var pendingIncoming: Incoming? = null

    @Volatile
    private var listener: Listener? = null

    /** Registered by WebRTCModule when a React instance comes up; cleared on teardown. */
    @Synchronized
    fun setListener(l: Listener?) {
        listener = l
        if (l != null) token?.let(l::onVoipToken)
    }

    @Synchronized
    fun updateToken(newToken: String) {
        token = newToken
        listener?.onVoipToken(newToken)
    }

    @Synchronized
    fun reportIncoming(incoming: Incoming) {
        val l = listener
        if (l != null) l.onVoipIncoming(incoming) else pendingIncoming = incoming
    }

    @Synchronized
    fun pending(): Incoming? = pendingIncoming

    @Synchronized
    fun clearPending() {
        pendingIncoming = null
    }
}
