package com.sarisari.pos.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarisari.pos.AppContainer
import com.sarisari.pos.data.local.entity.AssistantDocumentEntity
import com.sarisari.pos.data.repository.AssistantReply
import com.sarisari.pos.data.repository.AssistantSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val sources: List<AssistantSource> = emptyList(),
    val mode: String? = null
)

class AssistantViewModel(private val container: AppContainer) : ViewModel() {
    private val _messages = MutableStateFlow(
        listOf(ChatMessage(isUser = false, text = "Kumusta, boss! Ako si Ate Sari—your friendly, practical store helper. I can check local product stock, sales summaries, Utang totals, and your store notes. I’ll say when I can’t verify something, and I can’t change stock or complete transactions for you."))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _generationStatus = MutableStateFlow("Checking assistant configuration…")
    val generationStatus: StateFlow<String> = _generationStatus.asStateFlow()

    val documents: StateFlow<List<AssistantDocumentEntity>> = container.assistantRepository.observeDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { refreshGenerationStatus() }

    fun refreshGenerationStatus() {
        _generationStatus.value = container.assistantRepository.generationStatus()
    }

    fun ask(question: String) {
        val clean = question.trim()
        if (clean.isEmpty() || _busy.value) return
        _messages.value = _messages.value + ChatMessage(isUser = true, text = clean)
        _busy.value = true
        viewModelScope.launch {
            try {
                val reply: AssistantReply = container.assistantRepository.ask(clean)
                _messages.value = _messages.value + ChatMessage(
                    isUser = false,
                    text = reply.answer,
                    sources = reply.sources,
                    mode = reply.mode
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Couldn't answer that question."
            } finally {
                _busy.value = false
            }
        }
    }

    fun addDocument(title: String, body: String) {
        viewModelScope.launch {
            try {
                container.assistantRepository.saveDocument(title, body)
                _error.value = "Note added to local knowledge."
            } catch (e: Exception) { _error.value = e.message ?: "Could not save note." }
        }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch {
            try {
                container.assistantRepository.deleteDocument(id)
                _error.value = "Knowledge note removed."
            } catch (e: Exception) { _error.value = e.message ?: "Could not remove note." }
        }
    }

    fun dismissError() { _error.value = null }
}
