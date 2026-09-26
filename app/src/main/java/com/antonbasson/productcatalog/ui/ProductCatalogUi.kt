package com.antonbasson.productcatalog.util

import android.content.Context
import android.net.Uri
import com.antonbasson.productcatalog.data.ProductEntity
import com.antonbasson.productcatalog.data.ProductRepository
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

object CsvImporter {
    suspend fun importProducts(
        context: Context,
        uri: Uri,
        repository: ProductRepository,
        categoryName: String
    ): Int {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return 0
        val lines = BufferedReader(InputStreamReader(inputStream)).readLines()
        if (lines.isEmpty()) return 0

        val validHeaders = listOf("code", "description", "barcode", "sellingprice", "costprice")
        val header = lines.first().lowercase(Locale.getDefault()).split(",", ";")
        val headerMap = header.mapIndexed { index, value -> value.trim() to index }.toMap()

        if (!validHeaders.all { it in headerMap.keys }) {
            return 0
        }

        var imported = 0
        for (line in lines.drop(1)) {
            if (line.isBlank()) continue
            val values = line.split(",", ";")
            val code = values.getOrNull(headerMap["code"] ?: 0)?.trim().orEmpty()
            val description = values.getOrNull(headerMap["description"] ?: 1)?.trim().orEmpty()
            val barcode = values.getOrNull(headerMap["barcode"] ?: 2)?.trim().orEmpty()
            val sellingPrice = values.getOrNull(headerMap["sellingprice"] ?: 3)?.trim().orEmpty()
            val costPrice = values.getOrNull(headerMap["costprice"] ?: 4)?.trim().orEmpty()

            if (code.isBlank() || description.isBlank()) continue

            repository.importProduct(
                categoryName = categoryName,
                product = ProductEntity(
                    categoryId = 0L,
                    code = code,
                    description = description,
                    barcode = barcode,
                    sellingPrice = sellingPrice.toDoubleOrNull() ?: 0.0,
                    costPrice = costPrice.toDoubleOrNull() ?: 0.0,
                    imagePath = null
                )
            )
            imported += 1
        }

        return imported
    }
}
