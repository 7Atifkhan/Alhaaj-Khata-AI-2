package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val phone: String? = null,
    val whatsappNumber: String? = null,
    val address: String? = null,
    val photoUrl: String? = null,
    val notes: String? = null,
    val openingBalance: Double = 0.0,
    val balanceType: String = "YOU_WILL_GET", // "YOU_WILL_GET" (Customer Will Pay Me) or "YOU_WILL_GIVE" (I Will Pay Customer)
    val currentBalance: Double = 0.0,
    val lastTransactionDate: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
