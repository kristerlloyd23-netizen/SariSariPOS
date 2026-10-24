package com.sarisari.pos.data.repository

import androidx.room.withTransaction
import com.sarisari.pos.data.local.AppDatabase
import com.sarisari.pos.data.local.entity.CustomerEntity
import com.sarisari.pos.data.local.entity.UtangPaymentEntity
import com.sarisari.pos.data.local.model.TransactionWithItems
import kotlinx.coroutines.flow.Flow
import kotlin.math.round

class CustomerRepository(private val db: AppDatabase) {

    private val dao = db.customerDao()
    private val txDao = db.transactionDao()

    fun observeAll(): Flow<List<CustomerEntity>> = dao.observeAll()
    fun observeById(id: Long): Flow<CustomerEntity?> = dao.observeById(id)
    fun observeTotalReceivables(): Flow<Double> = dao.observeTotalReceivables()
    fun observePayments(customerId: Long): Flow<List<UtangPaymentEntity>> = dao.observePayments(customerId)
    fun observePurchases(customerId: Long): Flow<List<TransactionWithItems>> = txDao.observeForCustomer(customerId)

    suspend fun save(customer: CustomerEntity): Long {
        val name = customer.name.trim()
        if (name.isEmpty()) throw IllegalArgumentException("Name is required")
        if (customer.creditLimit < 0) throw IllegalArgumentException("Credit limit cannot be negative")
        val clean = customer.copy(name = name, phoneNumber = customer.phoneNumber?.trim()?.ifEmpty { null })
        return if (clean.id == 0L) dao.insert(clean) else {
            // Never let an edit overwrite a balance changed by a sale/payment in the meantime.
            val current = dao.getById(clean.id) ?: throw IllegalStateException("Customer not found")
            dao.update(clean.copy(currentBalance = current.currentBalance))
            clean.id
        }
    }

    suspend fun delete(customer: CustomerEntity) {
        db.withTransaction {
            val current = dao.getById(customer.id) ?: return@withTransaction
            if (current.currentBalance > 0.009) {
                throw IllegalStateException("${current.name} still has an unpaid balance")
            }
            dao.deletePaymentsFor(current.id)
            dao.delete(current)
        }
    }

    /** Records a full or partial ("hati-hati") repayment and reduces the balance immediately. */
    suspend fun recordPayment(customerId: Long, amount: Double): CustomerEntity =
        db.withTransaction {
            val customer = dao.getById(customerId) ?: throw IllegalStateException("Customer not found")
            if (amount <= 0) throw IllegalArgumentException("Enter an amount greater than zero")
            if (amount > customer.currentBalance + 0.009) {
                throw IllegalArgumentException("Amount is more than the balance")
            }
            dao.insertPayment(UtangPaymentEntity(customerId = customerId, amountPaid = amount))
            val newBalance = (round((customer.currentBalance - amount) * 100) / 100).coerceAtLeast(0.0)
            val updated = customer.copy(currentBalance = newBalance)
            dao.update(updated)
            updated
        }
}
