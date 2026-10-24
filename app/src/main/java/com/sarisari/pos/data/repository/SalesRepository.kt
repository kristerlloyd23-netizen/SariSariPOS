package com.sarisari.pos.data.repository

import androidx.room.withTransaction
import com.sarisari.pos.data.local.AppDatabase
import com.sarisari.pos.data.local.entity.TransactionEntity
import com.sarisari.pos.data.local.entity.TransactionItemEntity
import com.sarisari.pos.domain.BreakBulk
import com.sarisari.pos.domain.PaymentType
import kotlin.math.round

class CheckoutException(message: String) : Exception(message)

data class CheckoutResult(
    val transactionId: Long,
    val total: Double,
    val paymentType: PaymentType,
    val customerName: String?,
    val newCustomerBalance: Double?,
    val unboxedMessages: List<String>
)

class SalesRepository(private val db: AppDatabase) {

    private val productDao = db.productDao()
    private val customerDao = db.customerDao()
    private val txDao = db.transactionDao()

    /**
     * Atomically: validates stock (auto-opening bulk units for tingi items), validates the
     * customer's credit limit for Utang, deducts stock, updates the balance and saves the sale.
     * Any failure rolls back everything and throws [CheckoutException].
     */
    suspend fun checkout(
        cart: Map<Long, Int>,
        paymentType: PaymentType,
        customerId: Long?
    ): CheckoutResult = db.withTransaction {
        val lines = cart.filterValues { it > 0 }
        if (lines.isEmpty()) throw CheckoutException("Cart is empty")

        val unboxed = mutableListOf<String>()
        val items = mutableListOf<TransactionItemEntity>()
        var total = 0.0

        // Fetch once, then sell bulk/regular items first, followed by tingi items.
        val loaded = lines.map { (id, qty) ->
            (productDao.getById(id) ?: throw CheckoutException("A product in the cart no longer exists")) to qty
        }
        val ordered = loaded.sortedBy { (product, _) -> if (product.isBreakBulk) 1 else 0 }

        for ((product, qty) in ordered) {
            val parent = product.parentProductId?.takeIf { product.isBreakBulk }?.let { productDao.getById(it) }
            val plan = BreakBulk.planDeduction(product, parent, qty)
                ?: throw CheckoutException("Not enough stock for ${product.name}")

            productDao.update(product.copy(stockQuantity = plan.newChildStock))
            if (parent != null && plan.parentUnitsOpened > 0 && plan.newParentStock != null) {
                productDao.update(parent.copy(stockQuantity = plan.newParentStock))
                unboxed += "Opened ${plan.parentUnitsOpened} × ${parent.name}"
            }

            total += product.sellingPrice * qty
            items += TransactionItemEntity(
                transactionId = 0,
                productId = product.id,
                productName = product.name,
                quantity = qty,
                priceAtSale = product.sellingPrice,
                costAtSale = product.costPrice
            )
        }
        total = round(total * 100) / 100

        var customerName: String? = null
        var newBalance: Double? = null
        if (paymentType == PaymentType.UTANG) {
            val id = customerId ?: throw CheckoutException("Choose a customer for Utang")
            val customer = customerDao.getById(id) ?: throw CheckoutException("Customer not found")
            if (customer.currentBalance + total > customer.creditLimit + 0.009) {
                throw CheckoutException(
                    "Over credit limit: ${customer.name} has only " +
                        "₱%.2f credit left".format(customer.remainingCredit)
                )
            }
            newBalance = round((customer.currentBalance + total) * 100) / 100
            customerDao.update(customer.copy(currentBalance = newBalance))
            customerName = customer.name
        }

        val txId = txDao.insertTransaction(
            TransactionEntity(
                totalAmount = total,
                paymentType = paymentType.dbValue,
                customerId = if (paymentType == PaymentType.UTANG) customerId else null
            )
        )
        txDao.insertItems(items.map { it.copy(transactionId = txId) })

        CheckoutResult(txId, total, paymentType, customerName, newBalance, unboxed)
    }

}
