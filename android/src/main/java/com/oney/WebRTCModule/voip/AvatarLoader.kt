package com.oney.WebRTCModule.voip

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min

/**
 * Downloads a caller avatar and hands back a circular-cropped [Bitmap]. Used by
 * the incoming-call notification and the full-screen activity.
 *
 * Runs entirely off the main thread with short connect/read timeouts so a slow
 * or unreachable image can never block the call UI; failures resolve to `null`
 * and callers fall back to the initials avatar.
 */
object AvatarLoader {
    private const val TAG = "FishjamVoip.Avatar"
    private const val TIMEOUT_MS = 5000

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Downloads [url] and delivers a circular bitmap on the main thread, or `null`
     * on any failure. No-op with a `null` result if [url] is blank.
     */
    fun load(url: String?, onResult: (Bitmap?) -> Unit) {
        if (url.isNullOrBlank()) {
            onResult(null)
            return
        }
        scope.launch {
            val bitmap = runCatching { fetch(url) }
                .onFailure { Log.e(TAG, "Failed to load avatar: ${it.localizedMessage}") }
                .getOrNull()
                ?.let { circularCrop(it) }
            withContext(Dispatchers.Main) { onResult(bitmap) }
        }
    }

    private fun fetch(url: String): Bitmap? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
        }
        return try {
            connection.inputStream.use { BitmapFactory.decodeStream(it) }
        } finally {
            connection.disconnect()
        }
    }

    /** Center-crops [src] to a square and masks it into a circle. */
    private fun circularCrop(src: Bitmap): Bitmap {
        val size = min(src.width, src.height)
        val left = (src.width - size) / 2
        val top = (src.height - size) / 2
        val square = if (src.width == size && src.height == size) {
            src
        } else {
            Bitmap.createBitmap(src, left, top, size, size)
        }

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(square, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)
        return output
    }
}
