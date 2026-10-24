package com.sarisari.pos.ui.utang

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarisari.pos.AppContainer
import com.sarisari.pos.data.local.entity.CustomerEntity
import com.sarisari.pos.data.local.entity.UtangPaymentEntity
import com.sarisari.pos.data.local.model.TransactionWithItems
import com.sarisari.pos.ui.components.ConfirmDialog
import com.sarisari.pos.ui.components.EmptyContent
import com.sarisari.pos.ui.components.ErrorDialog
import com.sarisari.pos.ui.components.NumberEntryDialog
import com.sarisari.pos.ui.components.ScreenTitle
import com.sarisari.pos.ui.components.SectionCard
import com.sarisari.pos.util.toDateTime
import com.sarisari.pos.util.toPeso

@Composable
fun UtangScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val vm: UtangViewModel = viewModel(key = "utang", factory = UtangViewModelFactory(container))
    val customers by vm.customers.collectAsState()
    val total by vm.receivables.collectAsState()
    val message by vm.message.collectAsState()
    var search by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CustomerEntity?>(null) }
    var repayTarget by remember { mutableStateOf<CustomerEntity?>(null) }
    var historyTarget by remember { mutableStateOf<CustomerEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<CustomerEntity?>(null) }
    val visible = customers.filter { it.name.contains(search, true) || it.phoneNumber?.contains(search) == true }

    Column(modifier.fillMaxSize()) {
        ScreenTitle("Suki Ledger", "Store credit · partial repayments accepted", trailing = { Button(onClick = { adding = true }) { Text("+ Add suki") } })
        SectionCard {
            Text("Total outstanding Utang", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(total.toPeso(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text("${customers.count { it.currentBalance > 0 }} customers with a balance", style = MaterialTheme.typography.bodySmall)
        }
        OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), label = { Text("Search suki") }, singleLine = true)
        if (visible.isEmpty()) EmptyContent("No suki found", "Add customers to start tracking store credit.")
        else LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(visible, key = { it.id }) { customer ->
                CustomerCard(customer,
                    onRepay = { repayTarget = customer },
                    onEdit = { editing = customer },
                    onHistory = { historyTarget = customer },
                    onDelete = { deleteTarget = customer })
            }
        }
    }
    if (adding || editing != null) CustomerFormDialog(editing, onDismiss = { adding = false; editing = null }, onSave = { vm.save(it); adding = false; editing = null })
    repayTarget?.let { c ->
        NumberEntryDialog(
            title = "Record repayment",
            subtitle = "${c.name} owes ${c.currentBalance.toPeso()}. Enter the cash paid (full or partial hati-hati).",
            confirmLabel = "Record payment",
            onDismiss = { repayTarget = null },
            onConfirm = { vm.repay(c.id, it.toDoubleOrNull() ?: 0.0); repayTarget = null }
        )
    }
    historyTarget?.let { c ->
        val payments by vm.payments(c.id).collectAsState(initial = emptyList())
        val purchases by vm.purchases(c.id).collectAsState(initial = emptyList())
        CustomerHistoryDialog(c, payments, purchases, onDismiss = { historyTarget = null })
    }
    deleteTarget?.let { c -> ConfirmDialog("Remove suki?", "Remove ${c.name}? A customer with unpaid balance cannot be deleted.", "Remove", true, { deleteTarget = null }) { vm.delete(c); deleteTarget = null } }
    message?.let { ErrorDialog(it, vm::dismissMessage, title = "Suki ledger") }
}

@Composable
private fun CustomerCard(
    customer: CustomerEntity,
    onRepay: () -> Unit,
    onEdit: () -> Unit,
    onHistory: () -> Unit,
    onDelete: () -> Unit
) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(customer.name, style = MaterialTheme.typography.titleMedium)
                    Text(customer.phoneNumber ?: "No phone number", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(customer.currentBalance.toPeso(), style = MaterialTheme.typography.titleLarge, color = if (customer.currentBalance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Credit limit ${customer.creditLimit.toPeso()}", style = MaterialTheme.typography.bodySmall)
                Text("${customer.remainingCredit.toPeso()} available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (customer.currentBalance > 0) Button(onClick = onRepay, modifier = Modifier.weight(1f)) { Text("💵 Repay") }
                OutlinedButton(onClick = onHistory, modifier = Modifier.weight(1f)) { Text("History") }
                OutlinedButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun CustomerFormDialog(initial: CustomerEntity?, onDismiss: () -> Unit, onSave: (CustomerEntity) -> Unit) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var phone by remember(initial?.id) { mutableStateOf(initial?.phoneNumber.orEmpty()) }
    var limit by remember(initial?.id) { mutableStateOf(initial?.creditLimit?.toString() ?: "1000") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add suki" else "Edit suki") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(phone, { phone = it.filter { c -> c.isDigit() || c == '+' || c == ' ' || c == '-' } }, label = { Text("Phone number (optional)") }, singleLine = true)
                OutlinedTextField(limit, { limit = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Credit limit (₱)") }, singleLine = true)
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            Button(onClick = {
                val cap = limit.toDoubleOrNull()
                when {
                    name.isBlank() -> error = "Enter a name."
                    cap == null || cap < 0 -> error = "Enter a valid credit limit."
                    initial != null && cap < initial.currentBalance -> error = "Limit cannot be below the current balance."
                    else -> onSave(CustomerEntity(id = initial?.id ?: 0, name = name.trim(), phoneNumber = phone.trim().ifEmpty { null }, creditLimit = cap, currentBalance = initial?.currentBalance ?: 0.0))
                }
            }) { Text("Save") }
        }
    )
}

@Composable
private fun CustomerHistoryDialog(
    customer: CustomerEntity,
    payments: List<UtangPaymentEntity>,
    purchases: List<TransactionWithItems>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${customer.name}'s ledger") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Current balance ${customer.currentBalance.toPeso()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("Recent purchases", style = MaterialTheme.typography.titleSmall)
                if (purchases.isEmpty()) Text("No purchases on record.", style = MaterialTheme.typography.bodySmall)
                purchases.take(6).forEach { tx ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${tx.transaction.timestamp.toDateTime()} · ${tx.transaction.paymentType}", style = MaterialTheme.typography.bodySmall)
                        Text(tx.transaction.totalAmount.toPeso(), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("Repayments", style = MaterialTheme.typography.titleSmall)
                if (payments.isEmpty()) Text("No repayments recorded.", style = MaterialTheme.typography.bodySmall)
                payments.take(8).forEach { payment ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(payment.timestamp.toDateTime(), style = MaterialTheme.typography.bodySmall)
                        Text("+ ${payment.amountPaid.toPeso()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

class UtangViewModelFactory(private val container: AppContainer) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = UtangViewModel(container) as T
}
