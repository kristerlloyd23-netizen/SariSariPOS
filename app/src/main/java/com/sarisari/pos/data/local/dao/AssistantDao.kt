package com.sarisari.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.sarisari.pos.data.local.entity.AssistantChunkEntity
import com.sarisari.pos.data.local.entity.AssistantDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantDao {
    @Query("SELECT * FROM assistant_documents ORDER BY title COLLATE NOCASE")
    fun observeDocuments(): Flow<List<AssistantDocumentEntity>>

    @Query("SELECT COUNT(*) FROM assistant_documents")
    suspend fun documentCount(): Int

    @Query("SELECT * FROM assistant_documents WHERE id = :id")
    suspend fun getDocument(id: Long): AssistantDocumentEntity?

    @Insert
    suspend fun insertDocument(document: AssistantDocumentEntity): Long

    @Insert
    suspend fun insertChunks(chunks: List<AssistantChunkEntity>)

    @Query("DELETE FROM assistant_chunks WHERE documentId = :documentId")
    suspend fun deleteChunks(documentId: Long)

    @Query("DELETE FROM assistant_documents WHERE id = :documentId")
    suspend fun deleteDocument(documentId: Long)

    @Query("DELETE FROM assistant_chunks")
    suspend fun clearChunks()

    @Query("SELECT rowid, * FROM assistant_chunks WHERE assistant_chunks MATCH :query LIMIT :limit")
    suspend fun searchChunks(query: String, limit: Int): List<AssistantChunkEntity>

    @Transaction
    suspend fun replaceIndex(documentId: Long, chunks: List<AssistantChunkEntity>) {
        deleteChunks(documentId)
        if (chunks.isNotEmpty()) insertChunks(chunks)
    }
}
