package com.focuslite.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.focuslite.FocusLiteApp
import com.focuslite.ui.dashboard.DashboardScreen
import com.focuslite.ui.diagnostics.DiagnosticsScreen
import com.focuslite.ui.settings.SettingsScreen
import com.focuslite.ui.setup.SetupScreen
import com.focuslite.ui.theme.FocusLiteTheme

object Routes {
    const val DASHBOARD = "dashboard"
    const val SETUP = "setup"
    const val DIAGNOSTICS = "diagnostics"
    const val SETTINGS = "settings"
}

/** The only Activity. Compose + Navigation handle all screens inside it (like a SPA router). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as FocusLiteApp).container
        setContent {
            FocusLiteTheme {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = Routes.DASHBOARD) {
                    composable(Routes.DASHBOARD) {
                        DashboardScreen(
                            container = container,
                            onOpenSetup = { nav.navigate(Routes.SETUP) },
                            onOpenDiagnostics = { nav.navigate(Routes.DIAGNOSTICS) },
                            onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                        )
                    }
                    composable(Routes.SETUP) { SetupScreen(container, onBack = { nav.popBackStack() }) }
                    composable(Routes.DIAGNOSTICS) { DiagnosticsScreen(onBack = { nav.popBackStack() }) }
                    composable(Routes.SETTINGS) { SettingsScreen(container, onBack = { nav.popBackStack() }) }
                }
            }
        }
    }
}
