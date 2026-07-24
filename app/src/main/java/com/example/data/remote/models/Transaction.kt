package com.example.data.remote.models

import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import org.json.JSONObject
import java.util.UUID

data class Transaction(
    val id: String? = null,
    val userId: String,
    val customerId: String,
    val customerName: String? = null,
    val type: String, // PAYMENT_RECEIVED, PAYMENT_GIVEN, INCOME, EXPENSE (or RECEIVED / GIVEN mapped)
    val amount: Double,
    val date: String? = null,
    val notes: String? = null,
    val category: String? = null,
    val paymentMethod: String? = null,
    val attachmentUrl: String? = null,
    val isDeleted: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    fun toJsonObject(useTransactionDateColumn: Boolean = true): JSONObject {
        return JSONObject().apply {
            id?.let { put("id", it) }
            put("user_id", userId)
            put("customer_id", customerId)
            put("type", mapTypeForSupabase(type))
            put("amount", amount)
            put("payment_method", paymentMethod ?: "Cash")
            notes?.let { put("notes", it) }
            val formattedDate = formatToIsoDate(date) ?: java.time.Instant.now().toString()
            if (useTransactionDateColumn) {
                put("transaction_date", formattedDate)
            } else {
                put("date", formattedDate)
            }
            attachmentUrl?.let { put("attachment_url", it) }
        }
    }

    fun toEntity(isSynced: Boolean = true): TransactionEntity {
        return TransactionEntity(
            id = id ?: UUID.randomUUID().toString(),
            userId = userId,
            customerId = customerId,
            customerName = customerName ?: "",
            type = parseType(type),
            amount = amount,
            date = parseDateToMillis(date),
            notes = notes,
            category = category ?: "General",
            paymentMethod = paymentMethod ?: "Cash",
            attachmentUrl = attachmentUrl,
            isDeleted = isDeleted,
            isSynced = isSynced,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    companion object {
        fun mapTypeForSupabase(type: String): String {
            return when (type) {
                "PAYMENT_RECEIVED", "RECEIVED", "GOT", "INCOME" -> "RECEIVED"
                else -> "GIVEN"
            }
        }

        fun parseType(typeStr: String): TransactionType {
            return when (typeStr) {
                "RECEIVED", "PAYMENT_RECEIVED", "GOT", "INCOME" -> TransactionType.PAYMENT_RECEIVED
                else -> TransactionType.PAYMENT_GIVEN
            }
        }

        private fun formatToIsoDate(dateStr: String?): String? {
            if (dateStr.isNullOrBlank()) return null
            val millis = dateStr.toLongOrNull()
            return if (millis != null) {
                try {
                    java.time.Instant.ofEpochMilli(millis).toString()
                } catch (e: Exception) {
                    dateStr
                }
            } else {
                dateStr
            }
        }

        private fun parseDateToMillis(dateStr: String?): Long {
            if (dateStr.isNullOrBlank()) return System.currentTimeMillis()
            dateStr.toLongOrNull()?.let { return it }
            return try {
                java.time.Instant.parse(dateStr).toEpochMilli()
            } catch (e: Exception) {
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                    sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
                } catch (e2: Exception) {
                    System.currentTimeMillis()
                }
            }
        }

        fun fromJsonObject(json: JSONObject): Transaction {
            return Transaction(
                id = if (json.has("id") && !json.isNull("id")) json.optString("id") else null,
                userId = json.optString("user_id", ""),
                customerId = json.optString("customer_id", ""),
                customerName = if (json.has("customer_name") && !json.isNull("customer_name")) json.optString("customer_name") else null,
                type = json.optString("type", "GIVEN"),
                amount = json.optDouble("amount", 0.0),
                date = if (json.has("date") && !json.isNull("date")) json.optString("date") else null,
                notes = if (json.has("notes") && !json.isNull("notes")) json.optString("notes") else null,
                category = if (json.has("category") && !json.isNull("category")) json.optString("category") else null,
                paymentMethod = if (json.has("payment_method") && !json.isNull("payment_method")) json.optString("payment_method") else null,
                attachmentUrl = if (json.has("attachment_url") && !json.isNull("attachment_url")) json.optString("attachment_url") else null,
                isDeleted = json.optBoolean("is_deleted", false),
                createdAt = if (json.has("created_at") && !json.isNull("created_at")) json.optString("created_at") else null,
                updatedAt = if (json.has("updated_at") && !json.isNull("updated_at")) json.optString("updated_at") else null
            )
        }

        fun fromEntity(entity: TransactionEntity): Transaction {
            return Transaction(
                id = entity.id,
                userId = entity.userId,
                customerId = entity.customerId,
                customerName = entity.customerName,
                type = entity.type.name,
                amount = entity.amount,
                date = entity.date.toString(),
                notes = entity.notes,
                category = entity.category,
                paymentMethod = entity.paymentMethod,
                attachmentUrl = entity.attachmentUrl,
                isDeleted = entity.isDeleted
            )
        }
    }
}

