package com.sarisari.pos.ui.utang

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarisari.pos.AppContainer
import com.sarisari.pos.util.toPeso
import com.sarisari.pos.data.local.entity.CustomerEntity
import com.sarisari.pos.data.local.entity.UtangPaymentEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UtangViewModel(private val container: AppContainer) : ViewModel() {
    val customers: StateFlow<List<CustomerEntity>> = container.customerRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val receivables: StateFlow<Double> = container.customerRepository.observeTotalReceivables()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    fun dismissMessage() { _message.value = null }
    fun payments(customerId: Long) = container.customerRepository.observePayments(customerId)
    fun purchases(customerId: Long) = container.customerRepository.observePurchases(customerId)

    fun save(customer: CustomerEntity) {
        viewModelScope.launch {
            try {
                container.customerRepository.save(customer)
                _message.value = "${customer.name} saved."
            } catch (e: Exception) { _message.value = e.message ?: "Could not save customer." }
        }
    }
    fun repay(customerId: Long, amount: Double) {
        viewModelScope.launch {
            try {
                val updated = container.customerRepository.recordPayment(customerId, amount)
                _message.value = "Payment received. ${updated.name}'s new balance is ${updated.currentBalance.toPeso()}"
            } catch (e: Exception) { _message.value = e.message ?: "Could not record payment." }
        }
    }
    fun delete(customer: CustomerEntity) {
        viewModelScope.launch {
            try {
                container.customerRepository.delete(customer)
                _message.value = "${customer.name} removed."
            } catch (e: Exception) { _message.value = e.message ?: "Could not delete customer." }
        }
    }
}
