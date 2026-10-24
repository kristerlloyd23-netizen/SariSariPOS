package com.sarisari.pos.data.repository

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/** Imports, normalizes and stores product photos in private app storage. */
class ProductImageRepository(private val context: Context) {
    private val imageDir: File get() = File(context.filesDir, DIRECTORY).apply { mkdirs() }

    suspend fun importUri(uri: Uri): String = withContext(Dispatchers.IO) {
        val source = decodeUri(uri) ?: error("That image could not be opened")
        val rotation = runCatching { context.contentResolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0 }.getOrDefault(0)
        saveBitmap(source, rotation)
    }

    suspend fun importFile(file: File): String = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val options = decodeOptions(bounds.outWidth, bounds.outHeight)
        val source = BitmapFactory.decodeFile(file.absolutePath, options) ?: error("Could not open the photo")
        val rotation = runCatching { ExifInterface(file.absolutePath).rotationDegrees }.getOrDefault(0)
        saveBitmap(source, rotation)
    }

    /** Downloads a catalog photo only after the user selects it; restricts hosts and payload size. */
    suspend fun importRemoteImage(imageUrl: String): String = withContext(Dispatchers.IO) {
        val url = URL(imageUrl)
        require(url.protocol == "https" && url.host.endsWith(".openfoodfacts.org")) { "Only Open Food Facts image links are supported." }
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 12_000
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", "SariSariPOS/1.1 (Android product image import)")
        }
        try {
            if (connection.responseCode !in 200..299) error("Could not download this product photo.")
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_REMOTE_BYTES) { "Product photo is too large." }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions(bounds.outWidth, bounds.outHeight))
                ?: error("The catalog photo could not be opened.")
            saveBitmap(source, 0)
        } finally {
            connection.disconnect()
        }
    }

    /** Copies a bundled, pre-attributed catalog image into private product-photo storage. */
    suspend fun importAsset(assetPath: String): String = withContext(Dispatchers.IO) {
        val bytes = context.assets.open(assetPath).use { it.readBytes() }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions(bounds.outWidth, bounds.outHeight))
            ?: error("Bundled product photo could not be opened")
        saveBitmap(bitmap, 0)
    }

    private fun decodeUri(uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        val options = decodeOptions(bounds.outWidth, bounds.outHeight)
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun decodeOptions(width: Int, height: Int) = BitmapFactory.Options().apply {
        var sample = 1
        while (max(width / sample, height / sample) > 2000) sample *= 2
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.RGB_565
    }

    private fun saveBitmap(source: Bitmap, rotationDegrees: Int): String {
        val oriented = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true).also { source.recycle() }
        } else source
        val maxEdge = 1200
        val scale = minOf(1f, maxEdge.toFloat() / max(oriented.width, oriented.height))
        val resized = if (scale < 1f) Bitmap.createScaledBitmap(
            oriented,
            (oriented.width * scale).roundToInt().coerceAtLeast(1),
            (oriented.height * scale).roundToInt().coerceAtLeast(1),
            true
        ) else oriented
        val filename = "${UUID.randomUUID()}.jpg"
        FileOutputStream(File(imageDir, filename)).use { out ->
            check(resized.compress(Bitmap.CompressFormat.JPEG, 82, out)) { "Could not save the photo" }
        }
        if (resized !== oriented) resized.recycle()
        oriented.recycle()
        return "$DIRECTORY/$filename"
    }

    suspend fun loadBitmap(relativePath: String?): Bitmap? = withContext(Dispatchers.IO) {
        val file = resolve(relativePath) ?: return@withContext null
        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    fun resolve(relativePath: String?): File? {
        if (relativePath.isNullOrBlank() || relativePath.contains("..")) return null
        val file = File(context.filesDir, relativePath)
        return file.takeIf { it.canonicalPath.startsWith(imageDir.canonicalPath + File.separator) }
    }

    suspend fun delete(relativePath: String?) = withContext(Dispatchers.IO) {
        resolve(relativePath)?.delete()
        Unit
    }

    suspend fun deleteIfUnreferenced(relativePath: String?, referencedPaths: Set<String>) = withContext(Dispatchers.IO) {
        if (relativePath != null && relativePath !in referencedPaths) resolve(relativePath)?.delete()
        Unit
    }

    companion object {
        const val DIRECTORY = "product_images"
        private const val MAX_REMOTE_BYTES = 8 * 1024 * 1024
    }
}
