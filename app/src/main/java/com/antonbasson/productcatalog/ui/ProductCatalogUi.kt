package com.antonbasson.productcatalog.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.antonbasson.productcatalog.data.CategoryEntity
import com.antonbasson.productcatalog.data.ProductEntity
import com.antonbasson.productcatalog.data.ProductRepository
import com.antonbasson.productcatalog.util.CsvImporter
import com.antonbasson.productcatalog.util.PdfExporter
import com.antonbasson.productcatalog.util.PriceUtils
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.io.IOException

@Composable
fun ProductCatalogApp() {
    val context = LocalContext.current
    val repository = remember { ProductRepository(context) }
    val scope = rememberCoroutineScope()

    val categories by repository.getCategories().collectAsState(initial = emptyList())
    var selectedCategoryId by remember { mutableLongStateOf(0L) }
    var selectedCategoryName by remember { mutableStateOf("Products") }
    val selectedCategoryProducts by repository.getProductsByCategory(selectedCategoryId).collectAsState(initial = emptyList())

    var showProductForm by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var bulkResult by remember { mutableStateOf("") }

    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && (selectedCategoryId == 0L || categories.none { it.id == selectedCategoryId })) {
            selectedCategoryId = categories.first().id
            selectedCategoryName = categories.first().name
        }
    }

    val bulkUploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val imported = CsvImporter.importProducts(context, uri, repository, selectedCategoryName)
            bulkResult = if (imported > 0) "Imported $imported products into $selectedCategoryName" else "Bulk upload failed. Check the CSV headers."
        }
    }

    val exportPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val products = selectedCategoryProducts
        val output = PdfExporter.exportCatalogue(context, selectedCategoryName, products)
        if (output != null) {
            bulkResult = "PDF created for $selectedCategoryName"
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    context.contentResolver.openInputStream(output)?.use { input ->
                        input.copyTo(stream)
                    }
                }
            } catch (e: IOException) {
                bulkResult = "PDF created, but saving to document failed."
            }
        } else {
            bulkResult = "PDF export failed."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Product Catalog") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showProductForm = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add product")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (bulkResult.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = bulkResult,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Categories", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { showCategoryDialog = true }) {
                    Text("New category")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { category ->
                            val selected = category.id == selectedCategoryId
                            Card(
                                modifier = Modifier.clickable {
                                    selectedCategoryId = category.id
                                    selectedCategoryName = category.name
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = category.name,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedCategoryName == "Products") "Products" else "$selectedCategoryName (${selectedCategoryProducts.size})",
                    style = MaterialTheme.typography.titleLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { exportPdfLauncher.launch("${selectedCategoryName.replace(" ", "_")}_catalogue.pdf") }) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Text("Export PDF")
                    }
                    OutlinedButton(onClick = { bulkUploadLauncher.launch("text/*") }) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Text("Bulk upload")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedCategoryProducts.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("No products in this category yet.", modifier = Modifier.padding(16.dp))
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(selectedCategoryProducts) { product ->
                        ProductCard(
                            product = product,
                            onEdit = {
                                editingProduct = product
                                showProductForm = true
                            },
                            onDelete = {
                                scope.launch { repository.deleteProduct(product) }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Add category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category name") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            scope.launch {
                                val categoryId = repository.getOrCreateCategory(newCategoryName)
                                selectedCategoryId = categoryId
                                selectedCategoryName = newCategoryName
                                newCategoryName = ""
                            }
                        }
                        showCategoryDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showProductForm) {
        ProductFormDialog(
            categories = categories,
            defaultCategoryId = selectedCategoryId,
            product = editingProduct,
            onDismiss = {
                showProductForm = false
                editingProduct = null
            },
            onSave = { product ->
                scope.launch { repository.saveProduct(product) }
                showProductForm = false
                editingProduct = null
            }
        )
    }
}

@Composable
private fun ProductCard(
    product: ProductEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!product.imagePath.isNullOrBlank()) {
                val bitmap = remember(product.imagePath) { BitmapFactory.decodeFile(product.imagePath) }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = product.description,
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    ProductImagePlaceholder()
                }
            } else {
                ProductImagePlaceholder()
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(product.description, style = MaterialTheme.typography.titleMedium)
                Text("Code: ${product.code}")
                Text("Barcode: ${product.barcode}")
                Text("Selling: ${PriceUtils.formatCurrency(product.sellingPrice)}")
                Text("Cost: ${PriceUtils.formatCurrency(product.costPrice)}")
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onEdit) { Text("Edit") }
                OutlinedButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun ProductImagePlaceholder() {
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Image, contentDescription = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormDialog(
    categories: List<CategoryEntity>,
    defaultCategoryId: Long,
    product: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    val context = LocalContext.current
    var code by remember(product?.id) { mutableStateOf(product?.code ?: "") }
    var description by remember(product?.id) { mutableStateOf(product?.description ?: "") }
    var barcode by remember(product?.id) { mutableStateOf(product?.barcode ?: "") }
    var sellingPrice by remember(product?.id) { mutableDoubleStateOf(product?.sellingPrice ?: 0.0) }
    var costPrice by remember(product?.id) { mutableDoubleStateOf(product?.costPrice ?: 0.0) }
    var imagePath by remember(product?.id) { mutableStateOf(product?.imagePath ?: "") }
    var selectedCategoryId by remember(product?.id) { mutableLongStateOf(product?.categoryId ?: defaultCategoryId) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imagePath = copyUriToInternalStorage(context, uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "Add product" else "Edit product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("Barcode") })
                OutlinedTextField(
                    value = sellingPrice.toString(),
                    onValueChange = { sellingPrice = it.toDoubleOrNull() ?: 0.0 },
                    label = { Text("Selling price") }
                )
                OutlinedTextField(
                    value = costPrice.toString(),
                    onValueChange = { costPrice = it.toDoubleOrNull() ?: 0.0 },
                    label = { Text("Cost price") }
                )

                if (categories.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = categoryMenuExpanded,
                        onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Select category",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                            modifier = Modifier.menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        selectedCategoryId = category.id
                                        categoryMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Button(onClick = { launcher.launch("image/*") }) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Text("Choose image")
                }

                if (imagePath.isNotBlank()) {
                    Text("Image selected")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (description.isNotBlank()) {
                        onSave(
                            ProductEntity(
                                id = product?.id ?: 0L,
                                categoryId = selectedCategoryId,
                                code = code,
                                description = description,
                                barcode = barcode,
                                sellingPrice = sellingPrice,
                                costPrice = costPrice,
                                imagePath = imagePath
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun copyUriToInternalStorage(context: android.content.Context, uri: Uri): String {
    val input = context.contentResolver.openInputStream(uri) ?: return ""
    val file = java.io.File(context.filesDir, "product-images/${System.currentTimeMillis()}.jpg")
    file.parentFile?.mkdirs()
    val output = FileOutputStream(file)
    input.use { inputStream ->
        output.use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }
    return file.absolutePath
}

@Composable
fun ProductCatalogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(),
        content = content
    )
}

