package com.sarisari.pos.data.repository

import android.content.Context
import com.sarisari.pos.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Offline-first "sync": unsynced sales are exported as timestamped ZIP archives containing a JSON manifest and product images in
 * app storage (files/backups) and then flagged isSynced = 1. A full snapshot of products
 * and customer balances is written alongside so the store can be restored or audited.
 *
 * To sync to a real server later, replace [pushBatch] with an HTTP call – the
 * selection/marking logic and WorkManager scheduling stay the same.
 */
class BackupRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsRepository
) {
    val backupDir: File get() = File(context.filesDir, "backups").apply { mkdirs() }

    data class SyncOutcome(val transactionsSynced: Int, val file: File?)

    suspend fun syncNow(): SyncOutcome = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val txDao = db.transactionDao()
        val unsynced = txDao.getUnsynced()
        val payments = db.customerDao().getPaymentsSince(settings.settings.value.lastSyncAt)

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(now))
        val sales = JSONArray()
        unsynced.forEach { tw ->
            val t = tw.transaction
            sales.put(JSONObject().apply {
                put("id", t.id)
                put("timestamp", t.timestamp)
                put("totalAmount", t.totalAmount)
                put("paymentType", t.paymentType)
                put("customerId", t.customerId ?: JSONObject.NULL)
                put("items", JSONArray().apply {
                    tw.items.forEach { i ->
                        put(JSONObject().apply {
                            put("productId", i.productId)
                            put("productName", i.productName)
                            put("quantity", i.quantity)
                            put("priceAtSale", i.priceAtSale)
                            put("costAtSale", i.costAtSale)
                        })
                    }
                })
            })
        }
        val paymentsJson = JSONArray()
        payments.forEach { p ->
            paymentsJson.put(JSONObject().apply {
                put("id", p.id); put("customerId", p.customerId)
                put("amountPaid", p.amountPaid); put("timestamp", p.timestamp)
            })
        }

        val root = JSONObject().apply {
            put("store", settings.settings.value.storeName)
            put("exportedAt", now)
            put("sales", sales)
            put("utangPayments", paymentsJson)
            put("snapshot", snapshot())
        }

        val imageFiles = db.productDao().observeAll().first().mapNotNull { product ->
            product.imagePath?.let { relative ->
                val candidate = File(context.filesDir, relative)
                candidate.takeIf { it.isFile && it.canonicalPath.startsWith(File(context.filesDir, "product_images").canonicalPath + File.separator) }
            }
        }.distinctBy { it.name }
        val file = File(backupDir, "sarisari_sync_$stamp.zip")
        pushBatch(root, file, imageFiles)

        if (unsynced.isNotEmpty()) txDao.markSynced(unsynced.map { it.transaction.id })
        settings.markSynced(now)
        pruneOldBackups(keep = 30)
        SyncOutcome(unsynced.size, file)
    }

    fun latestBackup(): File? = backupDir.listFiles()?.maxByOrNull { it.lastModified() }

    /** Package the JSON manifest and image bytes so exported backups remain self-contained. */
    private fun pushBatch(payload: JSONObject, file: File, imageFiles: List<File>) {
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write(payload.toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            imageFiles.forEach { image ->
                zip.putNextEntry(ZipEntry("product_images/${image.name}"))
                FileInputStream(image).use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    private suspend fun snapshot(): JSONObject {
        val products = JSONArray()
        db.productDao().observeAll().first().forEach { p ->
                    products.put(JSONObject().apply {
                        put("id", p.id); put("name", p.name); put("category", p.category)
                        put("barcode", p.barcode ?: JSONObject.NULL)
                        put("costPrice", p.costPrice); put("sellingPrice", p.sellingPrice)
                        put("stockQuantity", p.stockQuantity)
                        put("isBreakBulk", p.isBreakBulk)
                        put("parentProductId", p.parentProductId ?: JSONObject.NULL)
                        put("conversionRate", p.conversionRate)
                        put("imagePath", p.imagePath ?: JSONObject.NULL)
                        put("imageSource", p.imageSource ?: JSONObject.NULL)
                    })
        }
        val customers = JSONArray()
        db.customerDao().observeAll().first().forEach { c ->
            customers.put(JSONObject().apply {
                put("id", c.id); put("name", c.name)
                put("phoneNumber", c.phoneNumber ?: JSONObject.NULL)
                put("creditLimit", c.creditLimit); put("currentBalance", c.currentBalance)
            })
        }
        return JSONObject().apply {
            put("products", products)
            put("customers", customers)
        }
    }

    private fun pruneOldBackups(keep: Int) {
        val files = backupDir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        files.drop(keep).forEach { it.delete() }
    }
}
