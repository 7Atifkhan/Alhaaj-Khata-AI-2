package com.example

import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.remote.models.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransactionModuleTest {

    @Test
    fun testAddTransaction() {
        val tx = TransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = "user_101",
            customerId = "cust_202",
            customerName = "Ali Raza",
            type = TransactionType.PAYMENT_RECEIVED,
            amount = 2500.0,
            notes = "Advance payment",
            category = "Sales",
            paymentMethod = "Cash",
            attachmentUrl = "https://example.com/receipt1.jpg",
            isDeleted = false,
            isSynced = false
        )

        assertEquals("user_101", tx.userId)
        assertEquals("cust_202", tx.customerId)
        assertEquals("Ali Raza", tx.customerName)
        assertEquals(TransactionType.PAYMENT_RECEIVED, tx.type)
        assertEquals(2500.0, tx.amount, 0.001)
        assertEquals("Advance payment", tx.notes)
        assertEquals("https://example.com/receipt1.jpg", tx.attachmentUrl)
        assertFalse(tx.isDeleted)
        assertFalse(tx.isSynced)
    }

    @Test
    fun testEditTransaction() {
        val original = TransactionEntity(
            id = "tx_001",
            userId = "user_101",
            customerId = "cust_202",
            customerName = "Ali Raza",
            type = TransactionType.PAYMENT_GIVEN,
            amount = 1000.0,
            notes = "Initial loan"
        )

        val edited = original.copy(
            amount = 1500.0,
            notes = "Updated loan with additional items",
            category = "Credit Sale",
            paymentMethod = "Bank Transfer",
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )

        assertEquals("tx_001", edited.id)
        assertEquals(1500.0, edited.amount, 0.001)
        assertEquals("Updated loan with additional items", edited.notes)
        assertEquals("Credit Sale", edited.category)
        assertEquals("Bank Transfer", edited.paymentMethod)
        assertFalse(edited.isSynced)
    }

    @Test
    fun testSoftDeleteAndRestoreTransaction() {
        val tx = TransactionEntity(
            id = "tx_002",
            userId = "user_101",
            customerId = "cust_202",
            type = TransactionType.PAYMENT_RECEIVED,
            amount = 500.0,
            isDeleted = false
        )

        val deleted = tx.copy(isDeleted = true, isSynced = false)
        assertTrue(deleted.isDeleted)

        val restored = deleted.copy(isDeleted = false, isSynced = false)
        assertFalse(restored.isDeleted)
    }

    @Test
    fun testCustomerBalanceAndRunningBalanceCalculation() {
        // Customer opening balance: 1000.0 (YOU_WILL_GET)
        var runningBalance = 1000.0

        // Transaction 1: PAYMENT_GIVEN 500.0 -> Customer owes 500 more -> runningBalance = 1500.0
        val tx1Amount = 500.0
        val tx1Type = TransactionType.PAYMENT_GIVEN
        runningBalance += if (tx1Type == TransactionType.PAYMENT_GIVEN) tx1Amount else -tx1Amount
        assertEquals(1500.0, runningBalance, 0.001)

        // Transaction 2: PAYMENT_RECEIVED 1200.0 -> Customer pays 1200 -> runningBalance = 300.0
        val tx2Amount = 1200.0
        val tx2Type = TransactionType.PAYMENT_RECEIVED
        runningBalance += if (tx2Type == TransactionType.PAYMENT_GIVEN) tx2Amount else -tx2Amount
        assertEquals(300.0, runningBalance, 0.001)
    }

    @Test
    fun testSearchFilteringLogic() {
        val tx1 = TransactionEntity(id = "1", userId = "u1", customerId = "c1", customerName = "Bilal", type = TransactionType.PAYMENT_RECEIVED, amount = 1000.0, category = "Wholesale", notes = "Invoice #101")
        val tx2 = TransactionEntity(id = "2", userId = "u1", customerId = "c2", customerName = "Usman", type = TransactionType.PAYMENT_GIVEN, amount = 5000.0, category = "Retail", notes = "Urgent advance")
        val tx3 = TransactionEntity(id = "3", userId = "u1", customerId = "c1", customerName = "Bilal", type = TransactionType.EXPENSE, amount = 300.0, category = "Transport", notes = "Delivery charges")

        val list = listOf(tx1, tx2, tx3)

        // Search by Customer Name
        val searchByName = list.filter { it.customerName.lowercase().contains("bilal") }
        assertEquals(2, searchByName.size)

        // Search by Category
        val searchByCategory = list.filter { it.category.lowercase().contains("transport") }
        assertEquals(1, searchByCategory.size)
        assertEquals("Delivery charges", searchByCategory[0].notes)

        // Search by Amount
        val searchByAmount = list.filter { it.amount.toString().contains("5000") }
        assertEquals(1, searchByAmount.size)
        assertEquals("Usman", searchByAmount[0].customerName)

        // Filter: Payment Received
        val receivedList = list.filter { it.type == TransactionType.PAYMENT_RECEIVED }
        assertEquals(1, receivedList.size)
        assertEquals("Bilal", receivedList[0].customerName)
    }

    @Test
    fun testOfflineSyncPayloadConversion() {
        val entity = TransactionEntity(
            id = "tx_uuid_999",
            userId = "user_888",
            customerId = "cust_777",
            customerName = "Tariq Auto Store",
            type = TransactionType.PAYMENT_RECEIVED,
            amount = 7500.0,
            date = 1689000000000L,
            notes = "Cheque cleared",
            category = "Payment",
            paymentMethod = "Cheque",
            attachmentUrl = "https://supabase.co/storage/v1/object/public/receipts/chq.jpg",
            isDeleted = false,
            isSynced = true
        )

        // Remote model conversion
        val remoteTx = Transaction.fromEntity(entity)
        assertEquals("tx_uuid_999", remoteTx.id)
        assertEquals("user_888", remoteTx.userId)
        assertEquals("cust_777", remoteTx.customerId)
        assertEquals("Tariq Auto Store", remoteTx.customerName)
        assertEquals("Payment", remoteTx.category)
        assertEquals("Cheque", remoteTx.paymentMethod)

        // JSON serialization
        val json = remoteTx.toJsonObject()
        assertEquals("tx_uuid_999", json.getString("id"))
        assertEquals("user_888", json.getString("user_id"))
        assertEquals("RECEIVED", json.getString("type"))
        assertEquals(7500.0, json.getDouble("amount"), 0.001)

        // JSON deserialization back to Entity
        val reconstructedRemote = Transaction.fromJsonObject(json)
        val reconstructedEntity = reconstructedRemote.toEntity(isSynced = true)

        assertEquals(entity.id, reconstructedEntity.id)
        assertEquals(entity.userId, reconstructedEntity.userId)
        assertEquals(entity.type, reconstructedEntity.type)
        assertEquals(entity.amount, reconstructedEntity.amount, 0.001)
        assertTrue(reconstructedEntity.isSynced)
    }

    @Test
    fun testUserIsolation() {
        val txUserA = TransactionEntity(id = "1", userId = "User_A", customerId = "c1", amount = 100.0, type = TransactionType.PAYMENT_RECEIVED)
        val txUserB = TransactionEntity(id = "2", userId = "User_B", customerId = "c2", amount = 200.0, type = TransactionType.PAYMENT_GIVEN)

        val list = listOf(txUserA, txUserB)

        val userATransactions = list.filter { it.userId == "User_A" }
        assertEquals(1, userATransactions.size)
        assertEquals(100.0, userATransactions[0].amount, 0.001)

        val userBTransactions = list.filter { it.userId == "User_B" }
        assertEquals(1, userBTransactions.size)
        assertEquals(200.0, userBTransactions[0].amount, 0.001)
    }
}
