package com.oney.WebRTCModule.voip

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.annotation.RequiresApi
import java.lang.ref.WeakReference

/**
 * Lets the host app's activity appear over the lock screen
 *
 * Two paths:
 *  - Cold start: [onCallAnswered] registers [Application.ActivityLifecycleCallbacks]
 *    so the flags land in `onActivityCreated`, before the host activity's
 *    window is first shown.
 *  - Warm start: [showOverLockScreen] flags an already-existing activity directly.
 *
 * [onCallEnded] clears the flags and unregisters, dropping the app back
 * behind the keyguard once the call is over.
 */
@RequiresApi(Build.VERSION_CODES.O)
object LockScreenController {
    const val VOIP_ANSWER = "fishjam.voip.VOIP_ANSWER"

    private var application: Application? = null
    private var flaggedActivity = WeakReference<Activity>(null)

    private val lifecycleCallbacks =
        object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is IncomingCallActivity || activity.intent?.getBooleanExtra(VOIP_ANSWER, false) != true) return
                showOverLockScreen(activity)
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        }

    /**
     * Starts watching for the host activity's creation (cold-start path).
     * Safe to call from any thread; registering twice is prevented.
     */
    fun onCallAnswered(context: Context) {
        val app = context.applicationContext as? Application ?: return
        synchronized(this) {
            if (application == null) {
                app.registerActivityLifecycleCallbacks(lifecycleCallbacks)
                application = app
            }
        }
    }

    /**
     * Flags an activity to show over the keyguard (warm-start path, or from
     * the lifecycle hook on cold start). Safe to call from any thread.
     */
    fun showOverLockScreen(activity: Activity) {
        flaggedActivity = WeakReference(activity)
        activity.runOnUiThread {
            if (activity.isDestroyed) return@runOnUiThread
            setLockScreenFlags(activity, show = true)
        }
    }

    /**
     * Clears the flags on the tracked activity and stops watching, so the app
     * is no longer reachable over the keyguard once the call ends.
     */
    fun onCallEnded() {
        synchronized(this) {
            application?.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
            application = null
        }
        val activity = flaggedActivity.get() ?: return
        flaggedActivity = WeakReference(null)
        activity.runOnUiThread {
            if (activity.isDestroyed) return@runOnUiThread
            setLockScreenFlags(activity, show = false)
        }
    }

    @Suppress("DEPRECATION")
    private fun setLockScreenFlags(activity: Activity, show: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            activity.setShowWhenLocked(show)
            activity.setTurnScreenOn(show)
        } else {
            val flags =
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            if (show) activity.window.addFlags(flags) else activity.window.clearFlags(flags)
        }
    }
}
