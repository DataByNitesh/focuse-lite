package com.focuslite.domain.rules

import com.focuslite.config.SupportedApps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fake screens are hand-written approximations. Once real Diagnostics captures exist,
 * add them as fixtures so every rule change is checked against real screens.
 */
class RuleEngineTest {

    private val engine = RuleEngine(DefaultRules.all)
    private val allTargets = DefaultRules.all.map { it.targetId }.toSet()

    private val ig = SupportedApps.INSTAGRAM.packageName
    private val yt = SupportedApps.YOUTUBE.packageName
    private val fb = SupportedApps.FACEBOOK.packageName

    private fun screen(vararg nodes: UiNode) = UiNode(className = "FrameLayout", children = nodes.toList())
    private fun tabBar(selectedId: String) = UiNode(
        viewId = "tab_bar",
        children = listOf("feed_tab", "search_tab", "clips_tab", "profile_tab").map {
            UiNode(viewId = it, className = "FrameLayout", isSelected = it == selectedId)
        },
    )

    @Test fun instagramReelsTabIsDetected() {
        val s = screen(UiNode(viewId = "clips_viewer_view_pager"), tabBar("clips_tab"))
        assertEquals(DefaultRules.INSTAGRAM_REELS, engine.evaluate(ig, s, allTargets)?.targetId)
    }

    @Test fun instagramReelOpenedFromDmIsDetected() {
        val s = screen(UiNode(viewId = "clips_viewer_container"))
        assertEquals(DefaultRules.INSTAGRAM_REELS, engine.evaluate(ig, s, allTargets)?.targetId)
    }

    @Test fun instagramHomeFeedAndDmsAreAllowed() {
        assertNull(engine.evaluate(ig, screen(UiNode(viewId = "feed_list"), tabBar("feed_tab")), allTargets))
        assertNull(engine.evaluate(ig, screen(UiNode(viewId = "direct_inbox_list")), allTargets))
    }

    @Test fun unselectedReelsTabAloneIsNotABlock() {
        // The Reels tab button is visible on every main screen; only selected counts.
        assertNull(engine.evaluate(ig, screen(tabBar("profile_tab")), allTargets))
    }

    @Test fun exploreBlockedOnlyWhenNotTyping() {
        val browsing = screen(UiNode(viewId = "explore_grid"), tabBar("search_tab"))
        val typing = screen(UiNode(viewId = "search_field", isEditableFocused = true), tabBar("search_tab"))
        assertEquals(DefaultRules.INSTAGRAM_EXPLORE, engine.evaluate(ig, browsing, allTargets)?.targetId)
        assertNull(engine.evaluate(ig, typing, allTargets))
    }

    @Test fun disabledTargetIsNotBlocked() {
        val s = screen(UiNode(viewId = "clips_viewer_view_pager"))
        assertNull(engine.evaluate(ig, s, allTargets - DefaultRules.INSTAGRAM_REELS))
    }

    @Test fun youtubeShortsPlayerIsDetected() {
        val s = screen(UiNode(viewId = "reel_player_page_container", children = listOf(UiNode(viewId = "reel_recycler"))))
        assertEquals(DefaultRules.YOUTUBE_SHORTS, engine.evaluate(yt, s, allTargets)?.targetId)
    }

    @Test fun youtubeNormalVideoIsAllowed() {
        val s = screen(UiNode(viewId = "watch_player"), UiNode(viewId = "watch_list"))
        assertNull(engine.evaluate(yt, s, allTargets))
    }

    @Test fun rulesDoNotLeakAcrossApps() {
        // An Instagram-looking id inside YouTube must not trigger the Instagram rule.
        val s = screen(UiNode(viewId = "clips_viewer_view_pager"))
        assertNull(engine.evaluate(yt, s, allTargets))
        assertNull(engine.evaluate("com.example.other", s, allTargets))
    }

    @Test fun facebookReelsSelectedTabIsDetectedCaseInsensitive() {
        val s = screen(UiNode(contentDesc = "reels, tab 3 of 6", isSelected = true))
        assertEquals(DefaultRules.FACEBOOK_REELS, engine.evaluate(fb, s, allTargets)?.targetId)
    }

    @Test fun facebookReelsUnselectedIsAllowed() {
        val s = screen(UiNode(contentDesc = "Reels, tab 3 of 6", isSelected = false), UiNode(contentDesc = "Home", isSelected = true))
        assertNull(engine.evaluate(fb, s, allTargets))
    }

    @Test fun monitoredPackagesComeFromRules() {
        assertEquals(setOf(ig, yt, fb), engine.monitoredPackages)
    }

    @Test fun matchingRulesIgnoresEnabledState() {
        val s = screen(UiNode(viewId = "clips_viewer_view_pager"))
        assertEquals(listOf(DefaultRules.INSTAGRAM_REELS), engine.matchingRules(ig, s).map { it.targetId })
    }

    @Test fun deepTreesAreFlattened() {
        var n = UiNode(viewId = "reel_recycler")
        repeat(200) { n = UiNode(className = "FrameLayout", children = listOf(n)) }
        assertEquals(DefaultRules.YOUTUBE_SHORTS, engine.evaluate(yt, n, allTargets)?.targetId)
    }

    @Test fun screenDumpContainsIdsAndFlagsOnly() {
        val s = screen(UiNode(viewId = "clips_tab", className = "FrameLayout", contentDesc = "Reels", isSelected = true))
        val dump = ScreenDump.format(s)
        assertTrue(dump.contains("FrameLayout #clips_tab [Reels] *selected"))
    }

    @Test fun defaultsEnableExpectedRulesButNotExplore() {
        assertTrue(DefaultRules.YOUTUBE_SHORTS in DefaultRules.defaultEnabledTargets)
        assertTrue(DefaultRules.INSTAGRAM_REELS in DefaultRules.defaultEnabledTargets)
        assertTrue(DefaultRules.INSTAGRAM_EXPLORE !in DefaultRules.defaultEnabledTargets)
    }
}
