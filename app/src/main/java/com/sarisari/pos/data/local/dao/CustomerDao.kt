package com.sarisari.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sarisari.pos.data.local.entity.CustomerEntity
import com.sarisari.pos.data.local.entity.UtangPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    fun observeById(id: Long): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: Long): CustomerEntity?

    @Query("SELECT COALESCE(SUM(currentBalance), 0) FROM customers")
    fun observeTotalReceivables(): Flow<Double>

    @Insert
    suspend fun insert(customer: CustomerEntity): Long

    @Insert
    suspend fun insertAll(customers: List<CustomerEntity>)

    @Update
    suspend fun update(customer: CustomerEntity)

    @Delete
    suspend fun delete(customer: CustomerEntity)

    // ---- Utang repayments ----

    @Insert
    suspend fun insertPayment(payment: UtangPaymentEntity): Long

    @Query("SELECT * FROM utang_payments WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun observePayments(customerId: Long): Flow<List<UtangPaymentEntity>>

    @Query("SELECT COALESCE(SUM(amountPaid), 0) FROM utang_payments WHERE timestamp BETWEEN :start AND :end")
    fun observeCollectedBetween(start: Long, end: Long): Flow<Double>

    @Query("SELECT * FROM utang_payments WHERE timestamp > :since ORDER BY timestamp")
    suspend fun getPaymentsSince(since: Long): List<UtangPaymentEntity>

    @Query("DELETE FROM utang_payments WHERE customerId = :customerId")
    suspend fun deletePaymentsFor(customerId: Long)
}
