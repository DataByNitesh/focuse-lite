package com.focuslite.ui.diagnostics

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuslite.service.ServiceRuntime
import com.focuslite.ui.common.ScreenScaffold
import kotlinx.coroutines.delay

/**
 * Developer tool for keeping rules up to date: capture the structure of a screen in a
 * supported app, then copy it and use it to fix rules. Captures contain ids, class names,
 * short labels and flags only — no message or caption text.
 */
@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    val running by ServiceRuntime.running.collectAsState()
    val captureAt by ServiceRuntime.captureAt.collectAsState()
    val lastCapture by ServiceRuntime.lastCapture.collectAsState()
    val log by ServiceRuntime.log.collectAsState()
    val clipboard = LocalClipboardManager.current

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(captureAt) {
        while (captureAt != null) {
            now = System.currentTimeMillis()
            delay(500)
        }
    }

    ScreenScaffold(title = "Diagnostics", onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!running) {
                Text("The accessibility service is not running, so captures can't happen. Finish setup first.", color = MaterialTheme.colorScheme.error)
            }
            Text(
                "1. Tap Capture.\n2. Within 5 seconds switch to Instagram, YouTube or Facebook and open the screen you want.\n" +
                    "3. Wait a moment, come back here, tap Copy and paste it to your developer.",
            )
            val pending = captureAt
            if (pending == null) {
                Button(enabled = running, onClick = { ServiceRuntime.requestCapture(System.currentTimeMillis() + 5_000) }) {
                    Text("Capture in 5 seconds")
                }
            } else {
                val secs = ((pending - now) / 1000).coerceAtLeast(0)
                Text(if (secs > 0) "Capturing in $secs s… switch apps now" else "Waiting for a supported app screen…")
            }

            lastCapture?.let { text ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { clipboard.setText(AnnotatedString(text)) }) { Text("Copy capture") }
                }
                Card(Modifier.fillMaxWidth()) {
                    SelectionContainer {
                        Text(
                            text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(12.dp).horizontalScroll(rememberScrollState()),
                        )
                    }
                }
            }

            Text("Recent decisions", style = MaterialTheme.typography.titleMedium)
            if (log.isEmpty()) Text("Nothing yet.", style = MaterialTheme.typography.bodySmall)
            log.forEach { Text(it, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
        }
    }
}
