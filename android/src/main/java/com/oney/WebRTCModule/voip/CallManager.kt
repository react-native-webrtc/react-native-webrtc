package com.oney.WebRTCModule.voip

import android.content.Context
import android.content.Intent
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallException
import androidx.core.telecom.CallsManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.oney.WebRTCModule.AudioOutputManager
import com.oney.WebRTCModule.foregroundService.ForegroundServiceController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface CallEventsListener {
    fun onStarted()
    fun onAnswered(requestId: String)
    fun onEnded(reason: String)
    fun onFailed(reason: String)
    fun onMuteChanged(muted: Boolean)
}

@RequiresApi(value = 26)
object CallManager {
    private const val DEFAULT_INCOMING_CALL_TIMEOUT_MS = 45_000L
    private const val DEFAULT_OUTGOING_CALL_TIMEOUT_MS = 60_000L
    private const val DEFAULT_FULFILL_ANSWER_TIMEOUT_MS = 10_000L

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var callsManager: CallsManager? = null
    private var registered = false
    private val callNotificationManager = CallNotificationManager()
    private var lastCurrentEndpoint: CallEndpointCompat? = null
    private var lastEndpoints: List<CallEndpointCompat> = emptyList()
    private var audioOutputManager: AudioOutputManager? = null

    private var ringTimeoutJob: Job? = null

    private var actions: Channel<CallAction>? = null

    sealed interface CallAction {
        data object Answer : CallAction
        data object Activate : CallAction
        data object Hold : CallAction
        data class SetEndpoint(val endpoint: CallEndpointCompat) : CallAction
        data class Disconnect(val cause: DisconnectCause) : CallAction
    }

    @Volatile private var hasActiveCall = false
    @Volatile private var answered = false
    @Volatile private var isOutgoing = false
    @Volatile private var pendingAnswerRequestId: String? = null
    private var appContext: Context? = null
    private var listener: CallEventsListener? = null
    private var displayName: String = ""
    private var videoCall: Boolean = false
    private var timeoutsLoaded = false
    private var incomingCallTimeoutMs = DEFAULT_INCOMING_CALL_TIMEOUT_MS
    private var outgoingCallTimeoutMs = DEFAULT_OUTGOING_CALL_TIMEOUT_MS
    private var fulfillAnswerTimeoutMs = DEFAULT_FULFILL_ANSWER_TIMEOUT_MS

    fun hasActiveCall(): Boolean = hasActiveCall
    fun isAnswered(): Boolean = answered
    fun pendingAnswerRequestId(): String? = pendingAnswerRequestId
    fun currentDisplayName(): String = displayName
    fun currentIsVideo(): Boolean = videoCall

    fun startOutgoingCall(ctx: Context, displayName: String, handle: String, isVideo: Boolean) {
        register(ctx, displayName, handle, isVideo, CallAttributesCompat.DIRECTION_OUTGOING)
    }

    fun reportIncomingCall(ctx: Context, displayName: String, handle: String, isVideo: Boolean) {
        register(ctx, displayName, handle, isVideo, CallAttributesCompat.DIRECTION_INCOMING)
    }

    fun answer() { actions?.trySend(CallAction.Answer) }
    private fun setCallActive() { actions?.trySend(CallAction.Activate) }

    fun fulfillAnswered(requestId: String): Boolean {
        if (!FulfillRequestManager.fulfill(requestId)) return false
        if (pendingAnswerRequestId == requestId) {
            pendingAnswerRequestId = null
        }
        markConnected()
        return true
    }

    fun reportOutgoingCallConnected() {
        if (!hasActiveCall || !isOutgoing) return
        markConnected()
    }

    private fun markConnected() {
        setCallActive()
        showOngoingNotification()
    }

    fun failAnswered(requestId: String) {
        if (!FulfillRequestManager.cancel(requestId)) return
        if (pendingAnswerRequestId == requestId) {
            pendingAnswerRequestId = null
        }
        endCall(DisconnectCause(DisconnectCause.ERROR))
    }

    fun endCall(cause: DisconnectCause = DisconnectCause(DisconnectCause.LOCAL)) {
        actions?.trySend(CallAction.Disconnect(cause))
    }

    fun setListener(l: CallEventsListener?) { listener = l }

    fun reasonToCause(reason: String): DisconnectCause = when (reason) {
        "local" -> DisconnectCause(DisconnectCause.LOCAL)
        "rejected" -> DisconnectCause(DisconnectCause.REJECTED)
        "missed" -> DisconnectCause(DisconnectCause.MISSED)
        "remote" -> DisconnectCause(DisconnectCause.REMOTE)
        "answeredElsewhere" -> DisconnectCause(DisconnectCause.ANSWERED_ELSEWHERE)
        "failed" -> DisconnectCause(DisconnectCause.ERROR)
        else -> DisconnectCause(DisconnectCause.LOCAL)
    }

    private fun causeToReason(cause: DisconnectCause): String = when (cause.code) {
        DisconnectCause.LOCAL -> "local"
        DisconnectCause.REJECTED -> "rejected"
        DisconnectCause.MISSED -> "missed"
        DisconnectCause.REMOTE -> "remote"
        DisconnectCause.ANSWERED_ELSEWHERE -> "answeredElsewhere"
        DisconnectCause.ERROR -> "failed"
        else -> "remote"
    }

    fun setAudioOutputManager(manager: AudioOutputManager?) {
        audioOutputManager = manager
        // On cold start the call is registered from the FCM push before React
        // attaches the manager, so it missed setTelecomOwnsRouting(true) and
        // the endpoint updates emitted inside the addCall block. Without this
        // replay, selectAudioOutput falls through to AudioManager-based
        // routing, which the platform ignores while Telecom owns the call.
        if (manager != null && hasActiveCall) {
            manager.setTelecomOwnsRouting(true)
            manager.onTelecomAudioStateChanged(
                lastCurrentEndpoint?.toWritableMap(),
                lastEndpoints.map { it.toWritableMap() }.toWritableArray(),
            )
        }
    }

    fun selectEndpoint(id: String): Boolean {
        val endpoint = lastEndpoints.firstOrNull { it.identifier.toString() == id } ?: return false
        return actions?.trySend(CallAction.SetEndpoint(endpoint))?.isSuccess == true
    }

    /** Fresh serialization of the last-known telecom endpoints for bridge consumers. */
    fun availableEndpointsSnapshot(): WritableArray = lastEndpoints.map { it.toWritableMap() }.toWritableArray()

    @SuppressLint("MissingPermission")
    private fun ensureRegistered(context: Context) {
        loadTimeouts(context)
        callNotificationManager.initChannels(context.applicationContext)

        if (callsManager == null) {
            callsManager = CallsManager(context.applicationContext)
        }

        if (!registered) {
            callsManager!!.registerAppWithTelecom(CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING or CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING)
            registered = true
        }
    }

    private fun loadTimeouts(context: Context) {
        if (timeoutsLoaded) return
        timeoutsLoaded = true
        incomingCallTimeoutMs = readTimeoutMs(context, "FishjamVoipIncomingCallTimeout", DEFAULT_INCOMING_CALL_TIMEOUT_MS)
        outgoingCallTimeoutMs = readTimeoutMs(context, "FishjamVoipOutgoingCallTimeout", DEFAULT_OUTGOING_CALL_TIMEOUT_MS)
        fulfillAnswerTimeoutMs = readTimeoutMs(context, "FishjamVoipFulfillAnswerTimeout", DEFAULT_FULFILL_ANSWER_TIMEOUT_MS)
    }

    /** Reads a manifest meta-data value in seconds; returns milliseconds. */
    private fun readTimeoutMs(context: Context, key: String, defaultMs: Long): Long = try {
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA,
        )
        val seconds = appInfo.metaData?.getInt(key, (defaultMs / 1000).toInt())
            ?: (defaultMs / 1000).toInt()
        if (seconds > 0) seconds * 1000L else defaultMs
    } catch (_: Exception) {
        defaultMs
    }

    @Synchronized
    @SuppressLint("MissingPermission")
    private fun register(ctx: Context, displayName: String, handle: String, isVideo: Boolean, direction: Int) {
        ensureRegistered(ctx)

        if (hasActiveCall) return

        val channel = Channel<CallAction>(Channel.BUFFERED)
        actions = channel
        hasActiveCall = true
        answered = false

        this.displayName = displayName
        this.videoCall = isVideo
        val isIncoming = direction == CallAttributesCompat.DIRECTION_INCOMING
        isOutgoing = direction == CallAttributesCompat.DIRECTION_OUTGOING
        val appContext = ctx.applicationContext
        this.appContext = appContext
        val callType = if (isVideo) CallAttributesCompat.CALL_TYPE_VIDEO_CALL else CallAttributesCompat.CALL_TYPE_AUDIO_CALL
        val callAttributes = CallAttributesCompat(
            displayName = displayName,
            address = "sip:${Uri.encode(handle)}".toUri(),
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
                    // Fires ONLY for external answer requests (system UI, Auto,
                    // Bluetooth, watch). App-initiated answers reach handleAnswered
                    // via processActions instead.
                    onAnswer = { _ ->
                        handleAnswered()
                        launchHostApp(appContext)
                    },
                    onDisconnect = { cause ->
                        FulfillRequestManager.cancelAll()
                        pendingAnswerRequestId = null
                        listener?.onEnded(causeToReason(cause))
                    },
                    onSetActive = {
                        answered = true
                        cancelRingTimeout()
                    },
                    onSetInactive = { }
                ) {
                    listener?.onStarted()
                    if (isIncoming) {
                        callNotificationManager.showIncoming(ctx.applicationContext, displayName, isVideo)
                        startRingTimeout(incomingCallTimeoutMs)
                    } else {
                        showConnectingNotification()
                        startRingTimeout(outgoingCallTimeoutMs)
                    }
                    audioOutputManager?.setTelecomOwnsRouting(true)
                    launch { processActions(channel.consumeAsFlow(), callType) }
                    launch { currentCallEndpoint.collect { endpoint ->
                        lastCurrentEndpoint = endpoint
                        audioOutputManager?.onTelecomAudioStateChanged(
                            endpoint.toWritableMap(),
                            lastEndpoints.map { it.toWritableMap() }.toWritableArray()
                        )
                    } }
                    launch { availableEndpoints.collect { endpoints ->
                        lastEndpoints = endpoints
                        audioOutputManager?.onTelecomAudioStateChanged(
                            lastCurrentEndpoint?.toWritableMap(),
                            endpoints.map { it.toWritableMap() }.toWritableArray()
                        )
                    } }
                    launch { isMuted.collect { listener?.onMuteChanged(it) } }
                    }
            } catch (e: CancellationException) {
                // Never swallow coroutine cancellation — let it propagate so the
                // parent scope tears down cleanly.
                throw e
            } catch (e: CallException) {
                listener?.onFailed(e.message ?: "addCall failed (code ${e.code})")
            } catch (e: UnsupportedOperationException) {
                listener?.onFailed(e.message ?: "Telecom not supported on this device")
            } finally {
                cancelRingTimeout()
                FulfillRequestManager.cancelAll()
                pendingAnswerRequestId = null
                hasActiveCall = false
                answered = false
                isOutgoing = false
                actions = null
                channel.close()
                audioOutputManager?.setTelecomOwnsRouting(false)
                LockScreenController.onCallEnded()
                ForegroundServiceController.getInstance().onCallEnded()
                callNotificationManager.cancel(ctx.applicationContext)
                VoipPushRegistry.clearPending()
                // Dismiss IncomingCallActivity if the call ended before the
                // user acted (remote hangup, timeout, answered elsewhere).
                ctx.applicationContext.sendBroadcast(
                    Intent(IncomingCallActivity.ACTION_CALL_ENDED).setPackage(appContext.packageName)
                )
            }
        }
    }

    private suspend fun CallControlScope.processActions(
        src: Flow<CallAction>,
        callType: Int,
    ) {
        src.collect { action ->
            val result: CallControlResult = when (action) {
                CallAction.Answer -> answer(callType)
                CallAction.Activate -> setActive()
                CallAction.Hold -> setInactive()
                is CallAction.SetEndpoint -> requestEndpointChange(action.endpoint)
                is CallAction.Disconnect -> { disconnect(action.cause) }
            }

            if (result is CallControlResult.Error) {
                listener?.onFailed("telecom action failed: ${result.errorCode}")
            } else if (action == CallAction.Answer) {
                // App-initiated answer succeeded — onAnswer won't fire for this,
                // so run the post-answer side effects here.
                handleAnswered()
            } else if (action == CallAction.Activate) {
                answered = true
                cancelRingTimeout()
            } else if (action is CallAction.Disconnect) {
                listener?.onEnded(causeToReason(action.cause))
                this@processActions.cancel()
            }
        }
    }

    private fun launchHostApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.putExtra(LockScreenController.VOIP_ANSWER, true)
        context.startActivity(intent)
    }

    /** Post-answer side effects shared by external (onAnswer) and app-initiated answers. */
    private fun handleAnswered() {
        cancelRingTimeout()
        if (pendingAnswerRequestId != null) return
        answered = true
        callNotificationManager.stopVibration()
        appContext?.let { LockScreenController.onCallAnswered(it) }
        showConnectingNotification()

        val requestId = FulfillRequestManager.createRequest(fulfillAnswerTimeoutMs) { timedOutRequestId ->
            if (pendingAnswerRequestId != timedOutRequestId) return@createRequest
            pendingAnswerRequestId = null
            listener?.onFailed("answer fulfill timed out")
            endCall(DisconnectCause(DisconnectCause.ERROR))
        }
        pendingAnswerRequestId = requestId
        listener?.onAnswered(requestId)
    }

    private fun startRingTimeout(timeoutMs: Long) {
        cancelRingTimeout()
        ringTimeoutJob = scope.launch {
            delay(timeoutMs)
            if (!hasActiveCall || answered) return@launch
            endCall(DisconnectCause(DisconnectCause.MISSED))
        }
    }

    private fun cancelRingTimeout() {
        ringTimeoutJob?.cancel()
        ringTimeoutJob = null
    }

    private fun showConnectingNotification() {
        ForegroundServiceController.getInstance().onCallConnecting(displayName, videoCall)
    }

    private fun showOngoingNotification() {
        ForegroundServiceController.getInstance().onCallConnected(displayName, videoCall)
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