package com.example.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CustomerEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.remote.AuthRepository
import com.example.data.remote.CustomerRepository
import com.example.data.remote.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class CustomerFilter {
    ALL,
    OWE_ME, // Customer Will Pay Me (You Will Get)
    I_OWE   // I Will Pay Customer (You Will Give)
}

class CustomersViewModel(
    private val customerRepository: CustomerRepository,
    private val authRepository: AuthRepository,
    private val transactionRepository: TransactionRepository? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(CustomerFilter.ALL)
    val selectedFilter: StateFlow<CustomerFilter> = _selectedFilter.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val currentUser = authRepository.currentUser.value
    private val userId = currentUser?.id ?: "guest_user"
    private val accessToken = currentUser?.accessToken

    private val _rawCustomers = customerRepository.getCustomersFlow(userId) ?: MutableStateFlow(emptyList())
    private val _rawDeletedCustomers = customerRepository.getDeletedCustomersFlow(userId) ?: MutableStateFlow(emptyList())

    val deletedCustomers: StateFlow<List<CustomerEntity>> = _rawDeletedCustomers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val customers: StateFlow<List<CustomerEntity>> = combine(
        _rawCustomers,
        _searchQuery,
        _selectedFilter
    ) { list, query, filter ->
        var filtered = list.filter {
            !it.isDeleted && (userId == "guest_user" || it.userId == userId)
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filtered = filtered.filter {
                it.name.lowercase().contains(q) ||
                        (it.phone?.lowercase()?.contains(q) == true) ||
                        (it.whatsappNumber?.lowercase()?.contains(q) == true)
            }
        }

        when (filter) {
            CustomerFilter.ALL -> filtered
            CustomerFilter.OWE_ME -> filtered.filter {
                it.currentBalance > 0 || (it.currentBalance == 0.0 && it.balanceType == "YOU_WILL_GET" && it.openingBalance > 0)
            }
            CustomerFilter.I_OWE -> filtered.filter {
                it.currentBalance < 0 || (it.currentBalance == 0.0 && it.balanceType == "YOU_WILL_GIVE" && it.openingBalance > 0)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        syncWithCloud()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: CustomerFilter) {
        _selectedFilter.value = filter
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun syncWithCloud() {
        viewModelScope.launch {
            _isSyncing.value = true
            customerRepository.syncWithSupabase(userId, accessToken)
            _isSyncing.value = false
        }
    }

    fun uploadPhoto(
        context: Context,
        imageUri: Uri,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val uploadedUrl = customerRepository.uploadCustomerPhoto(context, imageUri, accessToken)
            onResult(uploadedUrl)
        }
    }

    fun checkCustomerHasTransactions(
        customerId: String,
        onResult: (hasTransactions: Boolean, count: Int) -> Unit
    ) {
        viewModelScope.launch {
            val txList = transactionRepository?.getCustomerTransactionsFlow(customerId)?.first() ?: emptyList()
            onResult(txList.isNotEmpty(), txList.size)
        }
    }

    fun getCustomerTransactionsFlow(customerId: String): Flow<List<TransactionEntity>> {
        return transactionRepository?.getCustomerTransactionsFlow(customerId) ?: flowOf(emptyList())
    }

    fun addTransaction(
        customerId: String,
        customerName: String,
        type: TransactionType,
        amount: Double,
        notes: String?,
        paymentMethod: String?,
        date: Long,
        attachmentUrl: String?,
        onSuccess: () -> Unit
    ) {
        if (amount <= 0) {
            _userMessage.value = "Please enter a valid amount"
            return
        }
        val tx = TransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            customerId = customerId,
            customerName = customerName,
            type = type,
            amount = amount,
            notes = notes?.trim()?.ifEmpty { null },
            paymentMethod = paymentMethod?.ifEmpty { "Cash" } ?: "Cash",
            attachmentUrl = attachmentUrl,
            date = date
        )
        viewModelScope.launch {
            val res = transactionRepository?.addTransaction(tx, accessToken)
            if (res is com.example.data.remote.AuthResult.Success) {
                _userMessage.value = "Transaction recorded successfully"
                transactionRepository?.syncWithSupabase(userId, accessToken)
                onSuccess()
            } else if (res is com.example.data.remote.AuthResult.Error) {
                _userMessage.value = "Error: ${res.message}"
            }
        }
    }

    fun updateTransaction(
        existingTx: TransactionEntity,
        type: TransactionType,
        amount: Double,
        notes: String?,
        paymentMethod: String?,
        date: Long,
        attachmentUrl: String?,
        onSuccess: () -> Unit
    ) {
        if (amount <= 0) {
            _userMessage.value = "Please enter a valid amount"
            return
        }
        val updatedTx = existingTx.copy(
            type = type,
            amount = amount,
            notes = notes?.trim()?.ifEmpty { null },
            paymentMethod = paymentMethod?.ifEmpty { "Cash" } ?: "Cash",
            attachmentUrl = attachmentUrl,
            date = date,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            val res = transactionRepository?.updateTransaction(updatedTx, accessToken)
            if (res is com.example.data.remote.AuthResult.Success) {
                _userMessage.value = "Transaction updated successfully"
                transactionRepository?.syncWithSupabase(userId, accessToken)
                onSuccess()
            } else if (res is com.example.data.remote.AuthResult.Error) {
                _userMessage.value = "Error: ${res.message}"
            }
        }
    }

    fun deleteTransaction(transactionId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            transactionRepository?.softDeleteTransaction(transactionId, accessToken)
            _userMessage.value = "Transaction deleted"
            onSuccess()
        }
    }

    fun addCustomer(
        name: String,
        phone: String?,
        whatsappNumber: String?,
        address: String?,
        notes: String?,
        photoUrl: String?,
        openingBalance: Double,
        balanceType: String,
        onSuccess: () -> Unit
    ) {
        if (name.isBlank()) {
            _userMessage.value = "Customer name is required"
            return
        }

        val calculatedBalance = if (balanceType == "YOU_WILL_GIVE") -Math.abs(openingBalance) else Math.abs(openingBalance)

        val newCustomer = CustomerEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = name.trim(),
            phone = phone?.trim()?.ifEmpty { null },
            whatsappNumber = whatsappNumber?.trim()?.ifEmpty { null },
            address = address?.trim()?.ifEmpty { null },
            notes = notes?.trim()?.ifEmpty { null },
            photoUrl = photoUrl?.ifEmpty { null },
            openingBalance = Math.abs(openingBalance),
            balanceType = balanceType,
            currentBalance = calculatedBalance,
            lastTransactionDate = System.currentTimeMillis(),
            isDeleted = false,
            isSynced = false
        )

        viewModelScope.launch {
            customerRepository.addCustomer(newCustomer, accessToken)

            // Save Opening Balance: Automatically create the first ledger transaction representing opening balance
            if (Math.abs(openingBalance) > 0.0 && transactionRepository != null) {
                val openingTxType = if (balanceType == "YOU_WILL_GIVE") TransactionType.PAYMENT_RECEIVED else TransactionType.PAYMENT_GIVEN
                val openingTx = TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    customerId = newCustomer.id,
                    customerName = newCustomer.name,
                    type = openingTxType,
                    amount = Math.abs(openingBalance),
                    notes = "Opening Balance",
                    category = "Opening Balance",
                    paymentMethod = "Opening",
                    runningBalance = calculatedBalance,
                    date = System.currentTimeMillis()
                )
                transactionRepository.addTransaction(openingTx, accessToken)
            }

            _userMessage.value = "Customer added successfully"
            onSuccess()
        }
    }

    fun updateCustomer(
        customer: CustomerEntity,
        name: String,
        phone: String?,
        whatsappNumber: String?,
        address: String?,
        notes: String?,
        photoUrl: String?,
        openingBalance: Double,
        balanceType: String,
        onSuccess: () -> Unit
    ) {
        if (name.isBlank()) {
            _userMessage.value = "Customer name is required"
            return
        }

        val updatedBalance = if (balanceType == "YOU_WILL_GIVE") -Math.abs(openingBalance) else Math.abs(openingBalance)

        val updatedEntity = customer.copy(
            name = name.trim(),
            phone = phone?.trim()?.ifEmpty { null },
            whatsappNumber = whatsappNumber?.trim()?.ifEmpty { null },
            address = address?.trim()?.ifEmpty { null },
            notes = notes?.trim()?.ifEmpty { null },
            photoUrl = photoUrl?.ifEmpty { photoUrl },
            openingBalance = Math.abs(openingBalance),
            balanceType = balanceType,
            currentBalance = updatedBalance,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )

        viewModelScope.launch {
            customerRepository.updateCustomer(updatedEntity, accessToken)
            if (transactionRepository != null) {
                transactionRepository.recalculateCustomerBalances(customer.id)
            }
            _userMessage.value = "Customer updated successfully"
            onSuccess()
        }
    }

    fun softDeleteCustomer(customerId: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            customerRepository.softDeleteCustomer(customerId, userId, accessToken)
            _userMessage.value = "Customer moved to trash"
            onSuccess?.invoke()
        }
    }

    fun restoreCustomer(customerId: String) {
        viewModelScope.launch {
            customerRepository.restoreCustomer(customerId, userId, accessToken)
            _userMessage.value = "Customer restored successfully"
        }
    }
}
