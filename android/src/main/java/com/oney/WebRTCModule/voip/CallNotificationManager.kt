package com.oney.WebRTCModule.voip

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat

/**
 * Builds and posts the single CallStyle notification that keeps the app in
 * foreground-execution priority for the lifetime of a Core-Telecom call.
 *
 * Core-Telecom grants foreground priority only while a valid CallStyle
 * notification is posted within ~5s of [CallsManager.addCall] and stays valid
 * until the call ends, so [CallManager] posts here as soon as the call is
 * accepted and cancels in its `finally` block.
 *
 * One notification (id [NOTIFICATION_ID]) transitions incoming -> ongoing:
 *  - incoming is posted via notify() with a full-screen intent (rings over
 *    the lock screen; the FSI also makes the CallStyle valid on Android 14+).
 *  - ongoing is built by [buildOngoing] and posted by
 *    WebRTCForegroundService.startForeground() under the same id, so being
 *    FGS-attached is what keeps it valid — no full-screen intent needed, and
 *    the service's mediaProjection type covers screen share, the one
 *    capability Telecom's foreground delegation does not include.
 */
@RequiresApi(26)
class CallNotificationManager {
    companion object {
        private const val CHANNEL_INCOMING = "fishjam_telecom_incoming"
        private const val CHANNEL_ONGOING = "fishjam_telecom_ongoing"
        const val NOTIFICATION_ID = 8400

        // Distinct request codes so the PendingIntents don't collapse into one.
        private const val RC_ANSWER = 1
        private const val RC_DECLINE = 2
        private const val RC_HANGUP = 3
        private const val RC_FULL_SCREEN = 4
        private const val RC_CONTENT = 5

        private val RING_VIBRATION_PATTERN = longArrayOf(0, 350, 200, 350, 1200)
    }

    private val ringToneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    private var channelsReady = false
    private var vibrator: Vibrator? = null

    fun initChannels(context: Context) {
        if (channelsReady) return
        val nm = NotificationManagerCompat.from(context)

        val incomingChannel = NotificationChannelCompat.Builder(
            CHANNEL_INCOMING,
            NotificationManagerCompat.IMPORTANCE_HIGH,
        ).setName("Incoming calls")
            .setDescription("Handles the notifications when receiving a call")
            .setVibrationEnabled(false).setSound(
                ringToneUri,
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setLegacyStreamType(AudioManager.STREAM_RING)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build(),
            ).build()

        val ongoingChannel = NotificationChannelCompat.Builder(
            CHANNEL_ONGOING,
            NotificationManagerCompat.IMPORTANCE_DEFAULT,
        )
            .setName("Ongoing calls")
            .setDescription("Displays the ongoing call notifications")
            .setSound(null, null)
            .setVibrationEnabled(false)
            .build()

        nm.createNotificationChannel(incomingChannel)
        nm.createNotificationChannel(ongoingChannel)
        channelsReady = true
    }

    /** Incoming call: rings, shows over the lock screen, offers Answer/Decline. */
    fun showIncoming(context: Context, displayName: String, isVideo: Boolean) {
        val ctx = context.applicationContext
        initChannels(ctx)

        val notification =
            callNotificationBuilder(ctx, CHANNEL_INCOMING, displayName, if (isVideo) "Incoming video call" else "Incoming call")
                .setStyle(
                    NotificationCompat.CallStyle.forIncomingCall(
                        person(ctx, displayName),
                        declinePendingIntent(ctx),
                        answerPendingIntent(ctx),
                    )
                )
                .setFullScreenIntent(fullScreenPendingIntent(ctx), true)
                .setContentIntent(fullScreenPendingIntent(ctx))
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .build()

        notify(ctx, notification)
        startVibration(ctx)
    }

    fun buildOngoing(context: Context, displayName: String, connectedAtMs: Long): Notification {
        val ctx = context.applicationContext
        initChannels(ctx)
        return ongoingBuilder(ctx, displayName, connectedAtMs).build()
    }

    private fun ongoingBuilder(ctx: Context, displayName: String, connectedAtMs: Long): NotificationCompat.Builder =
        callNotificationBuilder(ctx, CHANNEL_ONGOING, displayName, "Ongoing call")
            .setStyle(
                NotificationCompat.CallStyle.forOngoingCall(
                    person(ctx, displayName),
                    hangupPendingIntent(ctx),
                )
            )
            .setContentIntent(appContentPendingIntent(ctx))
            .setOngoing(true)
            .setAutoCancel(false)
            .setUsesChronometer(true)
            .setWhen(connectedAtMs)

    fun cancel(context: Context) {
        stopVibration()
        NotificationManagerCompat.from(context.applicationContext).cancel(NOTIFICATION_ID)
    }

    /**
     * Starts the looping ring vibration. Honors the ringer mode (silent -> no
     * buzz) and tags the vibration as a ringtone so the OS applies its own
     * ring/DND policy. Safe to call repeatedly; the latest call replaces any
     * in-flight vibration.
     */
    private fun startVibration(context: Context) {
        val ctx = context.applicationContext
        val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager?.ringerMode == AudioManager.RINGER_MODE_SILENT) return

        val vib = resolveVibrator(ctx).also { vibrator = it }
        if (vib == null || !vib.hasVibrator()) return

        val effect = VibrationEffect.createWaveform(RING_VIBRATION_PATTERN, 0)
        @SuppressLint("MissingPermission")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            vib.vibrate(
                effect,
                VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_RINGTONE).build(),
            )
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(
                effect,
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build(),
            )
        }
    }

    /** Stops the ring vibration. Called on answer, decline, hangup, timeout or teardown. */
    fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
    }

    private fun resolveVibrator(ctx: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    // ---- builders --------------------------------------------------------

    private fun callNotificationBuilder(ctx: Context, channelId: String, title: String, text: String): NotificationCompat.Builder =
        NotificationCompat.Builder(ctx, channelId)
            .setSmallIcon(appIcon(ctx))
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

    private fun person(ctx: Context, displayName: String): Person =
        Person.Builder()
            .setName(displayName)
            .setIcon(IconCompat.createWithResource(ctx, android.R.drawable.ic_menu_call))
            .setImportant(true)
            .build()

    private fun answerPendingIntent(ctx: Context): PendingIntent {
        val intent =
            Intent(ctx, IncomingCallActivity::class.java).apply {
                action = IncomingCallActivity.ACTION_ANSWER
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        return PendingIntent.getActivity(ctx, RC_ANSWER, intent, immutable())
    }

    private fun declinePendingIntent(ctx: Context): PendingIntent =
        PendingIntent.getBroadcast(
            ctx,
            RC_DECLINE,
            Intent(ctx, EndCallNotificationReceiver::class.java).setAction(EndCallNotificationReceiver.ACTION_DECLINE),
            immutable(),
        )

    private fun hangupPendingIntent(ctx: Context): PendingIntent =
        PendingIntent.getBroadcast(
            ctx,
            RC_HANGUP,
            Intent(ctx, EndCallNotificationReceiver::class.java).setAction(EndCallNotificationReceiver.ACTION_HANGUP),
            immutable(),
        )

    private fun fullScreenPendingIntent(ctx: Context): PendingIntent {
        val intent =
            Intent(ctx, IncomingCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        return PendingIntent.getActivity(ctx, RC_FULL_SCREEN, intent, immutable())
    }

    private fun appContentPendingIntent(ctx: Context): PendingIntent {
        val launch =
            ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            } ?: Intent()
        return PendingIntent.getActivity(ctx, RC_CONTENT, launch, immutable())
    }

    private fun immutable(): Int = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    @SuppressLint("MissingPermission")
    private fun notify(ctx: Context, notification: Notification) {
        try {
            NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted (Android 13+). The host app is
            // responsible for requesting it; swallow so the call still proceeds.
        }
    }

    private fun appIcon(ctx: Context): Int =
        try {
            ctx.packageManager.getApplicationInfo(ctx.packageName, 0).icon
                .takeIf { it != 0 } ?: android.R.drawable.sym_def_app_icon
        } catch (_: PackageManager.NameNotFoundException) {
            android.R.drawable.sym_def_app_icon
        }
}
