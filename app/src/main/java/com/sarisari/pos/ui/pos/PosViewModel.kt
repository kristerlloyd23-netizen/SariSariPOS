package com.sarisari.pos.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarisari.pos.AppContainer
import com.sarisari.pos.data.local.entity.CustomerEntity
import com.sarisari.pos.data.local.entity.ProductEntity
import com.sarisari.pos.data.repository.CheckoutException
import com.sarisari.pos.data.repository.CheckoutResult
import com.sarisari.pos.domain.BreakBulk
import com.sarisari.pos.domain.CartLine
import com.sarisari.pos.domain.Categories
import com.sarisari.pos.domain.PaymentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CheckoutUiState {
    data object Closed : CheckoutUiState
    data object ChoosePayment : CheckoutUiState
    data class Pay(val type: PaymentType, val customer: CustomerEntity? = null) : CheckoutUiState
    data class Success(val result: CheckoutResult) : CheckoutUiState
}

class PosViewModel(private val container: AppContainer) : ViewModel() {
    val products: StateFlow<List<ProductEntity>> = container.productRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val customers: StateFlow<List<CustomerEntity>> = container.customerRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _cart = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val cart: StateFlow<Map<Long, Int>> = _cart.asStateFlow()

    private val _category = MutableStateFlow("All")
    val category: StateFlow<String> = _category.asStateFlow()
    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    private val _checkoutState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Closed)
    val checkoutState: StateFlow<CheckoutUiState> = _checkoutState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _scannerVisible = MutableStateFlow(false)
    val scannerVisible: StateFlow<Boolean> = _scannerVisible.asStateFlow()

    val filteredProducts: StateFlow<List<ProductEntity>> = combine(products, category, search) { all, cat, query ->
        all.filter { product ->
            (cat == "All" || product.category == cat) &&
                (query.isBlank() || product.name.contains(query.trim(), ignoreCase = true) ||
                    product.barcode?.contains(query.trim(), ignoreCase = true) == true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val cartLines: StateFlow<List<CartLine>> = combine(products, cart) { all, cartMap ->
        val byId = all.associateBy { it.id }
        cartMap.mapNotNull { (id, qty) -> byId[id]?.let { CartLine(it, qty) } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val cartTotal: StateFlow<Double> = cartLines.map { it.sumOf(CartLine::subtotal) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    val cartCount: StateFlow<Int> = cart.map { it.values.sum() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setCategory(value: String) { _category.value = value }
    fun setSearch(value: String) { _search.value = value }
    fun showScanner() { _scannerVisible.value = true }
    fun hideScanner() { _scannerVisible.value = false }
    fun dismissMessage() { _message.value = null }

    fun addByBarcode(code: String) {
        viewModelScope.launch {
            _scannerVisible.value = false
            val product = container.productRepository.getByBarcode(code)
            if (product == null) _message.value = "No product found for barcode $code. Add it from Inventory first."
            else add(product)
        }
    }

    fun add(product: ProductEntity) {
        val current = _cart.value.toMutableMap()
        val nextQty = (current[product.id] ?: 0) + 1
        current[product.id] = nextQty
        val map = products.value.associateBy { it.id }
        if (!BreakBulk.isCartFeasible(current, map)) {
            _message.value = "Not enough stock for ${product.name}."
            return
        }
        _cart.value = current
    }

    fun changeQty(productId: Long, delta: Int) {
        val current = _cart.value.toMutableMap()
        val next = (current[productId] ?: 0) + delta
        if (next <= 0) current.remove(productId) else current[productId] = next
        if (!BreakBulk.isCartFeasible(current, products.value.associateBy { it.id })) {
            _message.value = "Not enough stock available."
        } else _cart.value = current
    }

    fun clearCart() { _cart.value = emptyMap() }
    fun beginCheckout() { if (_cart.value.isNotEmpty()) _checkoutState.value = CheckoutUiState.ChoosePayment }
    fun choosePayment(type: PaymentType) {
        if (type == PaymentType.UTANG) _checkoutState.value = CheckoutUiState.Pay(type)
        else _checkoutState.value = CheckoutUiState.Pay(type)
    }
    fun chooseCustomer(customer: CustomerEntity) {
        _checkoutState.value = CheckoutUiState.Pay(PaymentType.UTANG, customer)
    }
    fun backToPaymentChoices() { _checkoutState.value = CheckoutUiState.ChoosePayment }
    fun closeCheckout() { _checkoutState.value = CheckoutUiState.Closed }

    fun completePayment(type: PaymentType, customerId: Long? = null) {
        if (_isProcessing.value) return
        _isProcessing.value = true
        viewModelScope.launch {
            try {
                val result = container.salesRepository.checkout(_cart.value, type, customerId)
                _cart.value = emptyMap()
                _checkoutState.value = CheckoutUiState.Success(result)
            } catch (e: CheckoutException) {
                _message.value = e.message ?: "Sale could not be completed."
            } catch (e: Exception) {
                _message.value = e.message ?: "Sale could not be completed."
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun closeSuccess() { _checkoutState.value = CheckoutUiState.Closed }
}

