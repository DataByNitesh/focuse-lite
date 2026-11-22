package com.focuslite.domain.rules

/**
 * Pure function core of the blocker: given a screen snapshot from an app,
 * return the first enabled rule that matches it (or null).
 */
class RuleEngine(val rules: List<DetectionRule>) {

    private val byPackage: Map<String, List<DetectionRule>> = rules.groupBy { it.packageName }

    /** Packages the accessibility service needs events from. */
    val monitoredPackages: Set<String> = byPackage.keys

    fun evaluate(packageName: String, root: UiNode, enabledTargets: Set<String>): DetectionRule? {
        val candidates = byPackage[packageName]?.filter { it.targetId in enabledTargets }
        if (candidates.isNullOrEmpty()) return null
        val nodes = root.flatten()
        return candidates.firstOrNull { it.matcher.matches(nodes) }
    }

    /** All rules (enabled or not) matching this screen. Used by the Diagnostics screen. */
    fun matchingRules(packageName: String, root: UiNode): List<DetectionRule> {
        val candidates = byPackage[packageName] ?: return emptyList()
        val nodes = root.flatten()
        return candidates.filter { it.matcher.matches(nodes) }
    }

    fun ruleFor(targetId: String): DetectionRule? = rules.firstOrNull { it.targetId == targetId }
}
