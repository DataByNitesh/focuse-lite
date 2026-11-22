package com.focuslite.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.focuslite.domain.policy.FocusSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// DataStore = small async key-value storage (think localStorage, but async and crash-safe).
// The delegate must be top-level so there is exactly one instance per file.
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context, private val defaultTargets: Set<String>) {

    private val store = context.applicationContext.settingsDataStore

    private object Keys {
        val MASTER = booleanPreferencesKey("master_enabled")
        val TARGETS = stringSetPreferencesKey("enabled_targets")
        val PAUSED_UNTIL = longPreferencesKey("paused_until")
        val CONSENT = booleanPreferencesKey("consent_accepted")
    }

    val settings: Flow<FocusSettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            FocusSettings(
                masterEnabled = p[Keys.MASTER] ?: true,
                enabledTargets = p[Keys.TARGETS] ?: defaultTargets,
                pausedUntilMillis = p[Keys.PAUSED_UNTIL] ?: 0L,
                consentAccepted = p[Keys.CONSENT] ?: false,
            )
        }

    suspend fun setTargetEnabled(targetId: String, enabled: Boolean) {
        store.edit { p ->
            val current = p[Keys.TARGETS] ?: defaultTargets
            p[Keys.TARGETS] = if (enabled) current + targetId else current - targetId
        }
    }

    suspend fun setMasterEnabled(enabled: Boolean) {
        store.edit { it[Keys.MASTER] = enabled }
    }

    suspend fun pauseUntil(epochMillis: Long) {
        store.edit { it[Keys.PAUSED_UNTIL] = epochMillis }
    }

    suspend fun setConsentAccepted(accepted: Boolean) {
        store.edit { it[Keys.CONSENT] = accepted }
    }
}
