package com.antonbasson.productcatalog.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class ProductRepository(context: Context) {
    private val dao = ProductDatabase.getInstance(context).productDao()

    fun getCategories(): Flow<List<CategoryEntity>> = dao.getCategories()

    fun getProductsByCategory(categoryId: Long): Flow<List<ProductEntity>> = dao.getProductsByCategory(categoryId)

    suspend fun getOrCreateCategory(name: String): Long = dao.upsertCategoryByName(name)

    suspend fun saveProduct(product: ProductEntity) {
        if (product.id == 0L) {
            dao.insertProduct(product)
        } else {
            dao.updateProduct(product)
        }
    }

    suspend fun deleteProduct(product: ProductEntity) {
        dao.deleteProduct(product)
    }

    suspend fun importProduct(categoryName: String, product: ProductEntity) {
        val categoryId = getOrCreateCategory(categoryName)
        val importedProduct = product.copy(categoryId = categoryId)
        saveProduct(importedProduct)
    }
}
