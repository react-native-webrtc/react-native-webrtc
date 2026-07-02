package com.oney.WebRTCModule.voip

import android.content.Context
import android.content.Intent
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallsManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.oney.WebRTCModule.AudioOutputManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
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
    private var lastCurrentEndpoint: CallEndpointCompat? = null
    private var lastEndpoints: List<CallEndpointCompat> = emptyList()
    private var audioOutputManager: AudioOutputManager? = null

    private var endpointJob: Job? = null
    private var availableJob: Job? = null
    private var muteJob: Job? = null

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
    private var displayName: String = ""

    fun hasActiveCall(): Boolean = hasActiveCall
    fun isAnswered(): Boolean = answered
    fun currentDisplayName(): String = displayName

    fun startOutgoingCall(ctx: Context, displayName: String, isVideo: Boolean) {
        register(ctx, displayName, isVideo, CallAttributesCompat.DIRECTION_OUTGOING)
    }

    fun reportIncomingCall(ctx: Context, displayName: String, isVideo: Boolean) {
        register(ctx, displayName, isVideo, CallAttributesCompat.DIRECTION_INCOMING)
    }

    fun answer() { actions?.trySend(CallAction.Answer) }
    fun setCallActive() { actions?.trySend(CallAction.Activate) }
    fun endCall() { actions?.trySend(CallAction.Disconnect(DisconnectCause(DisconnectCause.LOCAL))) }
    fun setListener(l: CallEventsListener?) { listener = l }

    fun setAudioOutputManager(manager: AudioOutputManager?) { audioOutputManager = manager }

    private fun ensureRegistered(context: Context) {
        CallNotificationManager.initChannels(context.applicationContext)

        if (callsManager == null) {
            callsManager = CallsManager(context.applicationContext)
        }

        if (!registered) {
            callsManager!!.registerAppWithTelecom(CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING or CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING)
            registered = true
        }
    }

    private fun register(ctx: Context, displayName: String, isVideo: Boolean, direction: Int) {
        ensureRegistered(ctx)

        if (hasActiveCall) return

        val channel = Channel<CallAction>()
        actions = channel
        hasActiveCall = true
        answered = false

        this.displayName = displayName
        val isIncoming = direction == CallAttributesCompat.DIRECTION_INCOMING
        val appContext = ctx.applicationContext
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
                    onAnswer = { _ ->
                        answered = true
                        CallNotificationManager.showOngoing(appContext, displayName)
                        listener?.onAnswered()
                    },
                    onDisconnect = { _ -> listener?.onEnded() },
                    onSetActive = { answered = true },
                    onSetInactive = { }
                ) {
                    listener?.onStarted()
                    if (isIncoming) CallNotificationManager.showIncoming(appContext, displayName, isVideo)
                    else CallNotificationManager.showOngoing(appContext, displayName)
                    audioOutputManager?.setTelecomOwnsRouting(true)
                    launch { processActions(channel.consumeAsFlow(), callType) }
                    endpointJob = launch { currentCallEndpoint.collect { endpoint ->
                        lastCurrentEndpoint = endpoint
                        audioOutputManager?.onTelecomAudioStateChanged(
                            endpoint.toWritableMap(),
                            lastEndpoints.map { it.toWritableMap() }.toWritableArray()
                        )
                    } }
                    availableJob = launch { availableEndpoints.collect { endpoints ->
                        lastEndpoints = endpoints
                        audioOutputManager?.onTelecomAudioStateChanged(
                            lastCurrentEndpoint?.toWritableMap(),
                            endpoints.map { it.toWritableMap() }.toWritableArray()
                        )
                    } }
                    muteJob = launch { isMuted.collect { listener?.onMuteChanged(it) } }
                    }
            } catch (e: Exception) { // should probably less generic
                listener?.onFailed(e.message ?: "addCall failed")
            } finally {
                hasActiveCall = false
                answered = false
                actions = null
                channel.close()
                audioOutputManager?.setTelecomOwnsRouting(false)

                CallNotificationManager.cancel(appContext)
                // Dismiss IncomingCallActivity if the call ended before the
                // user acted (remote hangup, timeout, answered elsewhere).
                appContext.sendBroadcast(
                    Intent(IncomingCallActivity.ACTION_CALL_ENDED).setPackage(appContext.packageName)
                )
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

    private fun CallEndpointCompat.toWritableMap(): WritableMap = Arguments.createMap().apply {
        putString("type", normalizedType())
        putString("nativeType", normalizedType())
        putString("name", name.toString())
        putString("id", identifier.toString())   // ParcelUuid -> String; AudioDevice.id is already a String
    }

    private fun List<WritableMap>.toWritableArray(): WritableArray {
        val array = Arguments.createArray()
        for (map in this) {
            array.pushMap(map)
        }
        return array
    }

    private fun CallEndpointCompat.normalizedType() = when (type) {
        CallEndpointCompat.TYPE_EARPIECE -> "earpiece"
        CallEndpointCompat.TYPE_SPEAKER -> "speaker"
        CallEndpointCompat.TYPE_BLUETOOTH -> "bluetooth"
        CallEndpointCompat.TYPE_WIRED_HEADSET -> "wiredHeadset"
        CallEndpointCompat.TYPE_STREAMING -> "streaming"   // watch/Auto — no AudioDeviceInfo analog
        else -> "unknown"
    }
}