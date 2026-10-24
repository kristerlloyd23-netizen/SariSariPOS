package com.sarisari.pos.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarisari.pos.AppContainer
import com.sarisari.pos.data.local.model.PaymentSummary
import com.sarisari.pos.domain.PaymentType
import com.sarisari.pos.ui.components.EmptyContent
import com.sarisari.pos.ui.components.ScreenTitle
import com.sarisari.pos.ui.components.SectionCard
import com.sarisari.pos.ui.theme.GcashBlue
import com.sarisari.pos.ui.theme.PinoyRed
import com.sarisari.pos.ui.theme.ProfitGreen
import com.sarisari.pos.ui.theme.ProfitGreenLight
import com.sarisari.pos.util.toPeso
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val vm: ReportsViewModel = viewModel(key = "reports", factory = ReportsViewModelFactory(container))
    val range by vm.range.collectAsState()
    val report by vm.report.collectAsState()
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item { ScreenTitle("Sales reports", "Revenue, capital and gross profit") }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportRange.entries.forEach { item ->
                    FilterChip(selected = range == item, onClick = { vm.setRange(item) }, label = { Text(item.label) })
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                MetricCard("Sales", report.revenue.toPeso(), "Total revenue", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                MetricCard("Capital", report.cost.toPeso(), "Cost of goods", MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
            }
        }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 14.dp), colors = CardDefaults.cardColors(containerColor = ProfitGreenLight), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Estimated gross profit", style = MaterialTheme.typography.titleSmall, color = ProfitGreen)
                    Text(report.profit.toPeso(), style = MaterialTheme.typography.headlineMedium, color = ProfitGreen, fontWeight = FontWeight.Bold)
                    Text("Revenue minus cost at time of sale", style = MaterialTheme.typography.bodySmall, color = ProfitGreen)
                }
            }
        }
        item {
            SectionCard {
                Text("Payment breakdown", style = MaterialTheme.typography.titleMedium)
                val cash = report.payments.firstOrNull { it.paymentType == PaymentType.CASH.dbValue }?.total ?: 0.0
                val utang = report.payments.firstOrNull { it.paymentType == PaymentType.UTANG.dbValue }?.total ?: 0.0
                val gcash = report.payments.firstOrNull { it.paymentType == PaymentType.GCASH.dbValue }?.total ?: 0.0
                PaymentBreakdownRow("Cash", cash, report.revenue, MaterialTheme.colorScheme.primary)
                PaymentBreakdownRow("Utang charged", utang, report.revenue, PinoyRed)
                PaymentBreakdownRow("GCash / Maya", gcash, report.revenue, GcashBlue)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Utang repayments collected", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(report.utangCollected.toPeso(), color = ProfitGreen, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item { SalesTrend(report.sales.map { it.timestamp to it.totalAmount }, range) }
        item {
            SectionCard {
                Text("Top products", style = MaterialTheme.typography.titleMedium)
                if (report.topProducts.isEmpty()) Text("No sales in this period yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                report.topProducts.forEachIndexed { i, p ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}.", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(p.productName, fontWeight = FontWeight.SemiBold)
                            Text("${p.qty} sold", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(p.revenue.toPeso(), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, subtitle: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PaymentBreakdownRow(label: String, amount: Double, total: Double, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text("${amount.toPeso()}  (${if (total > 0) (amount / total * 100).toInt() else 0}%)", fontWeight = FontWeight.SemiBold)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp))) {
            Box(Modifier.fillMaxWidth(if (total > 0) (amount / total).toFloat().coerceIn(0f, 1f) else 0f).height(6.dp).background(color, RoundedCornerShape(5.dp)))
        }
    }
}

@Composable
private fun SalesTrend(points: List<Pair<Long, Double>>, range: ReportRange) {
    val daily = points.groupBy { timestamp ->
        Calendar.getInstance().apply { timeInMillis = timestamp.first }.let { c ->
            val hourKey = if (range == ReportRange.TODAY) "-${c.get(Calendar.HOUR_OF_DAY)}" else ""
            "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}$hourKey"
        }
    }.map { (_, values) -> values.first().first to values.sumOf { it.second } }.sortedBy { it.first }
    SectionCard {
        Text("Sales trend", style = MaterialTheme.typography.titleMedium)
        if (daily.isEmpty()) {
            Text("No sales recorded in this period.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val max = daily.maxOf { it.second }.coerceAtLeast(1.0)
            daily.takeLast(if (range == ReportRange.TODAY) 10 else 12).forEach { (timestamp, amount) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        SimpleDateFormat(if (range == ReportRange.TODAY) "h:mm a" else "MMM d", Locale.US).format(Date(timestamp)),
                        modifier = Modifier.width(52.dp), style = MaterialTheme.typography.labelSmall
                    )
                    Box(Modifier.weight(1f).height(13.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(7.dp))) {
                        Box(Modifier.fillMaxWidth((amount / max).toFloat().coerceIn(0.04f, 1f)).height(13.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(7.dp)))
                    }
                    Text(amount.toPeso(), modifier = Modifier.width(82.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

class ReportsViewModelFactory(private val container: AppContainer) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = ReportsViewModel(container) as T
}
