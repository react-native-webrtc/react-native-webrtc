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
        val isVideo = data["isVideo"]?.toBoolean() ?: false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CallManager.reportIncomingCall(applicationContext, displayName, isVideo)
        }

        warmUpReact()
        VoipPushRegistry.reportIncoming(VoipPushRegistry.Incoming(roomName, displayName, isVideo))
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
