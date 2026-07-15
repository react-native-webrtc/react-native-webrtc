package com.oney.WebRTCModule.voip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi

/**
 * Handles notification actions without bringing an Activity forward: decline
 * and hang up, plus answering/declining a waiting (second) call.
 */
@RequiresApi(Build.VERSION_CODES.O)
class EndCallNotificationReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_DECLINE = "fishjam.voip.ACTION_DECLINE"
        const val ACTION_HANGUP = "fishjam.voip.ACTION_HANGUP"
        const val ACTION_ANSWER_WAITING = "fishjam.voip.ACTION_ANSWER_WAITING"
        const val ACTION_DECLINE_WAITING = "fishjam.voip.ACTION_DECLINE_WAITING"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DECLINE -> CallManager.endCall(DisconnectCause(DisconnectCause.REJECTED))
            ACTION_HANGUP -> CallManager.endCall(DisconnectCause(DisconnectCause.LOCAL))
            ACTION_ANSWER_WAITING -> CallManager.acceptWaitingCall(context)
            ACTION_DECLINE_WAITING -> CallManager.declineWaitingCall(context)
        }
    }
}
