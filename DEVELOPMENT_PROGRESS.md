# FocusLite — Development Progress

_Last updated: 2026-11-22_

## Status summary

| Area | State | Verified how |
|---|---|---|
| Rule engine, matchers, default rules | Written | 15 JVM unit tests pass (cloud, Kotlin 2.0.21 + JUnit 4) |
| Block policy, pause, action planner (Back → Home escalation) | Written | 11 JVM unit tests pass |
| Accessibility service, overlay banner | Written | **Not yet compiled by Gradle, not yet run on device** |
| Room stats, DataStore settings | Written | Not yet compiled by Gradle |
| UI: Dashboard, Setup, Diagnostics, Settings | Written | Not yet compiled by Gradle |
| GitHub Actions build | Written | Not yet run |

**Nothing is claimed to block anything on a real phone yet.** Each target below needs a device check.

## Target detection status

| Target | Rule reliability | Default | Device-verified |
|---|---|---|---|
| YouTube Shorts | Expected (player view ids) | On | ❌ not yet |
| Instagram Reels (tab, feed, DM) | Expected (viewer ids + selected tab) | On | ❌ not yet |
| Instagram Explore | Experimental (shares tab with Search) | Off | ❌ not yet |
| Facebook Reels | Experimental (English "Reels" label only) | On | ❌ not yet |

View ids in `DefaultRules.kt` are candidates from earlier app versions; adb recon on the Realme P4
was abandoned because `uiautomator dump` fails on video screens ("could not get idle state").
The in-app **Diagnostics** screen replaces it.

## Completed

- M0 (partial): environment inspection; adb connection to Realme P4 working; adb recon replaced by in-app Diagnostics.
- M1–M3 code: project skeleton, theme, navigation, pure rule engine + tests, accessibility service,
  consent/permission setup, block banner, Back/Home loop protection, local stats, delete-data, CI workflow.

## Known issues / risks

- Android code has never been compiled; expect a first round of build fixes from CI output.
- Today's count uses the day boundary from when the dashboard opened (stale if left open past midnight).
- Snapshot walks up to 1500 nodes per check; battery cost on video apps must be measured on device.
- A Reel opened from a DM is blocked too (same viewer). Allowing it is a possible future option.
- AppBlock is installed on the test phone and also uses accessibility — may conflict while both are active.
- Realme UI may kill the service; the dashboard shows "enabled but not running" in that case.
- Android 17 Advanced Protection Mode blocks non-accessibility-tool services (we must not set isAccessibilityTool).
- No launcher icon yet (uses Android default).

## Next steps

1. Push to GitHub, fix any CI compile errors.
2. Install on Realme P4, enable service, run the device checklist below.
3. Use Diagnostics to capture: IG Reels tab, IG Reel from feed, IG Reel from DM, IG Explore, IG search typing,
   IG DM inbox, YT Shorts tab, YT Short from home, YT normal video, FB home, FB Reels. Update rules + add the
   captures as unit-test fixtures.
4. M6: whole-app blocking, schedules, focus sessions, emergency exit.
5. M7: statistics screen (daily/weekly). M8: battery/reboot hardening.

## Device test checklist (manual, Realme P4)

- [ ] Service can be enabled; dashboard shows "Blocking is active"
- [ ] YouTube: Shorts tab → sent back; Short from home shelf → sent back; normal video plays
- [ ] Instagram: Reels tab → back; Reel in feed tapped → back; DMs, profile, stories usable
- [ ] Instagram: Reel opened from DM → back to the chat
- [ ] Facebook (experimental): Reels → back; home feed usable
- [ ] Pause 5 min → nothing blocked; resumes automatically
- [ ] Reboot phone → open Instagram Reels without opening FocusLite first → still blocked
- [ ] Swipe FocusLite away from Recents → still blocked
- [ ] Overnight battery: FocusLite share in Settings → Battery
