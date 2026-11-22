package com.focuslite.config

/**
 * The single place where package names of supported apps live.
 *
 * Note: AndroidManifest.xml also lists these packages inside <queries> (Android 11+ requires
 * apps to declare which other packages they want to "see"). XML can't read Kotlin constants,
 * so if you add an app here, add it there too.
 */
data class SupportedApp(
    val id: String,
    val packageName: String,
    val displayName: String,
)

object SupportedApps {
    val INSTAGRAM = SupportedApp("instagram", "com.instagram.android", "Instagram")
    val YOUTUBE = SupportedApp("youtube", "com.google.android.youtube", "YouTube")
    val FACEBOOK = SupportedApp("facebook", "com.facebook.katana", "Facebook")

    val all: List<SupportedApp> = listOf(INSTAGRAM, YOUTUBE, FACEBOOK)

    fun byPackage(packageName: String): SupportedApp? = all.firstOrNull { it.packageName == packageName }
}
