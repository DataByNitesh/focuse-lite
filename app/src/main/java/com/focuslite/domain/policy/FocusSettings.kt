package com.focuslite.domain.policy

/** User settings that control blocking. Stored in DataStore; plain data here. */
data class FocusSettings(
    val masterEnabled: Boolean = true,
    val enabledTargets: Set<String> = emptySet(),
    /** Epoch millis until which blocking is paused. 0 = not paused. */
    val pausedUntilMillis: Long = 0L,
    val consentAccepted: Boolean = false,
)

object BlockingPolicy {
    fun isPaused(settings: FocusSettings, nowMillis: Long): Boolean = nowMillis < settings.pausedUntilMillis

    /** Targets that should actually be blocked right now. Evaluated on every check, so it survives reboots for free. */
    fun effectiveTargets(settings: FocusSettings, nowMillis: Long): Set<String> =
        if (!settings.masterEnabled || isPaused(settings, nowMillis)) emptySet() else settings.enabledTargets
}

enum class ProtectionStatus { SERVICE_OFF, DISABLED, PAUSED, NO_RULES, ACTIVE }

object ProtectionStatusResolver {
    fun resolve(serviceRunning: Boolean, settings: FocusSettings, nowMillis: Long): ProtectionStatus = when {
        !serviceRunning -> ProtectionStatus.SERVICE_OFF
        !settings.masterEnabled -> ProtectionStatus.DISABLED
        BlockingPolicy.isPaused(settings, nowMillis) -> ProtectionStatus.PAUSED
        settings.enabledTargets.isEmpty() -> ProtectionStatus.NO_RULES
        else -> ProtectionStatus.ACTIVE
    }
}

object Estimates {
    /** Assumption used for the "estimated time avoided" figure. Clearly an estimate, not a measurement. */
    const val ASSUMED_MINUTES_PER_ATTEMPT = 2

    fun minutesAvoided(attempts: Int, minutesPerAttempt: Int = ASSUMED_MINUTES_PER_ATTEMPT): Int =
        attempts.coerceAtLeast(0) * minutesPerAttempt
}
