package com.focuslite.domain.policy

enum class BlockAction { NONE, BACK, HOME }

data class BlockDecision(val action: BlockAction, val countAsAttempt: Boolean)

/**
 * Decides HOW to react when a blocked screen is detected, without causing loops.
 *
 * - Pressing Back is the gentle default (returns you to the previous screen, e.g. your DMs).
 * - If Back keeps landing on a blocked screen (e.g. Reels is the root tab), escalate to Home.
 * - A short cooldown avoids reacting several times to the same screen transition.
 * - Repeated reactions within a few seconds count as ONE blocked attempt in statistics.
 *
 * Times are monotonic milliseconds (SystemClock.elapsedRealtime on device).
 */
class BlockActionPlanner(
    private val cooldownMs: Long = 500,
    private val escalateAfter: Int = 3,
    private val escalationWindowMs: Long = 6_000,
    private val attemptMergeWindowMs: Long = 4_000,
) {
    private val recentActions = ArrayDeque<Long>()
    private var lastActionAt: Long? = null
    private var lastAttemptAt: Long? = null

    fun onDetected(now: Long): BlockDecision {
        val last = lastActionAt
        if (last != null && now - last < cooldownMs) return BlockDecision(BlockAction.NONE, false)

        while (recentActions.isNotEmpty() && now - recentActions.first() > escalationWindowMs) {
            recentActions.removeFirst()
        }
        recentActions.addLast(now)
        lastActionAt = now

        val prevAttempt = lastAttemptAt
        val newAttempt = prevAttempt == null || now - prevAttempt > attemptMergeWindowMs
        lastAttemptAt = now

        val action = if (recentActions.size >= escalateAfter) {
            recentActions.clear()
            BlockAction.HOME
        } else {
            BlockAction.BACK
        }
        return BlockDecision(action, newAttempt)
    }
}
