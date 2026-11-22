package com.focuslite.ui.setup

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.focuslite.AppContainer
import com.focuslite.domain.policy.FocusSettings
import com.focuslite.service.ServiceRuntime
import com.focuslite.ui.common.ScreenScaffold
import kotlinx.coroutines.launch

/**
 * Explains every permission in plain language and walks through enabling it.
 * The accessibility button stays disabled until the user explicitly agrees to the disclosure.
 */
@Composable
fun SetupScreen(container: AppContainer, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by container.settingsRepository.settings.collectAsState(initial = FocusSettings())
    val running by ServiceRuntime.running.collectAsState()

    var enabledInSettings by remember { mutableStateOf(ServiceRuntime.isEnabledInSettings(context)) }
    var batteryExempt by remember { mutableStateOf(isBatteryExempt(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        enabledInSettings = ServiceRuntime.isEnabledInSettings(context)
        batteryExempt = isBatteryExempt(context)
    }

    ScreenScaffold(title = "Permissions & setup", onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StepCard(
                number = 1,
                title = "Accessibility service",
                done = enabledInSettings && running,
            ) {
                Text(
                    "FocusLite uses Android's Accessibility Service to see which screen is open in Instagram, " +
                        "YouTube and Facebook, and presses Back when you open Reels, Shorts or Explore.",
                )
                Text("What it reads: screen element ids (like \"clips_tab\"), whether a tab is selected, and short button labels — only inside those three apps.")
                Text("What it never does: read or store your messages, captions or passwords; send anything off your phone (the app has no internet permission).")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = settings.consentAccepted,
                        onCheckedChange = { checked -> scope.launch { container.settingsRepository.setConsentAccepted(checked) } },
                    )
                    Text("I understand and allow FocusLite to do this.")
                }
                Button(
                    enabled = settings.consentAccepted,
                    onClick = { open(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                ) { Text("Open Accessibility settings") }
                Text(
                    "In Settings: Downloaded apps (or Installed services) → FocusLite blocker → turn on.\n" +
                        "If the switch is greyed out (\"Restricted setting\"): open App info below → ⋮ menu → " +
                        "\"Allow restricted settings\", then try again. This happens when an APK is installed from a browser or file manager.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (enabledInSettings && !running) {
                    Text(
                        "Enabled in Settings but not running. Turn it off and on again, and complete step 2.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            StepCard(number = 2, title = "Keep FocusLite running (Realme)", done = batteryExempt) {
                Text("Realme UI stops background apps to save battery, which silently turns blocking off.")
                Button(onClick = { requestBatteryExemption(context) }) { Text("Don't optimise battery") }
                Text(
                    "Also on Realme:\n" +
                        "• App info → Battery usage → allow background activity and auto launch\n" +
                        "• Recent apps → long-press FocusLite → Lock",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(onClick = {
                    open(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                }) { Text("Open App info") }
            }

            StepCard(number = 3, title = "Other blockers", done = null) {
                Text(
                    "If another blocker (e.g. AppBlock) also uses accessibility on the same apps, both may react at once. " +
                        "Pause the other app's rules for Instagram/YouTube/Facebook while testing FocusLite.",
                )
            }
        }
    }
}

@Composable
private fun StepCard(number: Int, title: String, done: Boolean?, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val badge = when (done) {
                true -> " ✓ Done"
                false -> " — needs action"
                null -> ""
            }
            Text("$number. $title$badge", style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

private fun isBatteryExempt(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

@SuppressLint("BatteryLife") // Justified: Realme kills the accessibility service otherwise.
private fun requestBatteryExemption(context: Context) {
    val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
    if (!open(context, direct)) open(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
}

private fun open(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent)
    true
} catch (e: ActivityNotFoundException) {
    false
}
