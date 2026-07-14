package com.oney.WebRTCModule.voip

import com.oney.WebRTCModule.foregroundService.ForegroundServiceController

/**
 * Translates Core-Telecom call lifecycle transitions driven by [CallManager] into an
 * immutable [VoipForegroundRequest] and hands it to [ForegroundServiceController],
 * which owns that state and merges it with room media and screen-share
 * foreground-service requirements.
 */
object VoipForegroundServiceController {
    private val controller: ForegroundServiceController
        get() = ForegroundServiceController.getInstance()

    fun onCallConnecting(displayName: String?, isVideo: Boolean) {
        controller.setVoipRequest(
            VoipForegroundRequest.connecting(displayName.orEmpty(), isVideo),
        )
    }

    fun onCallConnected(displayName: String?, isVideo: Boolean) {
        controller.setVoipRequest(
            VoipForegroundRequest.connected(displayName.orEmpty(), isVideo, System.currentTimeMillis()),
        )
    }

    fun onCallEnded() {
        controller.setVoipRequest(VoipForegroundRequest.INACTIVE)
    }

    fun onCallHeld(held: Boolean) {
        controller.setVoipHeld(held)
    }
}
