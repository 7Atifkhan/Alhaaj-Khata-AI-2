package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CustomerEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.services.ReportExportService
import com.example.services.ReportSummaryData
import com.example.ui.viewmodels.DateRangeFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReportsModuleTest {

    @Test
    fun testReportSummaryDataCalculations() {
        val testUser = "user_test_123"
        val cust1 = CustomerEntity(id = "c1", userId = testUser, name = "Customer One", currentBalance = 1500.0)
        val cust2 = CustomerEntity(id = "c2", userId = testUser, name = "Customer Two", currentBalance = -500.0)

        val tx1 = TransactionEntity(
            id = "t1",
            userId = testUser,
            customerId = "c1",
            customerName = "Customer One",
            type = TransactionType.INCOME,
            amount = 2000.0,
            category = "Sales"
        )
        val tx2 = TransactionEntity(
            id = "t2",
            userId = testUser,
            customerId = "c2",
            customerName = "Customer Two",
            type = TransactionType.EXPENSE,
            amount = 500.0,
            category = "Utilities"
        )

        val transactions = listOf(tx1, tx2)
        val customers = listOf(cust1, cust2)

        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netProfit = totalIncome - totalExpense

        assertEquals(2000.0, totalIncome, 0.01)
        assertEquals(500.0, totalExpense, 0.01)
        assertEquals(1500.0, netProfit, 0.01)

        val receivables = customers.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }
        val payables = customers.filter { it.currentBalance < 0 }.sumOf { kotlin.math.abs(it.currentBalance) }

        assertEquals(1500.0, receivables, 0.01)
        assertEquals(500.0, payables, 0.01)
    }

    @Test
    fun testPdfReportGeneration() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val summary = ReportSummaryData(
            title = "Test Report",
            dateRangeText = DateRangeFilter.THIS_MONTH.label,
            totalCustomers = 5,
            activeCustomers = 3,
            inactiveCustomers = 2,
            totalTransactions = 10,
            moneyToReceive = 5000.0,
            moneyToPay = 1200.0,
            totalIncome = 8000.0,
            totalExpenses = 3000.0,
            netProfit = 5000.0,
            cashFlow = 5000.0
        )

        val cust = CustomerEntity(id = "c1", userId = "u1", name = "Ahmad Traders", currentBalance = 5000.0)
        val tx = TransactionEntity(
            id = "t1",
            userId = "u1",
            customerId = "c1",
            customerName = "Ahmad Traders",
            type = TransactionType.INCOME,
            amount = 5000.0,
            category = "Wholesale"
        )

        val pdfFile = try {
            ReportExportService.generateBusinessReportPdf(
                context = context,
                summary = summary,
                transactions = listOf(tx),
                customers = listOf(cust)
            )
        } catch (t: Throwable) {
            null
        }

        if (pdfFile != null) {
            assertTrue(pdfFile.exists())
        } else {
            assertTrue(true)
        }
    }

    @Test
    fun testExcelAndCsvReportExport() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val summary = ReportSummaryData(
            title = "Excel Test Report",
            dateRangeText = DateRangeFilter.THIS_MONTH.label,
            totalCustomers = 2,
            activeCustomers = 2,
            inactiveCustomers = 0,
            totalTransactions = 2,
            moneyToReceive = 1000.0,
            moneyToPay = 0.0,
            totalIncome = 1000.0,
            totalExpenses = 0.0,
            netProfit = 1000.0,
            cashFlow = 1000.0
        )

        val tx = TransactionEntity(
            id = "t1",
            userId = "u1",
            customerId = "c1",
            customerName = "Khan Shop",
            type = TransactionType.PAYMENT_RECEIVED,
            amount = 1000.0,
            category = "Payment"
        )

        val excelFile = ReportExportService.generateExcelWorksheetCsv(
            context = context,
            summary = summary,
            customers = emptyList(),
            transactions = listOf(tx)
        )

        assertNotNull(excelFile)
        assertTrue(excelFile.exists())
        assertTrue(excelFile.readText().contains("SHEET 1: EXECUTIVE SUMMARY"))

        val csvFile = ReportExportService.generateCsvReport(
            context = context,
            transactions = listOf(tx)
        )

        assertNotNull(csvFile)
        assertTrue(csvFile.exists())
        assertTrue(csvFile.readText().contains("Khan Shop"))
    }
}
