package com.sarisari.pos.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/** Local full-text retrieval index. [rowId] maps to SQLite's required FTS rowid. */
@Fts4
@Entity(tableName = "assistant_chunks")
data class AssistantChunkEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "rowid") val rowId: Long = 0,
    val documentId: Long,
    val title: String,
    val content: String
)
