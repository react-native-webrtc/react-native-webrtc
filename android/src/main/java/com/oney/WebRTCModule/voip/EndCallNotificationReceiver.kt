package com.oney.WebRTCModule.voip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Handles the notification actions that only need to end the call and don't
 * have to bring an Activity forward (Decline on an incoming call, Hang up on an
 * ongoing one). Both route to [CallManager.endCall].
 */
@RequiresApi(Build.VERSION_CODES.O)
class EndCallNotificationReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_DECLINE = "fishjam.voip.ACTION_DECLINE"
        const val ACTION_HANGUP = "fishjam.voip.ACTION_HANGUP"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DECLINE, ACTION_HANGUP -> CallManager.endCall()
        }
    }
}
