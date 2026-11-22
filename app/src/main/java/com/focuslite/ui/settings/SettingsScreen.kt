package com.focuslite.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.focuslite.AppContainer
import com.focuslite.BuildConfig
import com.focuslite.ui.common.ScreenScaffold
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(container: AppContainer, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val total by container.statsRepository.countAll().collectAsState(initial = 0)
    var confirmDelete by remember { mutableStateOf(false) }

    ScreenScaffold(title = "Settings", onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Privacy", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "• Everything stays on this phone. FocusLite has no internet permission, no account, no analytics and no ads.\n" +
                            "• Statistics store only: time, app and which rule blocked (e.g. \"instagram_reels\").\n" +
                            "• Screen content such as messages, captions and passwords is never stored.\n" +
                            "• App data is excluded from cloud backup.",
                    )
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Statistics", style = MaterialTheme.typography.titleMedium)
                    Text("$total blocked attempts stored.")
                    OutlinedButton(onClick = { confirmDelete = true }, enabled = total > 0) { Text("Delete all statistics") }
                }
            }
            Text("FocusLite ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete statistics?") },
            text = { Text("This permanently removes all stored blocked-attempt history. Settings are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch { container.statsRepository.deleteAll() }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
