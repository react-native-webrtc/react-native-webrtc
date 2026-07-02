package com.oney.WebRTCModule.voip

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

/**
 * Full-screen incoming-call screen shown over the lock screen via the
 * notification's full-screen intent. Built programmatically so the library
 * carries no layout resources.
 *
 * Two entry modes:
 *  - launched with [ACTION_ANSWER] (the notification's Answer button): answers
 *    immediately, brings the host app forward, and finishes without drawing UI.
 *  - launched without an action (full-screen intent): draws the ring UI with
 *    Answer/Decline buttons.
 *
 * Auto-dismisses when the call ends elsewhere via the [ACTION_CALL_ENDED]
 * broadcast that [CallManager] sends when its call tears down.
 */
@RequiresApi(Build.VERSION_CODES.O)
// for now i leave this not reviewed because I want to focus on displaying it actually, and int's not that important right now cuz i can't see it xd
class IncomingCallActivity : Activity() {
    companion object {
        const val ACTION_ANSWER = "fishjam.voip.ACTION_ANSWER"
        const val ACTION_CALL_ENDED = "fishjam.voip.ACTION_CALL_ENDED"
    }

    private val callEndedReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = finish()
        }

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

        setContentView(buildUi(CallManager.currentDisplayName()))
        registerCallEndedReceiver()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == ACTION_ANSWER) answerAndOpenApp()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(callEndedReceiver)
        } catch (_: IllegalArgumentException) {
            // Never registered (e.g. ACTION_ANSWER fast path); ignore.
        }
        super.onDestroy()
    }

    private fun answerAndOpenApp() {
        CallManager.answer()
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(it)
        }
        finish()
    }

    private fun registerCallEndedReceiver() {
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

    private fun buildUi(displayName: String): LinearLayout {
        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundColor(Color.parseColor("#111827"))
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                setPadding(48, 48, 48, 48)
            }

        root.addView(
            TextView(this).apply {
                text = displayName
                setTextColor(Color.WHITE)
                textSize = 28f
                gravity = Gravity.CENTER
            }
        )
        root.addView(
            TextView(this).apply {
                text = "Incoming call"
                setTextColor(Color.parseColor("#9CA3AF"))
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(0, 16, 0, 64)
            }
        )

        val buttonRow =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
        buttonRow.addView(
            Button(this).apply {
                text = "Decline"
                setOnClickListener {
                    CallManager.endCall()
                    finish()
                }
            }
        )
        buttonRow.addView(
            Button(this).apply {
                text = "Answer"
                setOnClickListener { answerAndOpenApp() }
            }
        )
        root.addView(buttonRow)

        return root
    }
}
