package com.oney.WebRTCModule.voip

import com.facebook.react.bridge.Promise
import com.google.firebase.installations.FirebaseInstallations

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
    private var token: String? = null

    @Volatile
    private var pendingIncoming: Incoming? = null

    @Volatile
    private var listener: Listener? = null

    /** Registered by WebRTCModule when a React instance comes up; cleared on teardown. */
    @Synchronized
    fun setListener(l: Listener?) {
        listener = l
    }

    @Synchronized
    fun updateToken(newToken: String) {
        if (token == newToken) {
            return
        }
        token = newToken
        listener?.onVoipToken(newToken)
    }

    @Synchronized
    fun getToken(): String? = token

    fun resolveToken(promise: Promise) {
        val cached = getToken()
        if (cached != null) {
            promise.resolve(cached)
        } else {
            fetchFid { promise.resolve(it) }
        }
    }

    private fun fetchFid(onResult: (String?) -> Unit) {
        try {
            FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(task.result)
                } else {
                    onResult(null)
                }
            }
        } catch (e: IllegalStateException) {
            onResult(null)
        }
    }

    @Synchronized
    fun reportIncoming(incoming: Incoming) {
        pendingIncoming = incoming
        listener?.onVoipIncoming(incoming)
    }

    @Synchronized
    fun pending(): Incoming? = pendingIncoming

    @Synchronized
    fun clearPending() {
        pendingIncoming = null
    }
}
