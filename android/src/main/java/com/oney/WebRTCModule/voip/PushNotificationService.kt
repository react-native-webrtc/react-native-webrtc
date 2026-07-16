package com.oney.WebRTCModule.voip

import android.os.Build
import android.os.Handler
import android.os.Looper
import com.facebook.react.ReactApplication
import com.facebook.react.internal.featureflags.ReactNativeFeatureFlags
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives VoIP wake-up pushes over FCM.
 *
 * The signaling backend sends a high-priority **data** message (so this runs even
 * when the app is backgrounded or killed, and gets the temporary foreground-service
 * start exemption) with `roomName` / `displayName` / `isVideo`. We report the call
 * to Core-Telecom immediately so it rings without needing JS, and hand the room
 * details to [VoipPushRegistry] for the JS layer.
 */
class PushNotificationService : FirebaseMessagingService() {
    override fun onRegistered(token: String) {
        VoipPushRegistry.updateToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val roomName = data["roomName"] ?: return
        val displayName = data["displayName"] ?: "Incoming call"
        val handle = data["handle"]?.takeIf { it.isNotEmpty() } ?: displayName
        val isVideo = data["isVideo"]?.toBoolean() ?: false
        val avatarUrl = data["avatarUrl"]?.takeIf { it.isNotEmpty() }
        val incoming = VoipPushRegistry.Incoming(roomName, displayName, handle, isVideo, avatarUrl)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (CallManager.reportIncomingCall(applicationContext, displayName, handle, isVideo, avatarUrl)) {
                IncomingCallSlot.CURRENT -> {
                    warmUpReact()
                    VoipPushRegistry.reportIncoming(incoming)
                }
                IncomingCallSlot.WAITING -> {
                    warmUpReact()
                    VoipPushRegistry.bufferWaitingIncoming(incoming)
                }
                IncomingCallSlot.REJECTED -> {
                    // Already tracking two calls - drop it, nothing to notify JS about.
                }
            }
        } else {
            warmUpReact()
            VoipPushRegistry.reportIncoming(incoming)
        }
    }

    private fun warmUpReact() {
        val app = applicationContext as? ReactApplication ?: return
        Handler(Looper.getMainLooper()).post {
            try {
                if (ReactNativeFeatureFlags.enableBridgelessArchitecture()) {
                    app.reactHost?.start()
                } else {
                    val manager = app.reactNativeHost.reactInstanceManager
                    if (!manager.hasStartedCreatingInitialContext()) {
                        manager.createReactContextInBackground()
                    }
                }
            } catch (e: Exception) {
                // Ignore React warm-up failures on push
            }
        }
    }
}
