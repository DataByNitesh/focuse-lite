package com.focuslite.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.focuslite.AppContainer
import com.focuslite.FocusLiteApp
import com.focuslite.config.SupportedApps
import com.focuslite.domain.policy.BlockAction
import com.focuslite.domain.policy.BlockActionPlanner
import com.focuslite.domain.policy.BlockingPolicy
import com.focuslite.domain.policy.FocusSettings
import com.focuslite.domain.rules.ScreenDump
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * The blocking engine's connection to Android.
 *
 * Lifecycle: Android creates this service when the user enables it in Settings > Accessibility,
 * and keeps it bound (high priority) until the user disables it, the app is updated/force-stopped,
 * or — on aggressive OEM builds like Realme UI — the system kills it.
 *
 * Flow: event from a monitored app -> (debounced) snapshot of the active window ->
 * RuleEngine -> BlockActionPlanner -> Back/Home + banner + record statistic.
 * All callbacks arrive on the main thread.
 */
class FocusAccessibilityService : AccessibilityService() {

    private lateinit var container: AppContainer
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private val planner = BlockActionPlanner()
    private var overlay: BlockOverlay? = null

    @Volatile private var settings = FocusSettings()
    private var monitored: Set<String> = emptySet()
    private var pendingEvaluation: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        container = (application as FocusLiteApp).container
        monitored = container.ruleEngine.monitoredPackages

        // Only receive events from the supported apps. Saves battery and is better for privacy.
        serviceInfo = serviceInfo.apply { packageNames = monitored.toTypedArray() }

        overlay = BlockOverlay(this)
        scope.launch { container.settingsRepository.settings.collect { settings = it } }
        ServiceRuntime.setRunning(true)
        Log.i(TAG, "Service connected; monitoring $monitored")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in monitored) return
        when (event.eventType) {
            // A new screen/activity/dialog: check immediately.
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> scheduleEvaluation(0)
            // Content changes fire constantly in video apps: check at most every CONTENT_THROTTLE_MS.
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> scheduleEvaluation(CONTENT_THROTTLE_MS)
        }
    }

    private fun scheduleEvaluation(delayMs: Long) {
        if (delayMs == 0L) {
            pendingEvaluation?.let(handler::removeCallbacks)
            pendingEvaluation = null
            evaluateNow()
            return
        }
        if (pendingEvaluation != null) return // one already queued; throttles bursts
        val r = Runnable {
            pendingEvaluation = null
            evaluateNow()
        }
        pendingEvaluation = r
        handler.postDelayed(r, delayMs)
    }

    private fun evaluateNow() {
        val root: AccessibilityNodeInfo = rootInActiveWindow ?: return
        val pkg = root.packageName?.toString() ?: return
        if (pkg !in monitored) return

        val now = System.currentTimeMillis()
        val captureAt = ServiceRuntime.captureAt.value
        val wantCapture = captureAt != null && now >= captureAt
        val targets = BlockingPolicy.effectiveTargets(settings, now)
        if (targets.isEmpty() && !wantCapture) return

        val snapshot = try {
            NodeSnapshot.from(root)
        } catch (e: RuntimeException) {
            // Nodes can become stale while we read them (the app changed screen). Try again on the next event.
            Log.d(TAG, "Snapshot failed", e)
            return
        }

        if (wantCapture) captureDiagnostics(pkg, snapshot)

        val rule = container.ruleEngine.evaluate(pkg, snapshot, targets) ?: return
        val decision = planner.onDetected(SystemClock.elapsedRealtime())
        when (decision.action) {
            BlockAction.NONE -> return
            BlockAction.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            BlockAction.HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
        }
        overlay?.show("${rule.title} blocked by FocusLite")
        ServiceRuntime.addLog("${timeNow()}  ${rule.targetId} -> ${decision.action}")

        if (decision.countAsAttempt) {
            container.appScope.launch { container.statsRepository.recordBlock(pkg, rule.targetId, now) }
        }
        // Verify we actually left the blocked screen; the planner escalates to Home if Back loops.
        handler.postDelayed({ evaluateNow() }, RECHECK_MS)
    }

    private fun captureDiagnostics(pkg: String, snapshot: com.focuslite.domain.rules.UiNode) {
        val version = try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(pkg, 0).versionName
        } catch (e: Exception) {
            "unknown"
        }
        val matches = container.ruleEngine.matchingRules(pkg, snapshot).joinToString { it.targetId }.ifEmpty { "none" }
        val header = buildString {
            appendLine("FocusLite screen capture")
            appendLine("app: ${SupportedApps.byPackage(pkg)?.displayName ?: pkg} ($pkg) v$version")
            appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("rules matching this screen: $matches")
            appendLine("---")
        }
        ServiceRuntime.completeCapture(header + ScreenDump.format(snapshot))
        ServiceRuntime.addLog("${timeNow()}  captured $pkg")
    }

    override fun onInterrupt() {
        // Required by the API; called when the system wants feedback interrupted. Nothing to do.
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        ServiceRuntime.setRunning(false)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        ServiceRuntime.setRunning(false)
        handler.removeCallbacksAndMessages(null)
        overlay?.hide()
        scope.cancel()
        super.onDestroy()
    }

    private fun timeNow(): String = LocalTime.now().format(TIME_FORMAT)

    companion object {
        private const val TAG = "FocusLite"
        private const val CONTENT_THROTTLE_MS = 400L
        private const val RECHECK_MS = 800L
        private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    }
}
