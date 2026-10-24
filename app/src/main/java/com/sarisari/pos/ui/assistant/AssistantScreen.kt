package com.sarisari.pos.ui.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarisari.pos.AppContainer
import com.sarisari.pos.data.local.entity.AssistantDocumentEntity
import com.sarisari.pos.data.repository.AssistantSource
import com.sarisari.pos.ui.components.ErrorDialog
import com.sarisari.pos.ui.components.ScreenTitle

@Composable
fun AssistantScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val vm: AssistantViewModel = viewModel(key = "assistant", factory = AssistantViewModelFactory(container))
    val messages by vm.messages.collectAsState()
    val busy by vm.busy.collectAsState()
    val documents by vm.documents.collectAsState()
    val generationStatus by vm.generationStatus.collectAsState()
    val error by vm.error.collectAsState()
    val settings by container.settingsRepository.settings.collectAsState()
    var draft by remember { mutableStateOf("") }
    var showKnowledge by remember { mutableStateOf(false) }
    LaunchedEffect(settings.allowCloudAssistant, settings.assistantBackendUrl, settings.geminiApiKeyConfigured) { vm.refreshGenerationStatus() }

    Scaffold(
        modifier = modifier,
        topBar = {
            ScreenTitle(
                "Ate Sari",
                if (settings.allowCloudAssistant && (settings.geminiApiKeyConfigured || settings.assistantBackendUrl.startsWith("https://"))) "Friendly store helper · local RAG + optional AI" else "Friendly store helper · local knowledge · works offline",
                trailing = { TextButton(onClick = { showKnowledge = true }) { Text("Knowledge (${documents.size})") } }
            )
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 8.dp)) {
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val prompts = listOf("What's low stock?", "Today's sales and profit", "How do I record Utang?", "Explain patingi-tingi")
                    items(prompts) { prompt -> AssistChip(onClick = { vm.ask(prompt) }, label = { Text(prompt) }) }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask about store…") },
                        maxLines = 3,
                        shape = RoundedCornerShape(16.dp)
                    )
                    FloatingActionButton(onClick = { vm.ask(draft); draft = "" }, containerColor = MaterialTheme.colorScheme.primary) {
                        Text("➤", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    ) { padding ->
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        LaunchedEffect(messages.size, busy) {
            if (messages.isNotEmpty()) listState.animateScrollToItem((messages.lastIndex + 1 + if (busy) 1 else 0).coerceAtLeast(0))
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(generationStatus, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        when {
                            settings.allowCloudAssistant && settings.geminiApiKeyConfigured -> Text(
                                "Your question and a small set of relevant local facts/notes go to Gemini. On the free API tier, Google may use prompts/replies to improve products—avoid sensitive or confidential details. Local answers still work offline.",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            settings.allowCloudAssistant && settings.assistantBackendUrl.startsWith("https://") -> Text(
                                "Only retrieved context is sent to your private HTTPS endpoint. Local answers remain available if it cannot be reached.",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            else -> Text(
                                "Ate Sari uses local search and live store data. For the optional free-tier AI, add your own Gemini key in Settings; no key is bundled.",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            items(messages, key = { it.id }) { message -> AssistantMessageCard(message) }
            if (busy) item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Checking local store data…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showKnowledge) KnowledgeDialog(
        documents = documents,
        onDismiss = { showKnowledge = false },
        onAdd = vm::addDocument,
        onDelete = vm::deleteDocument
    )
    error?.let { ErrorDialog(it, vm::dismissError, title = "Store assistant") }
}

@Composable
private fun AssistantMessageCard(message: ChatMessage) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start) {
        Card(
            modifier = Modifier.fillMaxWidth(if (message.isUser) 0.88f else 1f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(message.text, color = if (message.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                if (!message.isUser && message.mode != null) Text(message.mode, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                message.sources.forEach { source -> SourceCard(source) }
            }
        }
    }
}

@Composable
private fun SourceCard(source: AssistantSource) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Source · ${source.title}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text(source.excerpt.take(260), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun KnowledgeDialog(
    documents: List<AssistantDocumentEntity>,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var addMode by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (addMode) "Add store knowledge" else "Local knowledge base") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (addMode) {
                    Text("Add a short store procedure, supplier note, or FAQ. It is indexed locally on this phone.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(body, { body = it }, label = { Text("Note") }, minLines = 4, maxLines = 7)
                } else {
                    Text("Answers search these local notes and current Room data. Add only information you want the assistant to use.", style = MaterialTheme.typography.bodySmall)
                    androidx.compose.foundation.lazy.LazyColumn(Modifier.height(300.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(documents, key = { it.id }) { doc ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(doc.title, fontWeight = FontWeight.SemiBold)
                                        Text(doc.category, style = MaterialTheme.typography.labelSmall)
                                    }
                                    if (doc.category != "Store guide" && doc.category != "App guide") TextButton(onClick = { onDelete(doc.id) }) { Text("Remove") }
                                }
                            }
                        }
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = { if (addMode) addMode = false else onDismiss() }) { Text(if (addMode) "Back" else "Close") } },
        confirmButton = {
            if (addMode) Button(onClick = { onAdd(title, body); title = ""; body = ""; addMode = false }, enabled = title.isNotBlank() && body.isNotBlank()) { Text("Add note") }
            else Button(onClick = { addMode = true }) { Text("+ Add note") }
        }
    )
}

class AssistantViewModelFactory(private val container: AppContainer) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = AssistantViewModel(container) as T
}
