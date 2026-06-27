package com.salesinventory.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class BarcodeProductInfo(
    val name: String = "",
    val category: String = "",
    val brand: String = "",
    val imageUrl: String = "",
    val quantity: String = ""
)

suspend fun lookupBarcode(barcode: String): BarcodeProductInfo? {
    if (barcode.isBlank()) return null
    return withContext(Dispatchers.IO) {
        val result = lookupUpcDev(barcode)
        if (result != null) result else lookupOpenFoodFacts(barcode)
    }
}

private suspend fun lookupUpcDev(barcode: String): BarcodeProductInfo? {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("https://upc.dev/v1/product/$barcode")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json = JSONObject(response)
            if (json.optBoolean("ok") && json.has("data")) {
                val data = json.optJSONObject("data")
                val name = data?.optString("name", "").orEmpty().trim()
                if (name.isNotBlank()) {
                    BarcodeProductInfo(
                        name = name,
                        category = data?.optString("category", "").orEmpty().trim(),
                        brand = data?.optString("brand", "").orEmpty().trim(),
                        imageUrl = data?.optString("image_url", "").orEmpty().trim()
                    )
                } else null
            } else null
        } catch (_: Exception) { null }
    }
}

private suspend fun lookupOpenFoodFacts(barcode: String): BarcodeProductInfo? {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("https://world.openfoodfacts.org/api/v0/product/$barcode.json")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json = JSONObject(response)
            if (json.optInt("status") == 1) {
                val product = json.optJSONObject("product") ?: return@withContext null
                BarcodeProductInfo(
                    name = product.optString("product_name", "").trim(),
                    category = product.optString("categories", "").trim().split(",").firstOrNull()?.trim() ?: "",
                    imageUrl = product.optString("image_url", "").trim(),
                    quantity = product.optString("quantity", "").trim()
                )
            } else null
        } catch (_: Exception) { null }
    }
}
