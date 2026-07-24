package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class TransactionType {
    PAYMENT_RECEIVED, // Customer paid me (Debit / Got)
    PAYMENT_GIVEN,    // I gave customer money/credit (Credit / Gave)
    INCOME,           // Business Income
    EXPENSE           // Business Expense
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val customerId: String,
    val customerName: String = "",
    val type: TransactionType,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val category: String = "General",
    val paymentMethod: String = "Cash",
    val attachmentUrl: String? = null,
    val runningBalance: Double = 0.0,
    val isDeleted: Boolean = false,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

