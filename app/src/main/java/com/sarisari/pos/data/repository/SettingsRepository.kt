package com.sarisari.pos.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

enum class Wallet(val key: String, val label: String) {
    GCASH("gcash", "GCash"),
    MAYA("maya", "Maya")
}

data class StoreSettings(
    val storeName: String = "Sari-Sari Store",
    val gcashName: String = "",
    val gcashNumber: String = "",
    val mayaName: String = "",
    val mayaNumber: String = "",
    val assistantBackendUrl: String = "",
    val allowCloudAssistant: Boolean = false,
    val geminiApiKeyConfigured: Boolean = false,
    val lastSyncAt: Long = 0L,
    /** Bumped whenever a QR image changes so the UI reloads it. */
    val qrVersion: Int = 0
)

/** Simple SharedPreferences-backed settings + QR image storage in internal files. */
class SettingsRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("store_settings", Context.MODE_PRIVATE)
    private val geminiKeyStore = GeminiApiKeyStore(context)
    private val _settings = MutableStateFlow(StoreSettings())
    val settings: StateFlow<StoreSettings> = _settings.asStateFlow()

    init {
        _settings.value = load()
    }

    private fun load() = StoreSettings(
        storeName = prefs.getString("store_name", null) ?: "Sari-Sari Store",
        gcashName = prefs.getString("gcash_name", "") ?: "",
        gcashNumber = prefs.getString("gcash_number", "") ?: "",
        mayaName = prefs.getString("maya_name", "") ?: "",
        mayaNumber = prefs.getString("maya_number", "") ?: "",
        assistantBackendUrl = prefs.getString("assistant_backend_url", "") ?: "",
        allowCloudAssistant = prefs.getBoolean("allow_cloud_assistant", false),
        geminiApiKeyConfigured = geminiKeyStore.read() != null,
        lastSyncAt = prefs.getLong("last_sync_at", 0L)
    )

    fun update(transform: (StoreSettings) -> StoreSettings) {
        val next = transform(_settings.value)
        prefs.edit()
            .putString("store_name", next.storeName)
            .putString("gcash_name", next.gcashName)
            .putString("gcash_number", next.gcashNumber)
            .putString("maya_name", next.mayaName)
            .putString("maya_number", next.mayaNumber)
            .putString("assistant_backend_url", next.assistantBackendUrl)
            .putBoolean("allow_cloud_assistant", next.allowCloudAssistant)
            .putLong("last_sync_at", next.lastSyncAt)
            .apply()
        _settings.value = next
    }

    fun markSynced(time: Long) = update { it.copy(lastSyncAt = time) }

    fun getGeminiApiKey(): String? = geminiKeyStore.read()

    fun saveGeminiApiKey(apiKey: String) {
        geminiKeyStore.save(apiKey)
        _settings.value = _settings.value.copy(geminiApiKeyConfigured = geminiKeyStore.read() != null)
    }

    fun removeGeminiApiKey() {
        geminiKeyStore.clear()
        _settings.value = _settings.value.copy(geminiApiKeyConfigured = false)
    }

    var bundledImageSeeded: Boolean
        get() = prefs.getBoolean("bundled_product_image_seeded", false)
        set(value) { prefs.edit().putBoolean("bundled_product_image_seeded", value).apply() }

    var isSeeded: Boolean
        get() = prefs.getBoolean("seeded", false)
        set(value) { prefs.edit().putBoolean("seeded", value).apply() }

    // ---- Static QR images ----

    private fun qrFile(wallet: Wallet) = File(context.filesDir, "qr_${wallet.key}.img")

    fun hasQr(wallet: Wallet): Boolean = qrFile(wallet).exists()

    suspend fun loadQr(wallet: Wallet): Bitmap? = withContext(Dispatchers.IO) {
        val f = qrFile(wallet)
        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
    }

    /** Copies a picked image (e.g. a screenshot of your GCash "Receive money" QR) into app storage. */
    suspend fun saveQr(wallet: Wallet, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val ok = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                qrFile(wallet).outputStream().use { output -> input.copyTo(output) }
            } != null
        }.getOrDefault(false)
        if (ok) _settings.value = _settings.value.copy(qrVersion = _settings.value.qrVersion + 1)
        ok
    }

    suspend fun removeQr(wallet: Wallet) = withContext(Dispatchers.IO) {
        qrFile(wallet).delete()
        _settings.value = _settings.value.copy(qrVersion = _settings.value.qrVersion + 1)
    }
}
