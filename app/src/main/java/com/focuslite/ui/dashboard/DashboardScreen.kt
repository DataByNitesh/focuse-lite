package com.focuslite.ui.dashboard

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.focuslite.AppContainer
import com.focuslite.domain.policy.Estimates
import com.focuslite.domain.policy.ProtectionStatus
import com.focuslite.domain.rules.Reliability
import com.focuslite.service.ServiceRuntime
import com.focuslite.ui.common.ScreenScaffold
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    container: AppContainer,
    onOpenSetup: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Re-check things Android may change while we're in the background (user toggled settings).
    var enabledInSettings by remember { mutableStateOf(ServiceRuntime.isEnabledInSettings(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        enabledInSettings = ServiceRuntime.isEnabledInSettings(context)
    }

    ScreenScaffold(
        title = "FocusLite",
        actions = { TextButton(onClick = onOpenSettings) { Text("Settings") } },
    ) { padding ->
        val s = state
        if (s == null) {
            Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(Modifier.padding(32.dp))
            }
            return@ScreenScaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                StatusCard(
                    status = s.status,
                    pausedUntil = s.settings.pausedUntilMillis,
                    enabledInSettings = enabledInSettings,
                    onSetup = onOpenSetup,
                    onResume = vm::resume,
                    onEnable = { vm.setMasterEnabled(true) },
                )
            }
            item { TodayCard(s.blockedToday) }
            item {
                ControlsCard(
                    masterEnabled = s.settings.masterEnabled,
                    paused = s.status == ProtectionStatus.PAUSED,
                    onMasterChange = vm::setMasterEnabled,
                    onPause = vm::pauseFor,
                )
            }
            item {
                Text("Blocking rules", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            }
            items(s.rules, key = { it.rule.targetId }) { row ->
                RuleCard(row, installed = isInstalled(context, row.rule.packageName), onToggle = { vm.setTargetEnabled(row.rule.targetId, it) })
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenSetup) { Text("Permissions & setup") }
                    OutlinedButton(onClick = onOpenDiagnostics) { Text("Diagnostics") }
                }
            }
            item {
                Text(
                    "Coming next: whole-app blocking, schedules and focus sessions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    status: ProtectionStatus,
    pausedUntil: Long,
    enabledInSettings: Boolean,
    onSetup: () -> Unit,
    onResume: () -> Unit,
    onEnable: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val (container, title, body) = when (status) {
        ProtectionStatus.ACTIVE -> Triple(cs.primaryContainer, "Blocking is active", "Selected sections are blocked.")
        ProtectionStatus.SERVICE_OFF -> Triple(
            cs.errorContainer,
            "Not protecting",
            if (enabledInSettings) {
                "The accessibility service is switched on but not running. Android or Realme battery management may have stopped it. Open setup."
            } else {
                "The FocusLite accessibility service is off. Nothing is being blocked."
            },
        )
        ProtectionStatus.PAUSED -> Triple(cs.tertiaryContainer, "Paused", "Blocking resumes at ${formatTime(pausedUntil)}.")
        ProtectionStatus.DISABLED -> Triple(cs.surfaceVariant, "Blocking switched off", "Turn it back on below.")
        ProtectionStatus.NO_RULES -> Triple(cs.surfaceVariant, "No rules enabled", "Enable at least one rule below.")
    }
    Card(colors = CardDefaults.cardColors(containerColor = container), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            when (status) {
                ProtectionStatus.SERVICE_OFF -> Button(onClick = onSetup) { Text("Set up") }
                ProtectionStatus.PAUSED -> Button(onClick = onResume) { Text("Resume now") }
                ProtectionStatus.DISABLED -> Button(onClick = onEnable) { Text("Turn on") }
                else -> Unit
            }
        }
    }
}

@Composable
private fun TodayCard(blockedToday: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp)) {
            Column(Modifier.weight(1f)) {
                Text("$blockedToday", style = MaterialTheme.typography.displaySmall)
                Text("blocked today", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("~${Estimates.minutesAvoided(blockedToday)} min", style = MaterialTheme.typography.displaySmall)
                Text(
                    "estimated time avoided (assumes ${Estimates.ASSUMED_MINUTES_PER_ATTEMPT} min per blocked attempt)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ControlsCard(masterEnabled: Boolean, paused: Boolean, onMasterChange: (Boolean) -> Unit, onPause: (Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Blocking", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(checked = masterEnabled, onCheckedChange = onMasterChange)
            }
            if (masterEnabled && !paused) {
                Text("Pause temporarily", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 15, 30).forEach { m -> OutlinedButton(onClick = { onPause(m) }) { Text("$m min") } }
                }
            }
        }
    }
}

@Composable
private fun RuleCard(row: RuleRow, installed: Boolean, onToggle: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(row.rule.title, style = MaterialTheme.typography.titleMedium)
                val tags = buildList {
                    if (row.rule.reliability == Reliability.EXPERIMENTAL) add("Experimental")
                    if (!installed) add("App not installed")
                    add("${row.blockedToday} today")
                }
                Text(
                    tags.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (row.rule.reliability == Reliability.EXPERIMENTAL) MaterialTheme.colorScheme.tertiary else Color.Unspecified,
                )
            }
            Switch(checked = row.enabled, onCheckedChange = onToggle)
        }
    }
}

private fun isInstalled(context: Context, packageName: String): Boolean = try {
    @Suppress("DEPRECATION")
    context.packageManager.getPackageInfo(packageName, 0)
    true
} catch (e: PackageManager.NameNotFoundException) {
    false
}

private fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
