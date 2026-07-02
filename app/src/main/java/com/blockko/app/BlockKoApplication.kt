package com.blockko.app

import android.app.Application
import com.blockko.app.data.apps.InstalledAppsRepository
import com.blockko.app.data.blocklist.BlocklistRepository
import com.blockko.app.data.blocklist.StatsRepository
import com.blockko.app.data.datastore.SettingsRepository
import com.blockko.app.data.db.BlockKoDatabase
import com.blockko.app.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BlockKoApplication : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: BlockKoDatabase by lazy { BlockKoDatabase.getInstance(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val blocklistRepository: BlocklistRepository by lazy { BlocklistRepository(this, database) }
    val statsRepository: StatsRepository by lazy { StatsRepository(database) }
    val installedAppsRepository: InstalledAppsRepository by lazy { InstalledAppsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
        appScope.launch { blocklistRepository.ensureInitialLoadFromBundledAsset() }
        appScope.launch { statsRepository.pruneOldEvents() }
    }
}
