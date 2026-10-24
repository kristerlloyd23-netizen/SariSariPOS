package com.sarisari.pos.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarisari.pos.AppContainer
import com.sarisari.pos.data.local.model.PaymentSummary
import com.sarisari.pos.data.local.model.SalePoint
import com.sarisari.pos.data.local.model.TopProduct
import com.sarisari.pos.domain.PaymentType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

enum class ReportRange(val label: String) { TODAY("Today"), WEEK("7 days"), MONTH("Month") }

data class ReportData(
    val revenue: Double = 0.0,
    val cost: Double = 0.0,
    val payments: List<PaymentSummary> = emptyList(),
    val topProducts: List<TopProduct> = emptyList(),
    val utangCollected: Double = 0.0,
    val sales: List<SalePoint> = emptyList(),
    val start: Long = 0L,
    val end: Long = 0L
) {
    val profit: Double get() = revenue - cost
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(container: AppContainer) : ViewModel() {
    private val repo = container.reportsRepository
    private val _range = MutableStateFlow(ReportRange.TODAY)
    val range: StateFlow<ReportRange> = _range
    fun setRange(value: ReportRange) { _range.value = value }

    val report: StateFlow<ReportData> = _range.flatMapLatest { selected ->
        val (start, end) = bounds(selected)
        val revenue = repo.revenue(start, end)
        val cost = repo.cost(start, end)
        val payments = repo.paymentBreakdown(start, end)
        val top = repo.topProducts(start, end)
        val collected = repo.utangCollected(start, end)
        val sales = repo.salePoints(start, end)
        combine(revenue, cost, payments, top, collected, sales) { values ->
            ReportData(
                revenue = values[0] as Double,
                cost = values[1] as Double,
                payments = values[2] as List<PaymentSummary>,
                topProducts = values[3] as List<TopProduct>,
                utangCollected = values[4] as Double,
                sales = values[5] as List<SalePoint>,
                start = start,
                end = end
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportData())

    private fun bounds(range: ReportRange): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        when (range) {
            ReportRange.TODAY -> Unit
            ReportRange.WEEK -> {
                cal.add(Calendar.DAY_OF_YEAR, -6)
                // Start the 7-day window at midnight, inclusive.
            }
            ReportRange.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
            }
        }
        return cal.timeInMillis to now
    }
}
