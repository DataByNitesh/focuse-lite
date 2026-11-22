package com.focuslite.domain.rules

import com.focuslite.config.SupportedApps

/**
 * App-specific detection rules. When an app update breaks detection, this is the file to change.
 *
 * IMPORTANT: The view ids below are candidates based on previously observed app versions.
 * They have NOT yet been confirmed on the Realme P4. Use the in-app Diagnostics screen to
 * capture the real screen structure, then update these rules and the "notes" field.
 */
object DefaultRules {

    const val YOUTUBE_SHORTS = "youtube_shorts"
    const val INSTAGRAM_REELS = "instagram_reels"
    const val INSTAGRAM_EXPLORE = "instagram_explore"
    const val FACEBOOK_REELS = "facebook_reels"

    val youtubeShorts = DetectionRule(
        targetId = YOUTUBE_SHORTS,
        packageName = SupportedApps.YOUTUBE.packageName,
        title = "YouTube Shorts",
        reliability = Reliability.EXPECTED,
        defaultEnabled = true,
        // Detect the Shorts *player*, so every entry point (tab, home shelf, links) is covered.
        matcher = anyOf(
            viewId("reel_player_page_container"),
            viewId("reel_recycler"),
            viewId("reel_watch_player"),
        ),
        notes = "Signals: Shorts player container ids. Unverified on device.",
    )

    val instagramReels = DetectionRule(
        targetId = INSTAGRAM_REELS,
        packageName = SupportedApps.INSTAGRAM.packageName,
        title = "Instagram Reels",
        reliability = Reliability.EXPECTED,
        defaultEnabled = true,
        matcher = anyOf(
            viewId("clips_viewer_view_pager"),
            viewId("clips_viewer_container"),
            viewId("clips_tab", selected = true),
        ),
        notes = "Signals: Reels viewer ids + selected Reels tab. Reels opened from DMs use the same " +
            "viewer and are blocked too. Unverified on device.",
    )

    val instagramExplore = DetectionRule(
        targetId = INSTAGRAM_EXPLORE,
        packageName = SupportedApps.INSTAGRAM.packageName,
        title = "Instagram Explore",
        reliability = Reliability.EXPERIMENTAL,
        defaultEnabled = false,
        // Search tab selected AND the user is not typing a search. Searching for people stays possible
        // only if Instagram focuses the search field before we react — needs device testing.
        matcher = allOf(
            viewId("search_tab", selected = true),
            not(typingInTextField()),
        ),
        notes = "Explore shares a tab with Search, so blocking it may also block search. Off by default.",
    )

    val facebookReels = DetectionRule(
        targetId = FACEBOOK_REELS,
        packageName = SupportedApps.FACEBOOK.packageName,
        title = "Facebook Reels",
        reliability = Reliability.EXPERIMENTAL,
        defaultEnabled = true,
        // Facebook exposes few view ids; rely on the selected tab's accessibility label.
        // Label is language-dependent (English only for now).
        matcher = descStartsWith("Reels", selected = true),
        notes = "Signal: selected element labelled 'Reels…'. English UI only. Unverified on device.",
    )

    val all: List<DetectionRule> = listOf(youtubeShorts, instagramReels, instagramExplore, facebookReels)

    val defaultEnabledTargets: Set<String> = all.filter { it.defaultEnabled }.map { it.targetId }.toSet()
}
