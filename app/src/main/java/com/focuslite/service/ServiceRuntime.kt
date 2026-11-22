package com.focuslite.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-process shared state between the accessibility service and the UI.
 * Both run in the same app process, so plain StateFlows are enough (no IPC needed).
 * Nothing here is persisted — it resets when the process dies, which is fine for live status.
 */
object ServiceRuntime {
    private val _running = MutableStateFlow(false)
    /** True while the system has our accessibility service connected. */
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _captureAt = MutableStateFlow<Long?>(null)
    /** When set, the service captures the next monitored screen at/after this time (epoch millis). */
    val captureAt: StateFlow<Long?> = _captureAt.asStateFlow()

    private val _lastCapture = MutableStateFlow<String?>(null)
    val lastCapture: StateFlow<String?> = _lastCapture.asStateFlow()

    private val _log = MutableStateFlow<List<String>>(emptyList())
    /** Recent detection decisions, newest first. Contains rule ids and actions only. */
    val log: StateFlow<List<String>> = _log.asStateFlow()

    internal fun setRunning(value: Boolean) { _running.value = value }

    fun requestCapture(atMillis: Long) { _captureAt.value = atMillis }

    internal fun completeCapture(text: String) {
        _lastCapture.value = text
        _captureAt.value = null
    }

    internal fun addLog(line: String) {
        _log.update { (listOf(line) + it).take(30) }
    }

    /**
     * Whether the user has switched the service on in Android Settings.
     * Differs from [running]: on Realme the setting can be ON while the service was killed.
     */
    fun isEnabledInSettings(context: Context): Boolean {
        val expected = ComponentName(context, FocusAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }
}
