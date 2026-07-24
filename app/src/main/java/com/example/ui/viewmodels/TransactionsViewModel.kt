package com.example.ui.viewmodels

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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

enum class TransactionFilter {
    ALL,
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    PAYMENT_RECEIVED,
    PAYMENT_GIVEN,
    INCOME,
    EXPENSE
}

class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    private val customerRepository: CustomerRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TransactionFilter.ALL)
    val selectedFilter: StateFlow<TransactionFilter> = _selectedFilter.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val currentUser = authRepository.currentUser.value
    private val userId = currentUser?.id ?: "guest_user"
    private val accessToken = currentUser?.accessToken

    private val _rawTransactions = transactionRepository.getTransactionsFlow(userId) ?: MutableStateFlow(emptyList())
    private val _rawDeletedTransactions = transactionRepository.getDeletedTransactionsFlow(userId) ?: MutableStateFlow(emptyList())
    private val _rawCustomers = customerRepository.getCustomersFlow(userId) ?: MutableStateFlow(emptyList())

    val customers: StateFlow<List<CustomerEntity>> = _rawCustomers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val deletedTransactions: StateFlow<List<TransactionEntity>> = _rawDeletedTransactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val transactions: StateFlow<List<TransactionEntity>> = combine(
        _rawTransactions,
        _searchQuery,
        _selectedFilter
    ) { list, query, filter ->
        var filtered = list.filter {
            !it.isDeleted && (userId == "guest_user" || it.userId == userId)
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filtered = filtered.filter {
                it.customerName.lowercase().contains(q) ||
                        (it.notes?.lowercase()?.contains(q) == true) ||
                        it.category.lowercase().contains(q) ||
                        it.amount.toString().contains(q)
            }
        }

        val cal = Calendar.getInstance()
        when (filter) {
            TransactionFilter.ALL -> filtered
            TransactionFilter.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOfDay = cal.timeInMillis
                filtered.filter { it.date >= startOfDay }
            }
            TransactionFilter.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOfWeek = cal.timeInMillis
                filtered.filter { it.date >= startOfWeek }
            }
            TransactionFilter.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOfMonth = cal.timeInMillis
                filtered.filter { it.date >= startOfMonth }
            }
            TransactionFilter.PAYMENT_RECEIVED -> filtered.filter { it.type == TransactionType.PAYMENT_RECEIVED }
            TransactionFilter.PAYMENT_GIVEN -> filtered.filter { it.type == TransactionType.PAYMENT_GIVEN }
            TransactionFilter.INCOME -> filtered.filter { it.type == TransactionType.INCOME }
            TransactionFilter.EXPENSE -> filtered.filter { it.type == TransactionType.EXPENSE }
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

    fun setFilter(filter: TransactionFilter) {
        _selectedFilter.value = filter
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun syncWithCloud() {
        viewModelScope.launch {
            _isSyncing.value = true
            transactionRepository.syncWithSupabase(userId, accessToken)
            _isSyncing.value = false
        }
    }

    fun addTransaction(
        customerId: String,
        customerName: String,
        type: TransactionType,
        amount: Double,
        date: Long,
        notes: String?,
        category: String,
        paymentMethod: String,
        attachmentUrl: String?,
        onSuccess: () -> Unit
    ) {
        if (customerId.isBlank()) {
            _userMessage.value = "Please select a customer"
            return
        }
        if (amount <= 0) {
            _userMessage.value = "Amount must be greater than zero"
            return
        }

        val newTransaction = TransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            customerId = customerId,
            customerName = customerName,
            type = type,
            amount = amount,
            date = date,
            notes = notes?.trim()?.ifEmpty { null },
            category = category.ifEmpty { "General" },
            paymentMethod = paymentMethod.ifEmpty { "Cash" },
            attachmentUrl = attachmentUrl?.ifEmpty { null },
            isDeleted = false,
            isSynced = false
        )

        viewModelScope.launch {
            val res = transactionRepository.addTransaction(newTransaction, accessToken)
            if (res is com.example.data.remote.AuthResult.Success) {
                _userMessage.value = "Transaction recorded successfully"
                syncWithCloud()
                onSuccess()
            } else if (res is com.example.data.remote.AuthResult.Error) {
                _userMessage.value = "Error: ${res.message}"
            }
        }
    }

    fun updateTransaction(
        transaction: TransactionEntity,
        type: TransactionType,
        amount: Double,
        date: Long,
        notes: String?,
        category: String,
        paymentMethod: String,
        attachmentUrl: String?,
        onSuccess: () -> Unit
    ) {
        if (amount <= 0) {
            _userMessage.value = "Amount must be greater than zero"
            return
        }

        val updated = transaction.copy(
            type = type,
            amount = amount,
            date = date,
            notes = notes?.trim()?.ifEmpty { null },
            category = category,
            paymentMethod = paymentMethod,
            attachmentUrl = attachmentUrl?.ifEmpty { null },
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )

        viewModelScope.launch {
            val res = transactionRepository.updateTransaction(updated, accessToken)
            if (res is com.example.data.remote.AuthResult.Success) {
                _userMessage.value = "Transaction updated successfully"
                syncWithCloud()
                onSuccess()
            } else if (res is com.example.data.remote.AuthResult.Error) {
                _userMessage.value = "Error: ${res.message}"
            }
        }
    }

    fun softDeleteTransaction(id: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            transactionRepository.softDeleteTransaction(id, accessToken)
            _userMessage.value = "Transaction moved to trash"
            onSuccess?.invoke()
        }
    }

    fun restoreTransaction(id: String) {
        viewModelScope.launch {
            transactionRepository.restoreTransaction(id, accessToken)
            _userMessage.value = "Transaction restored"
        }
    }

    fun duplicateTransaction(transaction: TransactionEntity, onSuccess: () -> Unit) {
        val dup = transaction.copy(
            id = UUID.randomUUID().toString(),
            date = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        viewModelScope.launch {
            transactionRepository.addTransaction(dup, accessToken)
            _userMessage.value = "Transaction duplicated"
            onSuccess()
        }
    }
}
