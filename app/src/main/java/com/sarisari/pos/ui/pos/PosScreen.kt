package com.sarisari.pos.ui.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarisari.pos.AppContainer
import com.sarisari.pos.data.local.entity.CustomerEntity
import com.sarisari.pos.domain.Categories
import com.sarisari.pos.domain.PaymentType
import com.sarisari.pos.ui.components.BarcodeScannerDialog
import com.sarisari.pos.ui.components.CartBar
import com.sarisari.pos.ui.components.EmptyContent
import com.sarisari.pos.ui.components.ErrorDialog
import com.sarisari.pos.ui.components.FilterChipRow
import com.sarisari.pos.ui.components.ProductCard
import com.sarisari.pos.ui.components.ScreenTitle
import com.sarisari.pos.ui.theme.GcashBlue
import com.sarisari.pos.util.toPeso

@Composable
fun PosScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val vm: PosViewModel = viewModel(key = "pos", factory = PosViewModelFactory(container))
    val products by vm.filteredProducts.collectAsState()
    val allProducts by vm.products.collectAsState()
    val cart by vm.cart.collectAsState()
    val cartLines by vm.cartLines.collectAsState()
    val cartTotal by vm.cartTotal.collectAsState()
    val cartCount by vm.cartCount.collectAsState()
    val category by vm.category.collectAsState()
    val search by vm.search.collectAsState()
    val checkout by vm.checkoutState.collectAsState()
    val isProcessing by vm.isProcessing.collectAsState()
    val customers by vm.customers.collectAsState()
    val scanner by vm.scannerVisible.collectAsState()
    val message by vm.message.collectAsState()
    var cartOpen by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        ScreenTitle(
            "Point of Sale",
            "Tap items to add · ${allProducts.size} products",
            trailing = {
                IconButton(onClick = vm::showScanner) {
                    Icon(com.sarisari.pos.ui.components.AppIcons.BarcodeScan, "Scan barcode", tint = MaterialTheme.colorScheme.primary)
                }
            }
        )
        OutlinedTextField(
            value = search,
            onValueChange = vm::setSearch,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Search products or scan barcode…") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            leadingIcon = { Text("🔎") }
        )
        Spacer(Modifier.height(6.dp))
        FilterChipRow(listOf("All") + Categories.all, category, vm::setCategory)
        if (products.isEmpty()) {
            EmptyContent("No products found", "Try another search or add products in Inventory.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    val parent = product.parentProductId?.let { parentId -> allProducts.firstOrNull { it.id == parentId } }
                    val available = com.sarisari.pos.domain.BreakBulk.available(product, parent)
                    ProductCard(
                        product = product,
                        imageRepository = container.productImageRepository,
                        availableStock = available,
                        quantityInCart = cart[product.id] ?: 0,
                        onAdd = { vm.add(product) }
                    )
                }
            }
        }
        CartBar(cartCount, cartTotal, onClick = { cartOpen = true })
    }

    if (scanner) BarcodeScannerDialog(onBarcode = vm::addByBarcode, onDismiss = vm::hideScanner)
    if (cartOpen) CartDialog(
        lines = cartLines,
        total = cartTotal,
        onDismiss = { cartOpen = false },
        onQty = vm::changeQty,
        onClear = vm::clearCart,
        onCheckout = { cartOpen = false; vm.beginCheckout() }
    )
    when (val state = checkout) {
        CheckoutUiState.Closed -> Unit
        CheckoutUiState.ChoosePayment -> PaymentChoiceDialog(
            onDismiss = vm::closeCheckout,
            onChoose = vm::choosePayment
        )
        is CheckoutUiState.Pay -> {
            if (state.type == PaymentType.UTANG && state.customer == null) {
                CustomerSelectDialog(customers, total = cartTotal, onDismiss = vm::closeCheckout, onSelect = vm::chooseCustomer)
            } else {
                PaymentConfirmDialog(
                    type = state.type,
                    total = cartTotal,
                    customer = state.customer,
                    container = container,
                    isProcessing = isProcessing,
                    onBack = { if (state.type == PaymentType.UTANG) vm.backToPaymentChoices() else vm.backToPaymentChoices() },
                    onConfirm = { vm.completePayment(state.type, state.customer?.id) }
                )
            }
        }
        is CheckoutUiState.Success -> SaleSuccessDialog(state, onDone = vm::closeSuccess)
    }
    if (message != null) ErrorDialog(message!!, vm::dismissMessage, title = "POS")
}

@Composable
private fun CartDialog(
    lines: List<com.sarisari.pos.domain.CartLine>,
    total: Double,
    onDismiss: () -> Unit,
    onQty: (Long, Int) -> Unit,
    onClear: () -> Unit,
    onCheckout: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Current cart (${lines.sumOf { it.quantity }})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (lines.isEmpty()) EmptyContent("Cart is empty", "Tap a product to add it.")
                lines.forEach { line ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(line.product.name, fontWeight = FontWeight.SemiBold)
                            Text("${line.quantity} × ${line.product.sellingPrice.toPeso()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(line.subtotal.toPeso(), fontWeight = FontWeight.Bold)
                        TextButton(onClick = { onQty(line.product.id, -1) }) { Text("−") }
                        TextButton(onClick = { onQty(line.product.id, 1) }) { Text("+") }
                    }
                    HorizontalDivider()
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", style = MaterialTheme.typography.titleMedium)
                    Text(total.toPeso(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        dismissButton = { TextButton(onClick = onClear, enabled = lines.isNotEmpty()) { Text("Clear") } },
        confirmButton = { Button(onClick = onCheckout, enabled = lines.isNotEmpty()) { Text("Checkout") } }
    )
}

@Composable
private fun PaymentChoiceDialog(onDismiss: () -> Unit, onChoose: (PaymentType) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PaymentChoice("💵", "Cash", "Quick cash presets and change", onClick = { onChoose(PaymentType.CASH) })
                PaymentChoice("📒", "Utang", "Charge to a suki's ledger", onClick = { onChoose(PaymentType.UTANG) })
                PaymentChoice("📱", "GCash / Maya", "Show your static wallet QR", onClick = { onChoose(PaymentType.GCASH) })
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PaymentChoice(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Column(Modifier.padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CustomerSelectDialog(
    customers: List<CustomerEntity>,
    total: Double,
    onDismiss: () -> Unit,
    onSelect: (CustomerEntity) -> Unit
) {
    var search by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose suki for Utang") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sale total: ${total.toPeso()}", color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(search, { search = it }, label = { Text("Search customer") }, singleLine = true)
                androidx.compose.foundation.lazy.LazyColumn(Modifier.height(280.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(customers.filter { it.name.contains(search, true) || it.phoneNumber?.contains(search) == true }, key = { it.id }) { customer ->
                        val enough = customer.currentBalance + total <= customer.creditLimit + 0.009
                        Card(onClick = { if (enough) onSelect(customer) }, enabled = enough) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(customer.name, fontWeight = FontWeight.SemiBold)
                                    Text("Balance ${customer.currentBalance.toPeso()} · Limit ${customer.creditLimit.toPeso()}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text(if (enough) "${customer.remainingCredit.toPeso()} left" else "Limit", color = if (enough) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PaymentConfirmDialog(
    type: PaymentType,
    total: Double,
    customer: CustomerEntity?,
    container: AppContainer,
    isProcessing: Boolean,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    var cashReceived by remember(total) { mutableStateOf("") }
    var wallet by remember { mutableStateOf(com.sarisari.pos.data.repository.Wallet.GCASH) }
    val settings by container.settingsRepository.settings.collectAsState()
    val qrBitmap by androidx.compose.runtime.produceState<android.graphics.Bitmap?>(null, wallet, settings.qrVersion) {
        value = container.settingsRepository.loadQr(wallet)
    }
    val parsedCash = cashReceived.toDoubleOrNull() ?: 0.0
    val canConfirm = when (type) {
        PaymentType.CASH -> parsedCash >= total
        PaymentType.UTANG -> customer != null && customer.currentBalance + total <= customer.creditLimit + 0.009
        PaymentType.GCASH -> true
    }

    AlertDialog(
        onDismissRequest = onBack,
        title = { Text(when (type) {
            PaymentType.CASH -> "Cash payment"
            PaymentType.UTANG -> "Confirm Utang"
            PaymentType.GCASH -> "GCash / Maya payment"
        }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Amount due", style = MaterialTheme.typography.titleMedium)
                    Text(total.toPeso(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
                when (type) {
                    PaymentType.CASH -> {
                        OutlinedTextField(
                            value = cashReceived,
                            onValueChange = { cashReceived = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Cash received (₱)") },
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(20, 50, 100, 500, 1000).forEach { amount ->
                                TextButton(onClick = { cashReceived = amount.toString() }) { Text("₱$amount") }
                            }
                        }
                        if (parsedCash >= total) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Change", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text((parsedCash - total).toPeso(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        } else if (cashReceived.isNotBlank()) {
                            Text("Cash received is less than the amount due.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    PaymentType.UTANG -> {
                        Text("Customer: ${customer?.name ?: "Not selected"}", fontWeight = FontWeight.SemiBold)
                        if (customer != null) {
                            Text("Current balance: ${customer.currentBalance.toPeso()}")
                            Text("New balance: ${(customer.currentBalance + total).toPeso()} of ${customer.creditLimit.toPeso()} limit")
                        }
                    }
                    PaymentType.GCASH -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(com.sarisari.pos.data.repository.Wallet.GCASH, com.sarisari.pos.data.repository.Wallet.MAYA).forEach { w ->
                                if (wallet == w) Button(onClick = { wallet = w }) { Text(w.label) }
                                else OutlinedButton(onClick = { wallet = w }) { Text(w.label) }
                            }
                        }
                        if (qrBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = qrBitmap!!.asImageBitmap(),
                                contentDescription = "${wallet.label} receive QR code",
                                modifier = Modifier.fillMaxWidth().height(190.dp)
                            )
                            Text("Ask the customer to scan this QR, then confirm after payment is received.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("No ${wallet.label} QR saved yet. Add one in Settings → Wallet QR.", color = MaterialTheme.colorScheme.error)
                        }
                        val name = if (wallet == com.sarisari.pos.data.repository.Wallet.GCASH) settings.gcashName else settings.mayaName
                        val number = if (wallet == com.sarisari.pos.data.repository.Wallet.GCASH) settings.gcashNumber else settings.mayaNumber
                        if (name.isNotBlank() || number.isNotBlank()) Text("$name  ·  $number", style = MaterialTheme.typography.labelMedium, color = GcashBlue)
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onBack) { Text("Back") } },
        confirmButton = {
            Button(onClick = onConfirm, enabled = canConfirm && !isProcessing) {
                if (isProcessing) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Saving sale…")
                    }
                } else Text(if (type == PaymentType.GCASH) "Payment received" else "Complete sale")
            }
        }
    )
}

@Composable
private fun SaleSuccessDialog(state: CheckoutUiState.Success, onDone: () -> Unit) {
    val result = state.result
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Sale complete! 🎉") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(result.total.toPeso(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text("Paid via ${result.paymentType.label}")
                if (result.customerName != null) Text("Charged to ${result.customerName} · new balance ${result.newCustomerBalance?.toPeso()}")
                result.unboxedMessages.forEach { Text("📦 $it", style = MaterialTheme.typography.bodySmall) }
                Text("Receipt #${result.transactionId}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = onDone) { Text("Done") } }
    )
}

class PosViewModelFactory(private val container: AppContainer) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
        PosViewModel(container) as T
}
