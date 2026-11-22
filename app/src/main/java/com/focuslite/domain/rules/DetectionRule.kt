package com.focuslite.domain.rules

enum class Reliability {
    /** Based on view ids that have been stable across many app versions. Still needs device confirmation. */
    EXPECTED,

    /** Weak signals (e.g. localized labels) or known side effects. Off by default or clearly labelled. */
    EXPERIMENTAL,
}

/** One blockable section inside one app, e.g. "Instagram Reels". */
data class DetectionRule(
    val targetId: String,
    val packageName: String,
    val title: String,
    val reliability: Reliability,
    val defaultEnabled: Boolean,
    val matcher: Matcher,
    /** Human notes: which signals are used, known limitations, and which app version they were checked against. */
    val notes: String,
)
