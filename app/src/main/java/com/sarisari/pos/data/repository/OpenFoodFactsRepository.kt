package com.sarisari.pos.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Explicit, barcode-only lookup against the Open Food Facts product catalog. */
data class CatalogProduct(
    val barcode: String,
    val name: String,
    val brands: String,
    val imageUrl: String?,
    val productPage: String
)

class OpenFoodFactsRepository {
    suspend fun lookup(barcode: String): CatalogProduct? = withContext(Dispatchers.IO) {
        val code = barcode.trim()
        require(code.matches(Regex("[0-9A-Za-z._-]{4,64}"))) { "Enter a valid barcode first." }
        val fields = "code,product_name,product_name_en,brands,image_front_small_url,image_front_url,image_url"
        val url = URL("https://world.openfoodfacts.org/api/v2/product/${URLEncoder.encode(code, "UTF-8")}?fields=$fields")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 12_000
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", "SariSariPOS/1.1 (Android product barcode lookup)")
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (connection.responseCode !in 200..299) error("Product catalog is unavailable right now (${connection.responseCode}). Try again later.")
            val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            if (root.optInt("status") != 1) return@withContext null
            val product = root.optJSONObject("product") ?: return@withContext null
            val name = product.optString("product_name_en").takeIf(String::isNotBlank)
                ?: product.optString("product_name").takeIf(String::isNotBlank)
                ?: "Unnamed catalog product"
            val rawImage = listOf("image_front_small_url", "image_front_url", "image_url")
                .firstNotNullOfOrNull { product.optString(it).takeIf(String::isNotBlank) }
            val imageUrl = rawImage?.takeIf(::isAllowedImageUrl)
            CatalogProduct(
                barcode = product.optString("code", code),
                name = name,
                brands = product.optString("brands").takeIf(String::isNotBlank).orEmpty(),
                imageUrl = imageUrl,
                productPage = "https://world.openfoodfacts.org/product/${product.optString("code", code)}"
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun isAllowedImageUrl(value: String): Boolean = runCatching {
        val url = URL(value)
        url.protocol == "https" && (url.host == "openfoodfacts.org" || url.host.endsWith(".openfoodfacts.org"))
    }.getOrDefault(false)
}
