# FocusLite

Personal Android distraction blocker. Blocks Instagram Reels, YouTube Shorts and (experimentally)
Facebook Reels and Instagram Explore, while leaving DMs, profiles and normal videos usable.

Kotlin · Jetpack Compose · Material 3 · MVVM · Room · DataStore · minSdk 29 (Android 10)

Privacy: no internet permission, no account, no analytics. Everything stays on the phone.

## Build (GitHub Actions — no Android Studio needed)

1. Create a **private** repository on GitHub (e.g. `focuslite`).
2. From this folder:
   ```bash
   git init -b main
   git add .
   git commit -m "FocusLite MVP"
   git remote add origin https://github.com/<you>/focuslite.git
   git push -u origin main
   ```
3. Open the repo → **Actions** tab → wait for "Build FocusLite" to go green (~5–8 min first time).
4. Open the run → **Artifacts** → download `FocusLite-debug-apk` → unzip → `app-debug.apk`.
5. If the build is red: open the failed step, copy the error lines, and send them over.

## Install on the phone

```bash
adb install -r ~/Downloads/app-debug.apk
```
Installing with adb avoids Android's "restricted setting" block on the accessibility toggle.
Every CI build is signed with the same debug key (`keystore/debug.keystore`), so `-r` updates
in place and keeps your settings and permission.

## First run

1. Open FocusLite → **Set up** → tick consent → Open Accessibility settings → enable **FocusLite blocker**.
2. Tap **Don't optimise battery**, then in App info allow background activity / auto launch, and lock the app in Recents.
3. Pause AppBlock (or any other blocker) for Instagram/YouTube/Facebook while testing.

## Project layout

```
app/src/main/java/com/focuslite/
  config/      SupportedApps — the only place app package names live
  domain/      Pure Kotlin (no Android): rule engine, rules, policy, action planner  ← unit tested
  data/        Room (block events) + DataStore (settings) repositories
  service/     AccessibilityService, node snapshot, block banner, in-process runtime state
  ui/          Compose screens + ViewModels
app/src/test/  JVM unit tests
```

Detection rules: `domain/rules/DefaultRules.kt`. See DEVELOPMENT_PROGRESS.md for status and limitations.
