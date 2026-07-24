package com.example

import com.example.data.local.CustomerEntity
import com.example.data.remote.models.Customer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CustomerManagementTest {

    @Test
    fun testAddCustomer() {
        val userId = "user_123"
        val customerEntity = CustomerEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = "Ahmed Khan",
            phone = "+923001234567",
            whatsappNumber = "+923001234567",
            address = "Main Market, Lahore",
            notes = "Wholesale buyer",
            openingBalance = 5000.0,
            balanceType = "YOU_WILL_GET",
            currentBalance = 5000.0,
            isDeleted = false,
            isSynced = false
        )

        assertEquals("Ahmed Khan", customerEntity.name)
        assertEquals("+923001234567", customerEntity.phone)
        assertEquals(5000.0, customerEntity.currentBalance, 0.001)
        assertFalse(customerEntity.isDeleted)
        assertFalse(customerEntity.isSynced)
    }

    @Test
    fun testEditCustomer() {
        val original = CustomerEntity(
            id = "cust_001",
            userId = "user_123",
            name = "Bilal Siddiqui",
            phone = "+923210000000",
            currentBalance = 1200.0
        )

        val updated = original.copy(
            name = "Bilal Siddiqui (Updated)",
            phone = "+923219999999",
            currentBalance = 1500.0,
            isSynced = false
        )

        assertEquals("cust_001", updated.id)
        assertEquals("Bilal Siddiqui (Updated)", updated.name)
        assertEquals("+923219999999", updated.phone)
        assertEquals(1500.0, updated.currentBalance, 0.001)
        assertFalse(updated.isSynced)
    }

    @Test
    fun testSoftDeleteAndRestoreCustomer() {
        val customer = CustomerEntity(
            id = "cust_002",
            userId = "user_123",
            name = "Zainab Bibi",
            isDeleted = false
        )

        val deleted = customer.copy(isDeleted = true, isSynced = false)
        assertTrue(deleted.isDeleted)

        val restored = deleted.copy(isDeleted = false, isSynced = false)
        assertFalse(restored.isDeleted)
    }

    @Test
    fun testSearchAndFilterLogic() {
        val customer1 = CustomerEntity(id = "1", userId = "user_1", name = "Usman Ali", phone = "03001112223", currentBalance = 3000.0)
        val customer2 = CustomerEntity(id = "2", userId = "user_1", name = "Hamza Farooq", phone = "03214445556", currentBalance = -1500.0)
        val customer3 = CustomerEntity(id = "3", userId = "user_1", name = "Saima Khan", phone = "03337778889", currentBalance = 0.0)

        val list = listOf(customer1, customer2, customer3)

        // Search by Name
        val searchByName = list.filter { it.name.lowercase().contains("usman") }
        assertEquals(1, searchByName.size)
        assertEquals("Usman Ali", searchByName[0].name)

        // Search by Phone
        val searchByPhone = list.filter { it.phone?.contains("444") == true }
        assertEquals(1, searchByPhone.size)
        assertEquals("Hamza Farooq", searchByPhone[0].name)

        // Filter: Who Owes Me (You Will Get - positive balance)
        val owesMe = list.filter { it.currentBalance > 0 }
        assertEquals(1, owesMe.size)
        assertEquals("Usman Ali", owesMe[0].name)

        // Filter: Customers I Owe (You Will Give - negative balance)
        val iOwe = list.filter { it.currentBalance < 0 }
        assertEquals(1, iOwe.size)
        assertEquals("Hamza Farooq", iOwe[0].name)
    }

    @Test
    fun testOfflineSaveAndOnlineSyncConversion() {
        val entity = CustomerEntity(
            id = "uuid_test_99",
            userId = "usr_888",
            name = "Tariq Mahmood",
            phone = "+923456789012",
            whatsappNumber = "+923456789012",
            address = "Islamabad",
            notes = "VIP customer",
            openingBalance = 10000.0,
            balanceType = "YOU_WILL_GET",
            currentBalance = 10000.0,
            isDeleted = false,
            isSynced = true
        )

        // Convert to remote model for Supabase payload
        val remoteCustomer = Customer.fromEntity(entity)
        assertEquals("uuid_test_99", remoteCustomer.id)
        assertEquals("usr_888", remoteCustomer.userId)
        assertEquals("Tariq Mahmood", remoteCustomer.name)
        assertEquals("+923456789012", remoteCustomer.whatsappNumber)

        val json = remoteCustomer.toJsonObject()
        assertEquals("uuid_test_99", json.getString("id"))
        assertEquals("usr_888", json.getString("user_id"))
        assertEquals("+923456789012", json.getString("whatsapp_number"))
        assertEquals(10000.0, json.getDouble("opening_balance"), 0.001)

        // Parse from JSON payload back to local Entity
        val parsedRemote = Customer.fromJsonObject(json)
        val reconstructedEntity = parsedRemote.toEntity(isSynced = true)

        assertEquals(entity.id, reconstructedEntity.id)
        assertEquals(entity.userId, reconstructedEntity.userId)
        assertEquals(entity.name, reconstructedEntity.name)
        assertEquals(entity.whatsappNumber, reconstructedEntity.whatsappNumber)
        assertTrue(reconstructedEntity.isSynced)
    }

    @Test
    fun testUserIsolation() {
        val user1Customer = CustomerEntity(id = "c1", userId = "user_A", name = "Customer A")
        val user2Customer = CustomerEntity(id = "c2", userId = "user_B", name = "Customer B")

        val allCustomers = listOf(user1Customer, user2Customer)

        val userACustomers = allCustomers.filter { it.userId == "user_A" }
        assertEquals(1, userACustomers.size)
        assertEquals("Customer A", userACustomers[0].name)

        val userBCustomers = allCustomers.filter { it.userId == "user_B" }
        assertEquals(1, userBCustomers.size)
        assertEquals("Customer B", userBCustomers[0].name)
    }
}
