package com.focuslite.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

/**
 * A small banner shown at the top of the screen after a block, e.g. "Reels blocked by FocusLite".
 *
 * TYPE_ACCESSIBILITY_OVERLAY is a window type only accessibility services may use, so we do
 * NOT need the "Display over other apps" permission. The banner is not touchable, so it never
 * gets in the way. It is a plain Android View (not Compose) because Compose inside a service
 * window needs extra lifecycle plumbing that isn't worth it for one line of text.
 */
class BlockOverlay(private val service: AccessibilityService) {

    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var banner: TextView? = null
    private val hideRunnable = Runnable { hide() }

    fun show(message: String) {
        val view = banner ?: createBanner()?.also { banner = it } ?: return
        view.text = message
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, 2_500)
    }

    fun hide() {
        handler.removeCallbacks(hideRunnable)
        banner?.let { runCatching { windowManager.removeView(it) } }
        banner = null
    }

    private fun createBanner(): TextView? {
        val view = TextView(service).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            val pad = dp(14)
            setPadding(pad, pad, pad, pad)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Color.argb(235, 32, 33, 36))
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(64)
        }
        return try {
            windowManager.addView(view, params)
            view
        } catch (e: RuntimeException) {
            Log.w("FocusLite", "Could not show block banner", e)
            null
        }
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), service.resources.displayMetrics).toInt()
}
