package com.sarisari.pos.data.repository

import com.sarisari.pos.data.local.AppDatabase
import com.sarisari.pos.data.local.model.PaymentSummary
import com.sarisari.pos.data.local.model.SalePoint
import com.sarisari.pos.data.local.model.TopProduct
import com.sarisari.pos.data.local.model.TransactionWithItems
import kotlinx.coroutines.flow.Flow

class ReportsRepository(db: AppDatabase) {
    private val txDao = db.transactionDao()
    private val customerDao = db.customerDao()

    fun revenue(start: Long, end: Long): Flow<Double> = txDao.observeRevenue(start, end)
    fun cost(start: Long, end: Long): Flow<Double> = txDao.observeCost(start, end)
    fun paymentBreakdown(start: Long, end: Long): Flow<List<PaymentSummary>> = txDao.observePaymentBreakdown(start, end)
    fun topProducts(start: Long, end: Long): Flow<List<TopProduct>> = txDao.observeTopProducts(start, end, 5)
    fun salePoints(start: Long, end: Long): Flow<List<SalePoint>> = txDao.observeSalePoints(start, end)
    fun recent(start: Long, end: Long): Flow<List<TransactionWithItems>> = txDao.observeRecent(start, end, 30)
    fun utangCollected(start: Long, end: Long): Flow<Double> = customerDao.observeCollectedBetween(start, end)
}
