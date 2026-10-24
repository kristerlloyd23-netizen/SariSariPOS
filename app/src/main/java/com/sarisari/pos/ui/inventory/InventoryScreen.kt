package com.sarisari.pos.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarisari.pos.AppContainer
import com.sarisari.pos.data.local.entity.ProductEntity
import com.sarisari.pos.data.repository.CatalogProduct
import com.sarisari.pos.domain.Categories
import com.sarisari.pos.ui.components.ConfirmDialog
import com.sarisari.pos.ui.components.EmptyContent
import com.sarisari.pos.ui.components.ErrorDialog
import com.sarisari.pos.ui.components.NumberEntryDialog
import com.sarisari.pos.ui.components.ScreenTitle
import com.sarisari.pos.ui.components.ProductPhoto
import com.sarisari.pos.ui.components.ProductCameraDialog
import com.sarisari.pos.ui.theme.WarningAmber
import com.sarisari.pos.util.toPeso

@Composable
fun InventoryScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val vm: InventoryViewModel = viewModel(key = "inventory", factory = InventoryViewModelFactory(container))
    val products by vm.products.collectAsState()
    val low by vm.lowStock.collectAsState()
    val message by vm.message.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<ProductEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ProductEntity?>(null) }
    var stockTarget by remember { mutableStateOf<ProductEntity?>(null) }
    var unboxTarget by remember { mutableStateOf<ProductEntity?>(null) }
    var showLowOnly by remember { mutableStateOf(false) }

    val visible = products.filter { p ->
        (selectedCategory == "All" || p.category == selectedCategory) &&
            p.name.contains(query, true) && (!showLowOnly || p.isLowStock)
    }
    Column(modifier.fillMaxSize()) {
        ScreenTitle(
            "Inventory",
            "${products.size} products · ${low.size} low stock",
            trailing = { Button(onClick = { adding = true }) { Text("+ Add") } }
        )
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), label = { Text("Search inventory") }, singleLine = true)
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = showLowOnly, onClick = { showLowOnly = !showLowOnly }, label = { Text("⚠ Low stock (${low.size})") })
        }
        androidx.compose.foundation.lazy.LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            items((listOf("All") + Categories.all).size) { index ->
                val value = (listOf("All") + Categories.all)[index]
                FilterChip(selected = value == selectedCategory, onClick = { selectedCategory = value }, label = { Text(value) })
            }
        }
        if (visible.isEmpty()) EmptyContent("No inventory items", "Use Add to create your first product.")
        else LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(visible, key = { it.id }) { product ->
                InventoryProductCard(
                    product = product,
                    parentName = product.parentProductId?.let { id -> products.firstOrNull { it.id == id }?.name },
                    imageRepository = container.productImageRepository,
                    onEdit = { editing = product },
                    onDelete = { deleteTarget = product },
                    onRestock = { stockTarget = product },
                    onUnbox = { unboxTarget = product }
                )
            }
        }
    }
    if (adding || editing != null) ProductFormDialog(
        initial = editing,
        parents = products.filterNot { it.isBreakBulk && it.id != editing?.id },
        container = container,
        onDismiss = { adding = false; editing = null },
        onSave = { vm.save(it); adding = false; editing = null }
    )
    deleteTarget?.let { p -> ConfirmDialog("Delete product?", "Delete ${p.name}? This cannot be undone.", "Delete", true, { deleteTarget = null }) { vm.delete(p); deleteTarget = null } }
    stockTarget?.let { p -> NumberEntryDialog("Restock ${p.name}", "Enter units to add to current stock.", confirmLabel = "Add stock", onDismiss = { stockTarget = null }, onConfirm = { vm.restock(p.id, it.toIntOrNull() ?: 0); stockTarget = null }) }
    unboxTarget?.let { p -> NumberEntryDialog("Open ${p.name}", "How many parent packs/boxes do you want to open? ${p.conversionRate} loose units per pack.", initial = "1", confirmLabel = "Open stock", onDismiss = { unboxTarget = null }, onConfirm = { vm.unbox(p.id, it.toIntOrNull() ?: 0); unboxTarget = null }) }
    message?.let { ErrorDialog(it, vm::dismissMessage, title = "Inventory") }
}

@Composable
private fun InventoryProductCard(
    product: ProductEntity,
    parentName: String?,
    imageRepository: com.sarisari.pos.data.repository.ProductImageRepository,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRestock: () -> Unit,
    onUnbox: () -> Unit
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProductPhoto(product.imagePath, imageRepository, Categories.emoji(product.category), 56.dp)
                Column(Modifier.weight(1f)) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium)
                    Text("${product.category} · Cost ${product.costPrice.toPeso()} · SRP ${product.sellingPrice.toPeso()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (product.isLowStock) Text("LOW", color = WarningAmber, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Stock: ${product.stockQuantity}${if (product.isBreakBulk) " pcs" else " units"}", fontWeight = FontWeight.SemiBold)
                    if (product.isBreakBulk && parentName != null) Text("From $parentName · ${product.conversionRate} per unit", style = MaterialTheme.typography.labelSmall)
                    if (product.barcode != null) Text("Barcode: ${product.barcode}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    IconButton(onClick = onRestock) { Text("📥") }
                    IconButton(onClick = onEdit) { Text("✏️") }
                    IconButton(onClick = onDelete) { Text("🗑️") }
                }
            }
            if (product.imageSource != null) {
                TextButton(onClick = {
                    runCatching { uriHandler.openUri(product.imageSource) }
                }) { Text("Photo credit: Open Food Facts · CC BY-SA 3.0") }
            }
            if (product.isBreakBulk) OutlinedButton(onClick = onUnbox, modifier = Modifier.fillMaxWidth()) { Text("📦 Open parent stock") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductFormDialog(
    initial: ProductEntity?,
    parents: List<ProductEntity>,
    container: AppContainer,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var category by remember(initial?.id) { mutableStateOf(initial?.category ?: Categories.SNACKS) }
    var barcode by remember(initial?.id) { mutableStateOf(initial?.barcode.orEmpty()) }
    var cost by remember(initial?.id) { mutableStateOf(initial?.costPrice?.toString().orEmpty()) }
    var price by remember(initial?.id) { mutableStateOf(initial?.sellingPrice?.toString().orEmpty()) }
    var stock by remember(initial?.id) { mutableStateOf(initial?.stockQuantity?.toString() ?: "0") }
    var threshold by remember(initial?.id) { mutableStateOf(initial?.lowStockThreshold?.toString() ?: "5") }
    var breakBulk by remember(initial?.id) { mutableStateOf(initial?.isBreakBulk ?: false) }
    var parentId by remember(initial?.id) { mutableStateOf(initial?.parentProductId) }
    var conversion by remember(initial?.id) { mutableStateOf(initial?.conversionRate?.toString() ?: "12") }
    var imagePath by remember(initial?.id) { mutableStateOf(initial?.imagePath) }
    var imageSource by remember(initial?.id) { mutableStateOf(initial?.imageSource) }
    var catalogProduct by remember(initial?.id) { mutableStateOf<CatalogProduct?>(null) }
    var lookingUp by remember { mutableStateOf(false) }
    var importingCatalogImage by remember { mutableStateOf(false) }
    var cameraOpen by remember { mutableStateOf(false) }
    var barcodeScannerOpen by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val photoPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching { container.productImageRepository.importUri(uri) }
                .onSuccess { imagePath = it; imageSource = null; imageError = null }
                .onFailure { imageError = it.message ?: "Could not import photo" }
        }
    }
    var categoryExpanded by remember { mutableStateOf(false) }
    var parentExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add product" else "Edit product") },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                item { OutlinedTextField(name, { name = it }, label = { Text("Product name") }, singleLine = true) }
                item {
                    Column {
                        OutlinedButton(onClick = { categoryExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Category: $category") }
                        androidx.compose.material3.DropdownMenu(categoryExpanded, { categoryExpanded = false }) {
                            Categories.all.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { category = c; categoryExpanded = false }) }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        barcode,
                        { barcode = it },
                        label = { Text("Barcode (optional)") },
                        singleLine = true,
                        trailingIcon = { TextButton(onClick = { barcodeScannerOpen = true }) { Text("Scan") } }
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProductPhoto(imagePath, container.productImageRepository, "📦", 72.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (imagePath == null) "Product photo (optional)" else "Photo added")
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { cameraOpen = true }) { Text("Take photo") }
                                TextButton(onClick = {
                                    photoPicker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }) { Text("Choose") }
                                if (imagePath != null) TextButton(onClick = { imagePath = null; imageSource = null }) { Text("Remove") }
                            }
                        }
                    }
                    if (imageSource != null) {
                        TextButton(onClick = { runCatching { uriHandler.openUri(imageSource!!) } }) {
                            Text("Photo credit: Open Food Facts · CC BY-SA 3.0")
                        }
                    }
                    if (imageError != null) Text(imageError!!, color = MaterialTheme.colorScheme.error)
                }
                item {
                    OutlinedButton(
                        onClick = {
                            if (barcode.isBlank()) imageError = "Scan or enter a barcode before searching the catalog."
                            else scope.launch {
                                lookingUp = true
                                imageError = null
                                catalogProduct = null
                                runCatching { container.productCatalogRepository.lookup(barcode) }
                                    .onSuccess { result ->
                                        catalogProduct = result
                                        if (result == null) imageError = "No product found for this barcode. You can still take or choose a photo."
                                    }
                                    .onFailure { imageError = it.message ?: "Catalog lookup failed." }
                                lookingUp = false
                            }
                        },
                        enabled = !lookingUp,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (lookingUp) "Searching Open Food Facts…" else "Find product photo by barcode") }
                    catalogProduct?.let { result ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Catalog match — check it is the same package", fontWeight = FontWeight.SemiBold)
                                Text(listOf(result.brands, result.name).filter(String::isNotBlank).joinToString(" · "))
                                if (result.imageUrl != null) {
                                    Text("Photo source: Open Food Facts · CC BY-SA 3.0. Attribution will stay with the saved photo.", style = MaterialTheme.typography.bodySmall)
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                importingCatalogImage = true
                                                runCatching { container.productImageRepository.importRemoteImage(result.imageUrl) }
                                                    .onSuccess { path ->
                                                        imagePath = path
                                                        imageSource = result.productPage
                                                        if (name.isBlank()) name = listOf(result.brands, result.name).filter(String::isNotBlank).joinToString(" ")
                                                        catalogProduct = null
                                                        imageError = null
                                                    }
                                                    .onFailure { imageError = it.message ?: "Could not import catalog photo." }
                                                importingCatalogImage = false
                                            }
                                        },
                                        enabled = !importingCatalogImage,
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(if (importingCatalogImage) "Downloading photo…" else "Use this product photo") }
                                } else {
                                    Text("This catalog entry has no package photo. You can still use the product name or take your own photo.", style = MaterialTheme.typography.bodySmall)
                                    TextButton(onClick = { if (name.isBlank()) name = listOf(result.brands, result.name).filter(String::isNotBlank).joinToString(" "); catalogProduct = null }) { Text("Use catalog name") }
                                }
                            }
                        }
                    }
                }
                item { OutlinedTextField(cost, { cost = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Cost / capital (₱)") }, singleLine = true) }
                item { OutlinedTextField(price, { price = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Selling price / SRP (₱)") }, singleLine = true) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(stock, { stock = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("Stock") }, singleLine = true)
                        OutlinedTextField(threshold, { threshold = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("Low stock alert") }, singleLine = true)
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) { Text("Break-bulk / tingi item", fontWeight = FontWeight.SemiBold); Text("Opened from a pack or box", style = MaterialTheme.typography.bodySmall) }
                        Switch(checked = breakBulk, onCheckedChange = { breakBulk = it })
                    }
                }
                if (breakBulk) {
                    item {
                        Column {
                            OutlinedButton(onClick = { parentExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Parent: ${parents.firstOrNull { it.id == parentId }?.name ?: "Choose bulk item"}") }
                            DropdownMenu(parentExpanded, { parentExpanded = false }) {
                                parents.filter { it.id != initial?.id && !it.isBreakBulk }.forEach { parent ->
                                    DropdownMenuItem(text = { Text(parent.name) }, onClick = { parentId = parent.id; parentExpanded = false })
                                }
                            }
                        }
                    }
                    item { OutlinedTextField(conversion, { conversion = it.filter(Char::isDigit) }, label = { Text("Loose units per pack / box") }, singleLine = true) }
                }
                if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            Button(onClick = {
                val costValue = cost.toDoubleOrNull()
                val priceValue = price.toDoubleOrNull()
                val stockValue = stock.toIntOrNull()
                if (name.isBlank()) error = "Enter a product name."
                else if (costValue == null || priceValue == null) error = "Enter valid cost and selling prices."
                else if (stockValue == null) error = "Enter a valid stock quantity."
                else if (breakBulk && parentId == null) error = "Choose the bulk parent item."
                else if (breakBulk && (conversion.toIntOrNull() ?: 0) < 2) error = "Units per parent must be at least 2."
                else onSave(ProductEntity(
                    id = initial?.id ?: 0L,
                    name = name.trim(), category = category,
                    barcode = barcode.trim().ifEmpty { null },
                    costPrice = costValue, sellingPrice = priceValue,
                    stockQuantity = stockValue, lowStockThreshold = threshold.toIntOrNull() ?: 5,
                    isBreakBulk = breakBulk, parentProductId = if (breakBulk) parentId else null,
                    conversionRate = if (breakBulk) conversion.toIntOrNull() ?: 12 else 1,
                    imagePath = imagePath,
                    imageSource = imageSource
                ))
            }) { Text("Save") }
        }
    )
    if (barcodeScannerOpen) com.sarisari.pos.ui.components.BarcodeScannerDialog(
        onBarcode = { barcode = it; barcodeScannerOpen = false },
        onDismiss = { barcodeScannerOpen = false }
    )
    if (cameraOpen) ProductCameraDialog(
        onPhoto = { file ->
            cameraOpen = false
            scope.launch {
                runCatching { container.productImageRepository.importFile(file) }
                    .onSuccess { imagePath = it; imageSource = null; imageError = null }
                    .onFailure { imageError = it.message ?: "Could not save photo" }
                file.delete()
            }
        },
        onDismiss = { cameraOpen = false }
    )
}

class InventoryViewModelFactory(private val container: AppContainer) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = InventoryViewModel(container) as T
}
