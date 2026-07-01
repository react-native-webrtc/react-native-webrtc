package com.oney.WebRTCModule.voip

import android.content.Context
import android.net.Uri
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

interface CallEventsListener {
    fun onStarted()
    fun onAnswered()
    fun onEnded()
    fun onFailed(reason: String)
    fun onMuteChanged(muted: Boolean)
}

@RequiresApi(value = 26)
object CallManager {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var callsManager: CallsManager? = null
    private var registered = false

    private var callScope: CoroutineScope? = null
    private var controlScope: CallControlScope? = null

    // Channel for communicating with Java
    private var actions: Channel<CallAction>? = null

    sealed interface CallAction {
        data object Answer : CallAction
        data object Activate : CallAction
        data object Hold : CallAction
        data class Disconnect(val cause: DisconnectCause) : CallAction
    }

    @Volatile private var hasActiveCall = false
    @Volatile private var answered = false
    private var listener: CallEventsListener? = null

    fun hasActiveCall(): Boolean = hasActiveCall
    fun isAnswered(): Boolean = answered

    // java facing
    fun startOutgoingCall(ctx: Context, displayName: String, isVideo: Boolean) {
        register(ctx, displayName, isVideo, CallAttributesCompat.DIRECTION_OUTGOING)
    }

    fun reportIncomingCall(ctx: Context, displayName: String, isVideo: Boolean) {
        register(ctx, displayName, isVideo, CallAttributesCompat.DIRECTION_INCOMING)
    }

    private fun ensureRegistered(context: Context) {
        if (callsManager == null) {
            callsManager = CallsManager(context.applicationContext)
        }

        if (!registered) {
            callsManager!!.registerAppWithTelecom(CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING or CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING)
        }
    }

    private fun register(ctx: Context, displayName: String, isVideo: Boolean, direction: Int) {
        ensureRegistered(ctx)

        if (hasActiveCall) return

        val channel = Channel<CallAction>()
        hasActiveCall = true
        answered = false

        val callType = if (isVideo) CallAttributesCompat.CALL_TYPE_VIDEO_CALL else CallAttributesCompat.CALL_TYPE_AUDIO_CALL

        val callAttributes = CallAttributesCompat(
            displayName = displayName,
            address = "sip:$displayName".toUri(),
            direction = direction,
            callType = callType,
            callCapabilities =
                CallAttributesCompat.SUPPORTS_SET_INACTIVE or // can be put on hold
                    CallAttributesCompat.SUPPORTS_STREAM or // can stream to other surfaces (watch, car)
                    CallAttributesCompat.SUPPORTS_TRANSFER // can be transferred between devices

        )

        scope.launch {
            try {
                callsManager!!.addCall(
                    callAttributes,
                    onAnswer = { _ -> answered = true; listener?.onAnswered() },
                    onDisconnect = { _ -> listener?.onEnded() },
                    onSetActive = { answered = true },
                    onSetInactive = { }
                ) {
                    listener?.onStarted()
                    launch { processActions(channel.consumeAsFlow(), callType) }
                    launch { currentCallEndpoint.collect {  } }
                    launch { availableEndpoints.collect {  } }
                    launch { isMuted.collect { listener?.onMuteChanged(it) } }
                    }
            } catch (e: Exception) { // should probably less generic
                listener?.onFailed(e.message ?: "addCall failed")
            } finally {
                hasActiveCall = false
                answered = false
                actions = null
                channel.close()
            }
        }
    }

    private suspend fun CallControlScope.processActions(src: Flow<CallAction>, callType: Int) {
        src.collect { action ->
            val result: CallControlResult = when (action) {
                CallAction.Answer -> answer(callType)
                CallAction.Activate -> setActive()
                CallAction.Hold -> setInactive()
                is CallAction.Disconnect -> { disconnect(action.cause) }
            }

            if (result is CallControlResult.Error) listener?.onFailed("telecom action failed: ${result.errorCode}")
        }
    }
}