package com.sarisari.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.sarisari.pos.data.local.entity.TransactionEntity
import com.sarisari.pos.data.local.entity.TransactionItemEntity
import com.sarisari.pos.data.local.model.PaymentSummary
import com.sarisari.pos.data.local.model.SalePoint
import com.sarisari.pos.data.local.model.TopProduct
import com.sarisari.pos.data.local.model.TransactionWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert
    suspend fun insertItems(items: List<TransactionItemEntity>)

    // ---- Reporting ----

    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM transactions WHERE timestamp BETWEEN :start AND :end")
    fun observeRevenue(start: Long, end: Long): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(ti.quantity * ti.costAtSale), 0)
        FROM transaction_items ti
        INNER JOIN transactions t ON t.id = ti.transactionId
        WHERE t.timestamp BETWEEN :start AND :end
        """
    )
    fun observeCost(start: Long, end: Long): Flow<Double>

    @Query(
        """
        SELECT paymentType, COALESCE(SUM(totalAmount), 0) AS total, COUNT(*) AS count
        FROM transactions
        WHERE timestamp BETWEEN :start AND :end
        GROUP BY paymentType
        """
    )
    fun observePaymentBreakdown(start: Long, end: Long): Flow<List<PaymentSummary>>

    @Query(
        """
        SELECT ti.productId AS productId, ti.productName AS productName,
               SUM(ti.quantity) AS qty, SUM(ti.quantity * ti.priceAtSale) AS revenue
        FROM transaction_items ti
        INNER JOIN transactions t ON t.id = ti.transactionId
        WHERE t.timestamp BETWEEN :start AND :end
        GROUP BY ti.productId
        ORDER BY qty DESC
        LIMIT :limit
        """
    )
    fun observeTopProducts(start: Long, end: Long, limit: Int = 5): Flow<List<TopProduct>>

    @Query("SELECT timestamp, totalAmount FROM transactions WHERE timestamp BETWEEN :start AND :end")
    fun observeSalePoints(start: Long, end: Long): Flow<List<SalePoint>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(start: Long, end: Long, limit: Int = 30): Flow<List<TransactionWithItems>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun observeForCustomer(customerId: Long): Flow<List<TransactionWithItems>>

    // ---- Sync ----

    @Transaction
    @Query("SELECT * FROM transactions WHERE isSynced = 0 ORDER BY timestamp")
    suspend fun getUnsynced(): List<TransactionWithItems>

    @Query("SELECT COUNT(*) FROM transactions WHERE isSynced = 0")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("UPDATE transactions SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
