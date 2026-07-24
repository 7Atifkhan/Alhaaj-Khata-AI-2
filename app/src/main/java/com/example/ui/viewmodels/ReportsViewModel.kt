package com.example.ui.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CustomerEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.remote.AuthRepository
import com.example.data.remote.CustomerRepository
import com.example.data.remote.TransactionRepository
import com.example.services.ReportExportService
import com.example.services.ReportSummaryData
import com.example.ui.components.BarChartPoint
import com.example.ui.components.CategorySliceData
import com.example.ui.components.CustomerBalanceItem
import com.example.ui.components.LineTrendPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DateRangeFilter(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    LAST_WEEK("Last Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_YEAR("This Year"),
    CUSTOM("Custom Date Range")
}

data class ReportsUiState(
    val isLoading: Boolean = false,
    val dateFilter: DateRangeFilter = DateRangeFilter.THIS_MONTH,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val selectedCustomerId: String = "ALL",
    val selectedType: TransactionType? = null,
    val selectedCategory: String = "ALL",
    val searchQuery: String = "",
    val allCustomers: List<CustomerEntity> = emptyList(),
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    // Summary Metrics
    val totalCustomersCount: Int = 0,
    val activeCustomersCount: Int = 0,
    val inactiveCustomersCount: Int = 0,
    val totalTransactionsCount: Int = 0,
    val moneyToReceive: Double = 0.0,
    val moneyToPay: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val cashFlow: Double = 0.0,
    // Chart Items
    val monthlyBarPoints: List<BarChartPoint> = emptyList(),
    val categorySlices: List<CategorySliceData> = emptyList(),
    val cashFlowPoints: List<LineTrendPoint> = emptyList(),
    val topCustomersList: List<CustomerBalanceItem> = emptyList(),
    // Selected Customer for Individual Statement
    val selectedCustomerForStatement: CustomerEntity? = null,
    val customerStatementTransactions: List<TransactionEntity> = emptyList(),
    // Export File Result State
    val generatedExportFile: File? = null,
    val exportStatusMessage: String? = null
)

class ReportsViewModel(
    private val transactionRepository: TransactionRepository,
    private val customerRepository: CustomerRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState(isLoading = true))
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    private val userId: String
        get() = authRepository.currentUser.value?.id ?: ""

    init {
        loadData()
    }

    fun loadData() {
        if (userId.isEmpty()) return

        val customersFlow = customerRepository.getCustomersFlow(userId) ?: flowOf(emptyList())
        val transactionsFlow = transactionRepository.getTransactionsFlow(userId) ?: flowOf(emptyList())

        combine(
            customersFlow,
            transactionsFlow
        ) { customers, transactions ->
            calculateMetricsAndFilter(customers, transactions)
        }.launchIn(viewModelScope)
    }

    fun setDateFilter(filter: DateRangeFilter, startDate: Long? = null, endDate: Long? = null) {
        _uiState.value = _uiState.value.copy(
            dateFilter = filter,
            customStartDate = startDate,
            customEndDate = endDate
        )
        refreshCalculations()
    }

    fun setCustomerFilter(customerId: String) {
        _uiState.value = _uiState.value.copy(selectedCustomerId = customerId)
        refreshCalculations()
    }

    fun setTypeFilter(type: TransactionType?) {
        _uiState.value = _uiState.value.copy(selectedType = type)
        refreshCalculations()
    }

    fun setCategoryFilter(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        refreshCalculations()
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        refreshCalculations()
    }

    fun selectCustomerForStatement(customer: CustomerEntity?) {
        val custTxs = if (customer != null) {
            _uiState.value.filteredTransactions.filter { it.customerId == customer.id }
        } else emptyList()

        _uiState.value = _uiState.value.copy(
            selectedCustomerForStatement = customer,
            customerStatementTransactions = custTxs
        )
    }

    private fun refreshCalculations() {
        viewModelScope.launch {
            val customers = _uiState.value.allCustomers
            val rawTxsFlow = transactionRepository.getTransactionsFlow(userId) ?: flowOf(emptyList())
            rawTxsFlow.collect { txs ->
                calculateMetricsAndFilter(customers, txs)
            }
        }
    }

    private fun calculateMetricsAndFilter(
        customers: List<CustomerEntity>,
        allTransactions: List<TransactionEntity>
    ) {
        val state = _uiState.value
        val dateBounds = getDateBounds(state.dateFilter, state.customStartDate, state.customEndDate)

        // Filter transactions
        val filteredTxs = allTransactions.filter { tx ->
            val inDateRange = tx.date >= dateBounds.first && tx.date <= dateBounds.second
            val matchesCustomer = state.selectedCustomerId == "ALL" || tx.customerId == state.selectedCustomerId
            val matchesType = state.selectedType == null || tx.type == state.selectedType
            val matchesCategory = state.selectedCategory == "ALL" || tx.category.equals(state.selectedCategory, ignoreCase = true)
            val matchesSearch = state.searchQuery.isEmpty() ||
                    tx.customerName.contains(state.searchQuery, ignoreCase = true) ||
                    tx.category.contains(state.searchQuery, ignoreCase = true) ||
                    tx.notes?.contains(state.searchQuery, ignoreCase = true) == true ||
                    tx.amount.toString().contains(state.searchQuery)

            inDateRange && matchesCustomer && matchesType && matchesCategory && matchesSearch
        }

        // Metrics calculations
        val totalCust = customers.size
        val customerTxMap = allTransactions.groupBy { it.customerId }
        val activeCust = customers.count { (customerTxMap[it.id]?.size ?: 0) > 0 }
        val inactiveCust = totalCust - activeCust

        var moneyReceive = 0.0
        var moneyPay = 0.0

        customers.forEach { c ->
            if (c.currentBalance > 0) {
                moneyReceive += c.currentBalance
            } else if (c.currentBalance < 0) {
                moneyPay += kotlin.math.abs(c.currentBalance)
            }
        }

        var totalInc = 0.0
        var totalExp = 0.0

        filteredTxs.forEach { tx ->
            when (tx.type) {
                TransactionType.INCOME, TransactionType.PAYMENT_RECEIVED -> totalInc += tx.amount
                TransactionType.EXPENSE, TransactionType.PAYMENT_GIVEN -> totalExp += tx.amount
            }
        }

        val profit = totalInc - totalExp
        val cashFlowVal = totalInc - totalExp

        // Monthly Bar Points
        val monthlyPoints = calculateMonthlyBarPoints(filteredTxs)

        // Category Slices
        val categorySlices = calculateCategorySlices(filteredTxs)

        // Cash Flow Trend
        val cashFlowTrend = calculateCashFlowTrend(filteredTxs)

        // Top Customers List
        val topCustList = customers.sortedByDescending { kotlin.math.abs(it.currentBalance) }
            .take(5)
            .map {
                CustomerBalanceItem(
                    name = it.name,
                    amount = kotlin.math.abs(it.currentBalance),
                    isReceivable = it.currentBalance >= 0
                )
            }

        _uiState.value = state.copy(
            isLoading = false,
            allCustomers = customers,
            filteredTransactions = filteredTxs,
            totalCustomersCount = totalCust,
            activeCustomersCount = activeCust,
            inactiveCustomersCount = inactiveCust,
            totalTransactionsCount = filteredTxs.size,
            moneyToReceive = moneyReceive,
            moneyToPay = moneyPay,
            totalIncome = totalInc,
            totalExpenses = totalExp,
            netProfit = profit,
            cashFlow = cashFlowVal,
            monthlyBarPoints = monthlyPoints,
            categorySlices = categorySlices,
            cashFlowPoints = cashFlowTrend,
            topCustomersList = topCustList
        )
    }

    private fun getDateBounds(
        filter: DateRangeFilter,
        customStart: Long?,
        customEnd: Long?
    ): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return when (filter) {
            DateRangeFilter.TODAY -> {
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 1)
                Pair(start, cal.timeInMillis - 1)
            }
            DateRangeFilter.YESTERDAY -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 1)
                Pair(start, cal.timeInMillis - 1)
            }
            DateRangeFilter.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                val start = cal.timeInMillis
                cal.add(Calendar.WEEK_OF_YEAR, 1)
                Pair(start, cal.timeInMillis - 1)
            }
            DateRangeFilter.LAST_WEEK -> {
                cal.add(Calendar.WEEK_OF_YEAR, -1)
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                val start = cal.timeInMillis
                cal.add(Calendar.WEEK_OF_YEAR, 1)
                Pair(start, cal.timeInMillis - 1)
            }
            DateRangeFilter.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                Pair(start, cal.timeInMillis - 1)
            }
            DateRangeFilter.LAST_MONTH -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                Pair(start, cal.timeInMillis - 1)
            }
            DateRangeFilter.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                val start = cal.timeInMillis
                cal.add(Calendar.YEAR, 1)
                Pair(start, cal.timeInMillis - 1)
            }
            DateRangeFilter.CUSTOM -> {
                val start = customStart ?: 0L
                val end = customEnd ?: System.currentTimeMillis()
                Pair(start, end)
            }
        }
    }

    private fun calculateMonthlyBarPoints(txs: List<TransactionEntity>): List<BarChartPoint> {
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val grouped = txs.groupBy {
            monthFormat.format(Date(it.date))
        }

        return grouped.map { (month, list) ->
            var inc = 0.0
            var exp = 0.0
            list.forEach { t ->
                if (t.type == TransactionType.INCOME || t.type == TransactionType.PAYMENT_RECEIVED) inc += t.amount
                else exp += t.amount
            }
            BarChartPoint(label = month, income = inc, expense = exp)
        }
    }

    private fun calculateCategorySlices(txs: List<TransactionEntity>): List<CategorySliceData> {
        val colors = listOf(
            Color(0xFF00897B), Color(0xFFE53935), Color(0xFF1E88E5),
            Color(0xFFFB8C00), Color(0xFF8E24AA), Color(0xFF3949AB)
        )
        val categoryMap = txs.groupBy { it.category }
        var colorIdx = 0

        return categoryMap.map { (cat, list) ->
            val sum = list.sumOf { it.amount }
            val color = colors[colorIdx % colors.size]
            colorIdx++
            CategorySliceData(category = cat, amount = sum, color = color)
        }.sortedByDescending { it.amount }
    }

    private fun calculateCashFlowTrend(txs: List<TransactionEntity>): List<LineTrendPoint> {
        val dayFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val sortedTxs = txs.sortedBy { it.date }
        val grouped = sortedTxs.groupBy { dayFormat.format(Date(it.date)) }

        return grouped.map { (day, list) ->
            val net = list.sumOf { t ->
                if (t.type == TransactionType.INCOME || t.type == TransactionType.PAYMENT_RECEIVED) t.amount else -t.amount
            }
            LineTrendPoint(label = day, value = net)
        }.takeLast(7)
    }

    fun exportPdf(context: Context) {
        viewModelScope.launch {
            val state = _uiState.value
            val summaryData = ReportSummaryData(
                title = "Alhaaj Khata AI Business Report",
                dateRangeText = state.dateFilter.label,
                totalCustomers = state.totalCustomersCount,
                activeCustomers = state.activeCustomersCount,
                inactiveCustomers = state.inactiveCustomersCount,
                totalTransactions = state.totalTransactionsCount,
                moneyToReceive = state.moneyToReceive,
                moneyToPay = state.moneyToPay,
                totalIncome = state.totalIncome,
                totalExpenses = state.totalExpenses,
                netProfit = state.netProfit,
                cashFlow = state.cashFlow
            )
            val file = ReportExportService.generateBusinessReportPdf(
                context = context,
                summary = summaryData,
                transactions = state.filteredTransactions,
                customers = state.allCustomers
            )
            _uiState.value = state.copy(
                generatedExportFile = file,
                exportStatusMessage = "PDF Report generated successfully."
            )
        }
    }

    fun exportExcel(context: Context) {
        viewModelScope.launch {
            val state = _uiState.value
            val summaryData = ReportSummaryData(
                title = "Alhaaj Khata AI Business Report",
                dateRangeText = state.dateFilter.label,
                totalCustomers = state.totalCustomersCount,
                activeCustomers = state.activeCustomersCount,
                inactiveCustomers = state.inactiveCustomersCount,
                totalTransactions = state.totalTransactionsCount,
                moneyToReceive = state.moneyToReceive,
                moneyToPay = state.moneyToPay,
                totalIncome = state.totalIncome,
                totalExpenses = state.totalExpenses,
                netProfit = state.netProfit,
                cashFlow = state.cashFlow
            )
            val file = ReportExportService.generateExcelWorksheetCsv(
                context = context,
                summary = summaryData,
                customers = state.allCustomers,
                transactions = state.filteredTransactions
            )
            _uiState.value = state.copy(
                generatedExportFile = file,
                exportStatusMessage = "Excel Worksheet CSV generated successfully."
            )
        }
    }

    fun exportCsv(context: Context) {
        viewModelScope.launch {
            val state = _uiState.value
            val file = ReportExportService.generateCsvReport(
                context = context,
                transactions = state.filteredTransactions
            )
            _uiState.value = state.copy(
                generatedExportFile = file,
                exportStatusMessage = "CSV Ledger exported successfully."
            )
        }
    }

    fun exportCustomerStatementPdf(context: Context, customer: CustomerEntity) {
        viewModelScope.launch {
            val custTxs = _uiState.value.filteredTransactions.filter { it.customerId == customer.id }
            val file = ReportExportService.generateCustomerStatementPdf(
                context = context,
                customer = customer,
                transactions = custTxs
            )
            _uiState.value = _uiState.value.copy(
                generatedExportFile = file,
                exportStatusMessage = "Customer Statement PDF for ${customer.name} generated."
            )
        }
    }

    fun clearExportStatus() {
        _uiState.value = _uiState.value.copy(
            generatedExportFile = null,
            exportStatusMessage = null
        )
    }
}
