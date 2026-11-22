package com.focuslite.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.focuslite.AppContainer
import com.focuslite.data.SettingsRepository
import com.focuslite.data.StatsRepository
import com.focuslite.domain.policy.FocusSettings
import com.focuslite.domain.policy.ProtectionStatus
import com.focuslite.domain.policy.ProtectionStatusResolver
import com.focuslite.domain.rules.DetectionRule
import com.focuslite.domain.rules.RuleEngine
import com.focuslite.service.ServiceRuntime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RuleRow(val rule: DetectionRule, val enabled: Boolean, val blockedToday: Int)

data class DashboardState(
    val settings: FocusSettings,
    val status: ProtectionStatus,
    val blockedToday: Int,
    val rules: List<RuleRow>,
)

/**
 * ViewModel = screen state holder that survives rotation. It exposes a StateFlow that the
 * Compose UI collects (similar to a React hook returning state derived from several stores).
 */
class DashboardViewModel(
    private val settingsRepository: SettingsRepository,
    statsRepository: StatsRepository,
    private val ruleEngine: RuleEngine,
) : ViewModel() {

    /** Emits the current time periodically so "paused until" expires on screen without user action. */
    private val clock = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(15_000)
        }
    }

    val state: StateFlow<DashboardState?> = combine(
        settingsRepository.settings,
        statsRepository.countToday(),
        statsRepository.countsByTargetToday(),
        ServiceRuntime.running,
        clock,
    ) { settings, today, byTarget, running, now ->
        DashboardState(
            settings = settings,
            status = ProtectionStatusResolver.resolve(running, settings, now),
            blockedToday = today,
            rules = ruleEngine.rules.map { RuleRow(it, it.targetId in settings.enabledTargets, byTarget[it.targetId] ?: 0) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setTargetEnabled(targetId: String, enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTargetEnabled(targetId, enabled) }
    }

    fun setMasterEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMasterEnabled(enabled) }
    }

    fun pauseFor(minutes: Int) {
        viewModelScope.launch { settingsRepository.pauseUntil(System.currentTimeMillis() + minutes * 60_000L) }
    }

    fun resume() {
        viewModelScope.launch { settingsRepository.pauseUntil(0L) }
    }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory {
            initializer { DashboardViewModel(c.settingsRepository, c.statsRepository, c.ruleEngine) }
        }
    }
}
