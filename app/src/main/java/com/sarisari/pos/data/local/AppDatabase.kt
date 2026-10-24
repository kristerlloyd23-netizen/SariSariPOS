package com.sarisari.pos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sarisari.pos.data.local.dao.CustomerDao
import com.sarisari.pos.data.local.dao.AssistantDao
import com.sarisari.pos.data.local.dao.ProductDao
import com.sarisari.pos.data.local.dao.TransactionDao
import com.sarisari.pos.data.local.entity.CustomerEntity
import com.sarisari.pos.data.local.entity.AssistantChunkEntity
import com.sarisari.pos.data.local.entity.AssistantDocumentEntity
import com.sarisari.pos.data.local.entity.ProductEntity
import com.sarisari.pos.data.local.entity.TransactionEntity
import com.sarisari.pos.data.local.entity.TransactionItemEntity
import com.sarisari.pos.data.local.entity.UtangPaymentEntity

@Database(
    entities = [
        ProductEntity::class,
        CustomerEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        UtangPaymentEntity::class,
        AssistantDocumentEntity::class,
        AssistantChunkEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun assistantDao(): AssistantDao

    companion object {
        private const val DB_NAME = "sarisari_pos.db"

        /** Preserves existing products, sales and Utang data while adding product-photo + RAG tables. */
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN imagePath TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS assistant_documents (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, content TEXT NOT NULL, category TEXT NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS assistant_chunks USING FTS4(documentId INTEGER NOT NULL, title TEXT NOT NULL, content TEXT NOT NULL)")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN imageSource TEXT")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
    }
}
