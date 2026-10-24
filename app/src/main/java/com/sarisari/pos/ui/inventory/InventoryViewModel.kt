package com.sarisari.pos.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarisari.pos.AppContainer
import com.sarisari.pos.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel(private val container: AppContainer) : ViewModel() {
    val products: StateFlow<List<ProductEntity>> = container.productRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val lowStock: StateFlow<List<ProductEntity>> = container.productRepository.observeLowStock()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    fun dismissMessage() { _message.value = null }

    fun save(product: ProductEntity) {
        viewModelScope.launch {
            try {
                if (product.name.isBlank()) throw IllegalArgumentException("Product name is required")
                if (product.costPrice < 0 || product.sellingPrice < 0) throw IllegalArgumentException("Prices cannot be negative")
                if (product.stockQuantity < 0) throw IllegalArgumentException("Stock cannot be negative")
                val previousImage = products.value.firstOrNull { it.id == product.id }?.imagePath
                container.productRepository.save(product)
                if (previousImage != null && previousImage != product.imagePath) {
                    container.productImageRepository.deleteIfUnreferenced(previousImage, products.value.filterNot { it.id == product.id }.mapNotNull { it.imagePath }.toSet() + listOfNotNull(product.imagePath))
                }
                _message.value = "${product.name} saved."
            } catch (e: Exception) { _message.value = e.message ?: "Could not save product." }
        }
    }

    fun delete(product: ProductEntity) {
        viewModelScope.launch {
            try {
                container.productRepository.delete(product)
                container.productImageRepository.deleteIfUnreferenced(product.imagePath, products.value.filterNot { it.id == product.id }.mapNotNull { it.imagePath }.toSet())
                _message.value = "${product.name} deleted."
            } catch (e: Exception) { _message.value = e.message ?: "Could not delete product." }
        }
    }

    fun restock(id: Long, qty: Int) {
        viewModelScope.launch {
            try {
                if (qty <= 0) throw IllegalArgumentException("Enter a positive quantity")
                container.productRepository.restock(id, qty)
                _message.value = "Stock updated."
            } catch (e: Exception) { _message.value = e.message ?: "Could not update stock." }
        }
    }

    fun unbox(id: Long, units: Int) {
        viewModelScope.launch {
            try {
                if (units <= 0) throw IllegalArgumentException("Enter a positive number of packs")
                container.productRepository.unbox(id, units)
                _message.value = "Opened $units bulk unit(s). Loose stock updated."
            } catch (e: Exception) { _message.value = e.message ?: "Could not open bulk stock." }
        }
    }
}
