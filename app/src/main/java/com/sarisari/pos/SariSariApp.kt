package com.sarisari.pos

import android.app.Application
import com.sarisari.pos.data.local.SeedData
import com.sarisari.pos.data.worker.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SariSariApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Seed demo data once, then attach the bundled attributed photo without replacing owner photos.
        if (!container.settingsRepository.isSeeded || !container.settingsRepository.bundledImageSeeded) {
            appScope.launch {
                if (!container.settingsRepository.isSeeded) {
                    SeedData.seed(container.database)
                    container.settingsRepository.isSeeded = true
                }
                if (!container.settingsRepository.bundledImageSeeded) {
                    runCatching { SeedData.addBundledProductImageIfMissing(container.database, container.productImageRepository) }
                    container.settingsRepository.bundledImageSeeded = true
                }
            }
        }
        appScope.launch { container.assistantRepository.seedKnowledgeIfEmpty() }
        SyncWorker.schedulePeriodic(this)
    }
}
