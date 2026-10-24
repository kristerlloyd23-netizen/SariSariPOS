package com.sarisari.pos.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import com.sarisari.pos.AppContainer
import com.sarisari.pos.data.repository.Wallet
import com.sarisari.pos.data.worker.SyncWorker
import com.sarisari.pos.ui.components.ErrorDialog
import com.sarisari.pos.ui.components.ScreenTitle
import com.sarisari.pos.ui.components.SectionCard
import com.sarisari.pos.util.toDateTime
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by container.settingsRepository.settings.collectAsState()
    var storeName by remember(settings.storeName) { mutableStateOf(settings.storeName) }
    var gcashName by remember(settings.gcashName) { mutableStateOf(settings.gcashName) }
    var gcashNumber by remember(settings.gcashNumber) { mutableStateOf(settings.gcashNumber) }
    var mayaName by remember(settings.mayaName) { mutableStateOf(settings.mayaName) }
    var mayaNumber by remember(settings.mayaNumber) { mutableStateOf(settings.mayaNumber) }
    var assistantEndpoint by remember(settings.assistantBackendUrl) { mutableStateOf(settings.assistantBackendUrl) }
    var geminiApiKey by remember { mutableStateOf("") }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var allowCloudAssistant by remember(settings.allowCloudAssistant) { mutableStateOf(settings.allowCloudAssistant) }
    var toast by remember { mutableStateOf<String?>(null) }
    var showAbout by remember { mutableStateOf(false) }

    val gcashPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val ok = container.settingsRepository.saveQr(Wallet.GCASH, uri)
            toast = if (ok) "GCash QR saved securely on this device." else "Could not save the selected image."
        }
    }
    val mayaPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val ok = container.settingsRepository.saveQr(Wallet.MAYA, uri)
            toast = if (ok) "Maya QR saved securely on this device." else "Could not save the selected image."
        }
    }

    Column(modifier.fillMaxSize()) {
        ScreenTitle("Settings", "Store profile, wallets and local backup")
        androidx.compose.foundation.lazy.LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard {
                    Text("Store profile", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(storeName, { storeName = it }, Modifier.fillMaxWidth(), label = { Text("Store name") }, singleLine = true)
                    OutlinedTextField(gcashName, { gcashName = it }, Modifier.fillMaxWidth(), label = { Text("GCash account name") }, singleLine = true)
                    OutlinedTextField(gcashNumber, { gcashNumber = it }, Modifier.fillMaxWidth(), label = { Text("GCash number") }, singleLine = true)
                    OutlinedTextField(mayaName, { mayaName = it }, Modifier.fillMaxWidth(), label = { Text("Maya account name") }, singleLine = true)
                    OutlinedTextField(mayaNumber, { mayaNumber = it }, Modifier.fillMaxWidth(), label = { Text("Maya number") }, singleLine = true)
                    Button(onClick = {
                        container.settingsRepository.update {
                            it.copy(
                                storeName = storeName.trim().ifEmpty { "Sari-Sari Store" },
                                gcashName = gcashName.trim(), gcashNumber = gcashNumber.trim(),
                                mayaName = mayaName.trim(), mayaNumber = mayaNumber.trim()
                            )
                        }
                        toast = "Store settings saved."
                    }, modifier = Modifier.fillMaxWidth()) { Text("Save store settings") }
                }
            }
            item {
                SectionCard {
                    Text("Ate Sari assistant · optional AI", style = MaterialTheme.typography.titleMedium)
                    Text("Local search, stock lookups and retrieval always run on this phone. If you enable cloud answers, the app sends your question plus a small set of relevant facts/notes. On Gemini's free API tier, Google may use prompts and replies to improve its products—avoid personal, sensitive or confidential store information. Use a protected private backend for sensitive data. Free quotas and model availability can change.", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { runCatching { uriHandler.openUri("https://aistudio.google.com/apikey") } }) { Text("Get your own Gemini API key") }
                    OutlinedTextField(
                        value = geminiApiKey,
                        onValueChange = { geminiApiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (settings.geminiApiKeyConfigured) "Replace Gemini API key" else "Your Gemini API key") },
                        placeholder = { Text("Paste your personal key") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true
                    )
                    Button(onClick = {
                        if (geminiApiKey.isBlank()) toast = "Paste your Gemini API key first."
                        else {
                            container.settingsRepository.saveGeminiApiKey(geminiApiKey)
                            geminiApiKey = ""
                            toast = "Gemini key saved encrypted on this device."
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Save Gemini key on this device") }
                    if (settings.geminiApiKeyConfigured) OutlinedButton(onClick = {
                        container.settingsRepository.removeGeminiApiKey()
                        geminiApiKey = ""
                        toast = "Gemini key removed from this device."
                    }, modifier = Modifier.fillMaxWidth()) { Text("Remove saved Gemini key") }
                    Text("The key is not bundled in the APK and is encrypted at rest with Android Keystore. A key used directly by a client app can still be exposed on a compromised/running device; for a public multi-user release, use a protected backend instead.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = assistantEndpoint,
                        onValueChange = { assistantEndpoint = it.trim() },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Optional private HTTPS backend /ask URL") },
                        placeholder = { Text("https://your-server.example/ask") },
                        singleLine = true
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("Allow cloud answer generation", style = MaterialTheme.typography.titleSmall)
                            Text("Uses Gemini key first, then HTTPS backend; otherwise local only.", style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(checked = allowCloudAssistant, onCheckedChange = { allowCloudAssistant = it })
                    }
                    Button(onClick = {
                        if (allowCloudAssistant && assistantEndpoint.isNotBlank() && !assistantEndpoint.startsWith("https://")) {
                            toast = "The assistant backend must use HTTPS."
                        } else {
                            container.settingsRepository.update { it.copy(assistantBackendUrl = assistantEndpoint, allowCloudAssistant = allowCloudAssistant) }
                            toast = "Assistant privacy settings saved."
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Save assistant settings") }
                }
            }
            item { WalletQrCard(Wallet.GCASH, container, settings.qrVersion, onPick = { gcashPicker.launch("image/*") }, onMessage = { toast = it }) }
            item { WalletQrCard(Wallet.MAYA, container, settings.qrVersion, onPick = { mayaPicker.launch("image/*") }, onMessage = { toast = it }) }
            item {
                SectionCard {
                    Text("Offline backup & sync", style = MaterialTheme.typography.titleMedium)
                    Text("Sales and inventory stay on this device. Sync exports unsynced sales and a current stock/customer snapshot to a local ZIP archive with product images.", style = MaterialTheme.typography.bodyMedium)
                    Text(if (settings.lastSyncAt > 0) "Last export: ${settings.lastSyncAt.toDateTime()}" else "No backup export yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = {
                        SyncWorker.runNow(context)
                        toast = "Backup started. Check the backup file shortly."
                    }, modifier = Modifier.fillMaxWidth()) { Text("Run backup now") }
                    OutlinedButton(onClick = {
                        scope.launch {
                            runCatching { container.backupRepository.syncNow() }
                                .onSuccess { outcome ->
                                    if (outcome.file != null) {
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outcome.file)
                                        val share = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/zip"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(share, "Share Sari-Sari backup archive"))
                                    } else toast = "No backup created."
                                }
                                .onFailure { toast = it.message ?: "Backup failed." }
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Create & share backup file") }
                    Text("Backup files are stored privately on this device under app storage/backups.", style = MaterialTheme.typography.labelSmall)
                }
            }
            item {
                SectionCard {
                    Text("About", style = MaterialTheme.typography.titleMedium)
                    Text("Sari-Sari POS · Offline-first · v1.1", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { showAbout = true }) { Text("How to use this app") }
                }
            }
        }
    }
    toast?.let { ErrorDialog(it, onDismiss = { toast = null }, title = "Sari-Sari POS") }
    if (showAbout) AlertDialog(
        onDismissRequest = { showAbout = false },
        title = { Text("Getting started") },
        text = { Text("Add your store products in Inventory. Use barcode scan to find items quickly. Tap products in POS to build a cart, then check out with Cash, Utang, or GCash/Maya. Add suki to the ledger to track credit and partial repayments. This app works offline; create backup files regularly and keep a copy outside your phone.") },
        confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Got it") } }
    )
}

@Composable
private fun WalletQrCard(
    wallet: Wallet,
    container: AppContainer,
    version: Int,
    onPick: () -> Unit,
    onMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val bitmap by produceState<android.graphics.Bitmap?>(null, wallet, version) {
        value = container.settingsRepository.loadQr(wallet)
    }
    SectionCard {
        Text("${wallet.label} receive QR", style = MaterialTheme.typography.titleMedium)
        Text("Add a screenshot of your static receive QR. It stays only in this app's private storage.", style = MaterialTheme.typography.bodySmall)
        if (bitmap != null) {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Image(bitmap!!.asImageBitmap(), contentDescription = "${wallet.label} receive QR", Modifier.fillMaxWidth().height(220.dp).padding(8.dp))
            }
        } else Text("No QR image saved", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPick, modifier = Modifier.weight(1f)) { Text(if (bitmap == null) "Choose QR image" else "Replace image") }
            if (bitmap != null) OutlinedButton(onClick = {
                scope.launch {
                    container.settingsRepository.removeQr(wallet)
                    onMessage("${wallet.label} QR removed.")
                }
            }) { Text("Remove") }
        }
    }
}
