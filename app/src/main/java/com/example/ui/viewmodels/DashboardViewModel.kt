package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CustomerEntity
import com.example.data.local.TransactionEntity
import com.example.data.remote.AuthRepository
import com.example.data.remote.CustomerRepository
import com.example.data.remote.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

data class DashboardUiState(
    val isLoading: Boolean = false,
    val netBalance: Double = 0.0,
    val moneyToReceive: Double = 0.0,
    val moneyToPay: Double = 0.0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val totalCustomers: Int = 0,
    val totalTransactions: Int = 0
)

class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val customerRepository: CustomerRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val currentUser = authRepository.currentUser.value
    private val userId = currentUser?.id ?: "guest_user"
    private val accessToken = currentUser?.accessToken

    private val _rawCustomers = customerRepository.getCustomersFlow(userId) ?: flowOf(emptyList())
    private val _rawTransactions = transactionRepository.getTransactionsFlow(userId) ?: flowOf(emptyList())

    val uiState: StateFlow<DashboardUiState> = combine(
        _rawCustomers,
        _rawTransactions
    ) { customers, transactions ->
        val activeCustomers = customers.filter { !it.isDeleted }
        val activeTransactions = transactions.filter { !it.isDeleted }

        var receive = 0.0
        var pay = 0.0

        activeCustomers.forEach { c ->
            if (c.currentBalance > 0) {
                receive += c.currentBalance
            } else if (c.currentBalance < 0) {
                pay += abs(c.currentBalance)
            }
        }

        val net = receive - pay

        val recent = activeTransactions
            .sortedByDescending { it.date }
            .take(5)

        DashboardUiState(
            isLoading = false,
            netBalance = net,
            moneyToReceive = receive,
            moneyToPay = pay,
            recentTransactions = recent,
            totalCustomers = activeCustomers.size,
            totalTransactions = activeTransactions.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    init {
        syncData()
    }

    fun syncData() {
        if (userId.isNotBlank() && userId != "guest_user") {
            viewModelScope.launch {
                customerRepository.syncWithSupabase(userId, accessToken)
                transactionRepository.syncWithSupabase(userId, accessToken)
            }
        }
    }
}
