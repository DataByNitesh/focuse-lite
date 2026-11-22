package com.focuslite.domain.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyTest {

    private val targets = setOf("a", "b")

    @Test fun activeSettingsReturnEnabledTargets() {
        assertEquals(targets, BlockingPolicy.effectiveTargets(FocusSettings(enabledTargets = targets), 1_000))
    }

    @Test fun masterSwitchOffBlocksNothing() {
        assertTrue(BlockingPolicy.effectiveTargets(FocusSettings(masterEnabled = false, enabledTargets = targets), 1_000).isEmpty())
    }

    @Test fun pauseBlocksNothingUntilItExpires() {
        val s = FocusSettings(enabledTargets = targets, pausedUntilMillis = 5_000)
        assertTrue(BlockingPolicy.effectiveTargets(s, 4_999).isEmpty())
        assertEquals(targets, BlockingPolicy.effectiveTargets(s, 5_000))
    }

    @Test fun statusPriorities() {
        val s = FocusSettings(enabledTargets = targets)
        assertEquals(ProtectionStatus.SERVICE_OFF, ProtectionStatusResolver.resolve(false, s, 0))
        assertEquals(ProtectionStatus.ACTIVE, ProtectionStatusResolver.resolve(true, s, 0))
        assertEquals(ProtectionStatus.DISABLED, ProtectionStatusResolver.resolve(true, s.copy(masterEnabled = false), 0))
        assertEquals(ProtectionStatus.PAUSED, ProtectionStatusResolver.resolve(true, s.copy(pausedUntilMillis = 10), 0))
        assertEquals(ProtectionStatus.NO_RULES, ProtectionStatusResolver.resolve(true, s.copy(enabledTargets = emptySet()), 0))
    }

    @Test fun estimateIsSimpleAndNonNegative() {
        assertEquals(10, Estimates.minutesAvoided(5))
        assertEquals(0, Estimates.minutesAvoided(-3))
    }

    @Test fun plannerPressesBackFirst() {
        val p = BlockActionPlanner()
        val d = p.onDetected(0)
        assertEquals(BlockAction.BACK, d.action)
        assertTrue(d.countAsAttempt)
    }

    @Test fun plannerIgnoresDetectionsDuringCooldown() {
        val p = BlockActionPlanner(cooldownMs = 500)
        p.onDetected(0)
        assertEquals(BlockAction.NONE, p.onDetected(300).action)
    }

    @Test fun plannerEscalatesToHomeWhenBackLoops() {
        val p = BlockActionPlanner(cooldownMs = 500, escalateAfter = 3, escalationWindowMs = 6_000)
        assertEquals(BlockAction.BACK, p.onDetected(0).action)
        assertEquals(BlockAction.BACK, p.onDetected(1_000).action)
        assertEquals(BlockAction.HOME, p.onDetected(2_000).action)
        // After escalating, the cycle restarts.
        assertEquals(BlockAction.BACK, p.onDetected(3_000).action)
    }

    @Test fun plannerDoesNotEscalateForSpreadOutVisits() {
        val p = BlockActionPlanner(escalateAfter = 3, escalationWindowMs = 6_000)
        assertEquals(BlockAction.BACK, p.onDetected(0).action)
        assertEquals(BlockAction.BACK, p.onDetected(10_000).action)
        assertEquals(BlockAction.BACK, p.onDetected(20_000).action)
    }

    @Test fun repeatedReactionsCountAsOneAttempt() {
        val p = BlockActionPlanner(attemptMergeWindowMs = 4_000)
        assertTrue(p.onDetected(0).countAsAttempt)
        assertFalse(p.onDetected(1_000).countAsAttempt)
        assertFalse(p.onDetected(2_000).countAsAttempt)
        assertTrue(p.onDetected(10_000).countAsAttempt)
    }
}
