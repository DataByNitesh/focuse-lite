package com.focuslite

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.focuslite.data.SettingsRepository
import com.focuslite.data.StatsRepository
import com.focuslite.data.db.FocusDatabase
import com.focuslite.domain.rules.DefaultRules
import com.focuslite.domain.rules.RuleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency injection: one object that creates and holds the app's singletons.
 * (Hilt would generate this for us; at this size a hand-written container is simpler.)
 */
class AppContainer(context: Context) {
    val ruleEngine = RuleEngine(DefaultRules.all)

    private val database: FocusDatabase =
        Room.databaseBuilder(context.applicationContext, FocusDatabase::class.java, "focuslite.db").build()

    val settingsRepository = SettingsRepository(context, DefaultRules.defaultEnabledTargets)
    val statsRepository = StatsRepository(database.blockEventDao())

    /** For work that must finish even if a screen closes (e.g. writing a block event). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

/** Application subclass: created once per process, before any Activity or Service. */
class FocusLiteApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
