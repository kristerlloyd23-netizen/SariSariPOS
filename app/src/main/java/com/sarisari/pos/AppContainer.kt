package com.sarisari.pos

import android.content.Context
import com.sarisari.pos.data.local.AppDatabase
import com.sarisari.pos.data.repository.BackupRepository
import com.sarisari.pos.data.repository.AssistantRepository
import com.sarisari.pos.data.repository.CustomerRepository
import com.sarisari.pos.data.repository.ProductRepository
import com.sarisari.pos.data.repository.ProductImageRepository
import com.sarisari.pos.data.repository.ReportsRepository
import com.sarisari.pos.data.repository.SalesRepository
import com.sarisari.pos.data.repository.SettingsRepository
import com.sarisari.pos.data.repository.OpenFoodFactsRepository

/** Manual dependency injection container (keeps the build light – no Hilt/Dagger needed). */
class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.getInstance(context)
    val settingsRepository = SettingsRepository(context.applicationContext)
    val productRepository = ProductRepository(database)
    val productImageRepository = ProductImageRepository(context.applicationContext)
    val productCatalogRepository = OpenFoodFactsRepository()
    val customerRepository = CustomerRepository(database)
    val salesRepository = SalesRepository(database)
    val reportsRepository = ReportsRepository(database)
    val assistantRepository = AssistantRepository(database, settingsRepository)
    val backupRepository = BackupRepository(context.applicationContext, database, settingsRepository)
}
