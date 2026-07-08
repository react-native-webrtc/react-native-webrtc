package com.oney.WebRTCModule.voip

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import kotlin.math.abs

/**
 * Full-screen incoming-call screen shown over the lock screen via the
 * notification's full-screen intent. Built programmatically so the library
 * carries no layout resources.
 *
 * Two entry modes:
 *  - launched with [ACTION_ANSWER] (the notification's Answer button): answers
 *    immediately, brings the host app forward, and shows a minimal
 *    "Connecting..." view until the host activity covers it.
 *  - launched without an action (full-screen intent): draws the ring UI with
 *    Answer/Decline buttons.
 *
 * Auto-dismisses when the call ends elsewhere via the [ACTION_CALL_ENDED]
 * broadcast that [CallManager] sends when its call tears down.
 */
@RequiresApi(Build.VERSION_CODES.O)
class IncomingCallActivity : Activity() {
    companion object {
        const val ACTION_ANSWER = "fishjam.voip.ACTION_ANSWER"
        const val ACTION_CALL_ENDED = "fishjam.voip.ACTION_CALL_ENDED"

        /** Fraction of max travel the knob must cross to trigger the action. */
        private const val SWIPE_TRIGGER = 0.7f

        /** Max handset tilt (degrees) at full travel; negative = toward Decline. */
        private const val ICON_MAX_TILT = 35f

        /** How much the non-target label fades at full travel (0..1). */
        private const val LABEL_FADE = 0.75f

        /** How far (dp) the target label slides outward at full travel. */
        private const val LABEL_SHIFT_DP = 10
    }

    private val callEndedReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = finish()
        }
    private var callEndedReceiverRegistered = false
    private var isAnswering = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureWindowForLockScreen()

        if (intent?.action == ACTION_ANSWER) {
            answerAndOpenApp()
            return
        }

        if (!CallManager.hasActiveCall()) {
            finish()
            return
        }

        setContentView(buildUi(CallManager.currentDisplayName(), CallManager.currentIsVideo()))
        registerCallEndedReceiver()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == ACTION_ANSWER) answerAndOpenApp()
    }

    override fun onDestroy() {
        if (callEndedReceiverRegistered) unregisterReceiver(callEndedReceiver)
        super.onDestroy()
    }

    private fun answerAndOpenApp() {
        // Reachable from both the swipe gesture and repeated ACTION_ANSWER
        // intents (onCreate + onNewIntent); only the first one may run.
        if (isAnswering) return
        isAnswering = true

        CallManager.answer()
        // Register the lifecycle hook before launching, so the host activity
        // is flagged in onActivityCreated even on the fastest cold start.
        LockScreenController.onCallAnswered(applicationContext)
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            it.putExtra(LockScreenController.VOIP_ANSWER, true)
            startActivity(it)
        }
        setContentView(buildConnectingUi(CallManager.currentDisplayName()))
        registerCallEndedReceiver()
    }

    override fun onStop() {
        super.onStop()
        // Finish once we're covered after any answer: our own swipe/button
        // (isAnswering), or an external answer (headset/Bluetooth/Auto/watch)
        // whose CallManager-launched host activity now sits on top of us.
        if (isAnswering || CallManager.isAnswered()) finish()
    }

    private fun registerCallEndedReceiver() {
        if (callEndedReceiverRegistered) return
        callEndedReceiverRegistered = true
        ContextCompat.registerReceiver(
            this,
            callEndedReceiver,
            IntentFilter(ACTION_CALL_ENDED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    @Suppress("DEPRECATION")
    private fun configureWindowForLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
        )
    }

    // Pixel-dialer-inspired palette.
    private val backgroundDark = Color.parseColor("#121316")
    private val avatarGreen = Color.parseColor("#1B6B50")
    private val answerGreen = Color.parseColor("#1E8E3E")
    private val declineRed = Color.parseColor("#DC362E")
    private val barGray = Color.parseColor("#2B2D31")
    private val textPrimary = Color.parseColor("#E9EBEE")
    private val textSecondary = Color.parseColor("#C4C7CC")

    private fun buildUi(displayName: String, isVideo: Boolean): ViewGroup {
        val name = displayName.ifBlank { "Unknown" }

        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setBackgroundColor(backgroundDark)
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                setPadding(0, dp(56), 0, dp(40))
            }

        root.addView(
            TextView(this).apply {
                text = if (isVideo) "Incoming video call" else "Incoming voice call"
                setTextColor(textSecondary)
                textSize = 16f
                gravity = Gravity.CENTER
            }
        )

        // Big caller name; marquee-scrolls when it overflows, like the dialer.
        root.addView(
            TextView(this).apply {
                text = name
                setTextColor(textPrimary)
                textSize = 38f
                gravity = Gravity.CENTER
                isSingleLine = true
                ellipsize = TextUtils.TruncateAt.MARQUEE
                marqueeRepeatLimit = -1
                isSelected = true
                setPadding(0, dp(6), 0, 0)
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
            }
        )

        root.addView(Space(this), weight(1f))

        // Large centered avatar (the audio-call variant's hero element).
        root.addView(avatarView(name, sizeDp = 180, textSizeSp = 72f))

        root.addView(Space(this), weight(1f))

        root.addView(buildSwipeBar())

        return root
    }

    /** Placeholder shown after answering, until the host activity covers us. */
    private fun buildConnectingUi(displayName: String): ViewGroup {
        val name = displayName.ifBlank { "Unknown" }

        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundColor(backgroundDark)
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
            }

        root.addView(avatarView(name, sizeDp = 120, textSizeSp = 48f))

        root.addView(
            TextView(this).apply {
                text = name
                setTextColor(textPrimary)
                textSize = 26f
                gravity = Gravity.CENTER
                setPadding(0, dp(20), 0, 0)
            }
        )

        root.addView(
            TextView(this).apply {
                text = "Connecting…"
                setTextColor(textSecondary)
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, 0)
            }
        )

        return root
    }

    /**
     * The bottom pill: "Decline" on the left, "Answer" on the right, and a white
     * knob with a green phone icon in the middle. Drag the knob right to answer
     * (tints green) or left to decline (tints red); release short of the
     * threshold and it springs back.
     */
    private fun buildSwipeBar(): FrameLayout {
        val bar =
            FrameLayout(this).apply {
                background = pill(barGray, radiusDp = 48)
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(96),
                    ).apply {
                        marginStart = dp(20)
                        marginEnd = dp(20)
                    }
            }

        val declineLabel =
            TextView(this).apply {
                text = "Decline"
                setTextColor(textPrimary)
                textSize = 17f
                layoutParams =
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.START or Gravity.CENTER_VERTICAL,
                    ).apply { marginStart = dp(32) }
            }
        bar.addView(declineLabel)

        val answerLabel =
            TextView(this).apply {
                text = "Answer"
                setTextColor(textPrimary)
                textSize = 17f
                layoutParams =
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.END or Gravity.CENTER_VERTICAL,
                    ).apply { marginEnd = dp(32) }
            }
        bar.addView(answerLabel)

        val icon =
            ImageView(this).apply {
                setImageResource(android.R.drawable.sym_action_call)
                setColorFilter(answerGreen)
                layoutParams =
                    FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER)
            }
        val knobBackground = pill(Color.WHITE, radiusDp = 36)
        val knob =
            FrameLayout(this).apply {
                background = knobBackground
                contentDescription = "Swipe right to answer, left to decline"
                layoutParams = FrameLayout.LayoutParams(dp(120), dp(72), Gravity.CENTER)
                addView(icon)
            }
        bar.addView(knob)

        attachSwipe(bar, knob, knobBackground, icon, declineLabel, answerLabel)
        return bar
    }

    private fun attachSwipe(
        bar: FrameLayout,
        knob: FrameLayout,
        knobBackground: GradientDrawable,
        icon: ImageView,
        declineLabel: TextView,
        answerLabel: TextView,
    ) {
        var downX = 0f

        // fraction is signed, in [-1, 1]; negative = dragging toward Decline.
        fun applyDragVisuals(fraction: Float) {
            val magnitude = abs(fraction)
            val target = if (fraction >= 0) answerGreen else declineRed

            knobBackground.setColor(ColorUtils.blendARGB(Color.WHITE, target, magnitude))
            icon.setColorFilter(ColorUtils.blendARGB(answerGreen, Color.WHITE, magnitude))
            // Tilt the handset with the drag (hang-up tilt toward Decline).
            icon.rotation = fraction * ICON_MAX_TILT

            if (fraction < 0) {
                // Toward Decline: its label reddens and slides out; Answer fades.
                declineLabel.setTextColor(
                    ColorUtils.blendARGB(textPrimary, declineRed, magnitude)
                )
                declineLabel.translationX = -dp(LABEL_SHIFT_DP) * magnitude
                declineLabel.alpha = 1f
                answerLabel.setTextColor(textPrimary)
                answerLabel.translationX = 0f
                answerLabel.alpha = 1f - LABEL_FADE * magnitude
            } else {
                // Toward Answer: its label greens and slides out; Decline fades.
                answerLabel.setTextColor(
                    ColorUtils.blendARGB(textPrimary, answerGreen, magnitude)
                )
                answerLabel.translationX = dp(LABEL_SHIFT_DP) * magnitude
                answerLabel.alpha = 1f
                declineLabel.setTextColor(textPrimary)
                declineLabel.translationX = 0f
                declineLabel.alpha = 1f - LABEL_FADE * magnitude
            }
        }

        fun resetVisuals() {
            knob.animate().translationX(0f).setDuration(150).start()
            icon.animate().rotation(0f).setDuration(150).start()
            knobBackground.setColor(Color.WHITE)
            icon.setColorFilter(answerGreen)
            for (label in listOf(declineLabel, answerLabel)) {
                label.animate().translationX(0f).alpha(1f).setDuration(150).start()
                label.setTextColor(textPrimary)
            }
        }

        knob.setOnTouchListener { view, event ->
            val maxTravel = (bar.width - knob.width) / 2f - dp(12)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (maxTravel <= 0f) return@setOnTouchListener true
                    val dx = (event.rawX - downX).coerceIn(-maxTravel, maxTravel)
                    knob.translationX = dx
                    applyDragVisuals(dx / maxTravel)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    view.performClick()
                    val fraction =
                        if (maxTravel > 0f) knob.translationX / maxTravel else 0f
                    when {
                        fraction >= SWIPE_TRIGGER -> answerAndOpenApp()
                        fraction <= -SWIPE_TRIGGER -> {
                            CallManager.endCall()
                            finish()
                        }
                        else -> resetVisuals()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    resetVisuals()
                    true
                }
                else -> false
            }
        }
    }

    private fun avatarView(name: String, sizeDp: Int, textSizeSp: Float): TextView =
        TextView(this).apply {
            text = name.firstOrNull()?.uppercase() ?: "?"
            setTextColor(Color.WHITE)
            textSize = textSizeSp
            gravity = Gravity.CENTER
            background =
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(avatarGreen)
                }
            layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
        }

    private fun pill(color: Int, radiusDp: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(color)
        }

    private fun weight(value: Float): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, value)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
