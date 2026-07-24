package com.example

import com.example.data.remote.models.Customer
import com.example.data.remote.models.Profile
import com.example.data.remote.models.Settings
import com.example.data.remote.models.Transaction
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseModuleTest {

    @Test
    fun testProfileCreationAndSerialization() {
        val userId = "usr_test_123"
        val profile = Profile(
            userId = userId,
            businessName = "Alhaaj Traders",
            ownerName = "Muhammad Ali",
            phone = "+923001234567",
            address = "Lahore, Pakistan",
            currency = "PKR",
            logoUrl = "https://example.com/logo.png"
        )

        val json = profile.toJsonObject()
        assertEquals(userId, json.getString("user_id"))
        assertEquals("Alhaaj Traders", json.getString("business_name"))
        assertEquals("Muhammad Ali", json.getString("owner_name"))
        assertEquals("+923001234567", json.getString("phone"))
        assertEquals("Lahore, Pakistan", json.getString("address"))
        assertEquals("PKR", json.getString("currency"))

        // Deserialization check (Profile Retrieval)
        val deserialized = Profile.fromJsonObject(json)
        assertEquals(profile.userId, deserialized.userId)
        assertEquals(profile.businessName, deserialized.businessName)
        assertEquals(profile.ownerName, deserialized.ownerName)
    }

    @Test
    fun testProfileUpdate() {
        val originalJson = JSONObject().apply {
            put("id", "prof_uuid_1")
            put("user_id", "usr_test_123")
            put("business_name", "Old Shop Name")
            put("owner_name", "Old Owner")
            put("currency", "PKR")
        }

        val profile = Profile.fromJsonObject(originalJson)
        val updatedProfile = profile.copy(
            businessName = "Updated Alhaaj Superstore",
            ownerName = "New Owner Name"
        )

        val updatedJson = updatedProfile.toJsonObject()
        assertEquals("prof_uuid_1", updatedJson.getString("id"))
        assertEquals("usr_test_123", updatedJson.getString("user_id"))
        assertEquals("Updated Alhaaj Superstore", updatedJson.getString("business_name"))
        assertEquals("New Owner Name", updatedJson.getString("owner_name"))
    }

    @Test
    fun testCustomerAndRLSUserIsolation() {
        val userId = "usr_user_456"
        val customer = Customer(
            userId = userId,
            name = "Tariq Khan",
            phone = "+923219876543",
            address = "Karachi",
            notes = "Regular buyer"
        )

        val json = customer.toJsonObject()
        assertEquals(userId, json.getString("user_id"))
        assertEquals("Tariq Khan", json.getString("name"))

        val parsed = Customer.fromJsonObject(json)
        assertEquals(userId, parsed.userId)
    }

    @Test
    fun testTransactionAndRLSUserIsolation() {
        val userId = "usr_user_789"
        val customerId = "cust_uuid_99"
        val transaction = Transaction(
            userId = userId,
            customerId = customerId,
            type = "RECEIVED",
            amount = 5000.0,
            notes = "Payment for invoice #102"
        )

        val json = transaction.toJsonObject()
        assertEquals(userId, json.getString("user_id"))
        assertEquals(customerId, json.getString("customer_id"))
        assertEquals("RECEIVED", json.getString("type"))
        assertEquals(5000.0, json.getDouble("amount"), 0.001)

        val parsed = Transaction.fromJsonObject(json)
        assertEquals(userId, parsed.userId)
        assertEquals(customerId, parsed.customerId)
    }

    @Test
    fun testSettingsDefaultsAndSerialization() {
        val userId = "usr_settings_001"
        val settings = Settings(userId = userId)

        val json = settings.toJsonObject()
        assertEquals(userId, json.getString("user_id"))
        assertEquals("en", json.getString("language"))
        assertEquals("system", json.getString("theme"))
        assertTrue(json.getBoolean("notifications_enabled"))
    }
}
