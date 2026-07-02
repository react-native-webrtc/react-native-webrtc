package com.oney.WebRTCModule.voip

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat

/**
 * Posts and updates the single CallStyle notification that keeps the app in
 * foreground-execution priority for the lifetime of a Core-Telecom call.
 *
 * Core-Telecom grants foreground priority only while a valid CallStyle
 * notification is posted within ~5s of [CallsManager.addCall] and stays valid
 * until the call ends, so [CallManager] posts here as soon as the call is
 * accepted and cancels in its `finally` block.
 *
 * One notification (id [NOTIFICATION_ID]) is re-posted as the call transitions
 * incoming -> ongoing; the call-style factory decides how it renders
 */
@RequiresApi(26)
object CallNotificationManager {
    private const val CHANNEL_INCOMING = "fishjam_telecom_incoming"
    private const val CHANNEL_ONGOING = "fishjam_telecom_ongoing"
    const val NOTIFICATION_ID = 8400
    private val ringToneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

    // Distinct request codes so the PendingIntents don't collapse into one.
    private const val RC_ANSWER = 1
    private const val RC_DECLINE = 2
    private const val RC_HANGUP = 3
    private const val RC_FULL_SCREEN = 4

    private var channelsReady = false

    fun initChannels(context: Context) {
        if (channelsReady) return
        val nm = NotificationManagerCompat.from(context)

        val incomingChannel = NotificationChannelCompat.Builder(
            CHANNEL_INCOMING,
            NotificationManagerCompat.IMPORTANCE_HIGH,
        ).setName("Incoming calls")
            .setDescription("Handles the notifications when receiving a call")
            .setVibrationEnabled(true).setSound(
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
    }

    /** Connected/active call: silent, single Hang up action, live duration timer. */
    fun showOngoing(context: Context, displayName: String, connectedAtMs: Long = System.currentTimeMillis()) {
        val ctx = context.applicationContext
        initChannels(ctx)

        val notification =
            callNotificationBuilder(ctx, CHANNEL_ONGOING, displayName, "Ongoing call") // i think we could also change displayName to displayText since we might want something else rather than user name
                .setStyle(
                    NotificationCompat.CallStyle.forOngoingCall(
                        person(ctx, displayName),
                        hangupPendingIntent(ctx),
                    )
                )
                .setContentIntent(fullScreenPendingIntent(ctx)) // leave it for now but this should forward the user to the app and rn view instead of oour default one here
                .setOngoing(true)
                .setAutoCancel(false)
                .setUsesChronometer(true)
                .setWhen(connectedAtMs)
                .build()

        notify(ctx, notification)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context.applicationContext).cancel(NOTIFICATION_ID)
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

    private fun immutable(): Int = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

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
