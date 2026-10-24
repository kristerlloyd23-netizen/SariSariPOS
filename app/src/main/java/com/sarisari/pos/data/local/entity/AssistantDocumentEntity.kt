package com.sarisari.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Store-owned FAQ, supplier note or operating instruction used as a RAG source. */
@Entity(tableName = "assistant_documents")
data class AssistantDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "Store guide",
    val updatedAt: Long = System.currentTimeMillis()
)
