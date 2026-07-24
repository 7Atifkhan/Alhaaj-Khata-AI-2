package com.example.data.remote.models

import com.example.data.local.CustomerEntity
import org.json.JSONObject

data class Customer(
    val id: String? = null,
    val userId: String,
    val name: String,
    val phone: String? = null,
    val whatsappNumber: String? = null,
    val address: String? = null,
    val photoUrl: String? = null,
    val notes: String? = null,
    val openingBalance: Double = 0.0,
    val balanceType: String = "YOU_WILL_GET",
    val currentBalance: Double = 0.0,
    val isDeleted: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            id?.let { put("id", it) }
            put("user_id", userId)
            put("name", name)
            phone?.let { put("phone", it) }
            address?.let { put("address", it) }
            photoUrl?.let { put("photo_url", it) }
            notes?.let { put("notes", it) }
        }
    }

    fun toEntity(isSynced: Boolean = true): CustomerEntity {
        return CustomerEntity(
            id = id ?: java.util.UUID.randomUUID().toString(),
            userId = userId,
            name = name,
            phone = phone,
            whatsappNumber = whatsappNumber,
            address = address,
            photoUrl = photoUrl,
            notes = notes,
            openingBalance = openingBalance,
            balanceType = balanceType,
            currentBalance = currentBalance,
            isDeleted = isDeleted,
            isSynced = isSynced,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    companion object {
        fun fromJsonObject(json: JSONObject): Customer {
            return Customer(
                id = if (json.has("id") && !json.isNull("id")) json.optString("id") else null,
                userId = json.optString("user_id", ""),
                name = json.optString("name", ""),
                phone = if (json.has("phone") && !json.isNull("phone")) json.optString("phone") else null,
                whatsappNumber = if (json.has("whatsapp_number") && !json.isNull("whatsapp_number")) json.optString("whatsapp_number") else null,
                address = if (json.has("address") && !json.isNull("address")) json.optString("address") else null,
                photoUrl = if (json.has("photo_url") && !json.isNull("photo_url")) json.optString("photo_url") else null,
                notes = if (json.has("notes") && !json.isNull("notes")) json.optString("notes") else null,
                openingBalance = json.optDouble("opening_balance", 0.0),
                balanceType = json.optString("balance_type", "YOU_WILL_GET"),
                currentBalance = json.optDouble("current_balance", 0.0),
                isDeleted = json.optBoolean("is_deleted", false),
                createdAt = if (json.has("created_at") && !json.isNull("created_at")) json.optString("created_at") else null,
                updatedAt = if (json.has("updated_at") && !json.isNull("updated_at")) json.optString("updated_at") else null
            )
        }

        fun fromEntity(entity: CustomerEntity): Customer {
            return Customer(
                id = entity.id,
                userId = entity.userId,
                name = entity.name,
                phone = entity.phone,
                whatsappNumber = entity.whatsappNumber,
                address = entity.address,
                photoUrl = entity.photoUrl,
                notes = entity.notes,
                openingBalance = entity.openingBalance,
                balanceType = entity.balanceType,
                currentBalance = entity.currentBalance,
                isDeleted = entity.isDeleted
            )
        }
    }
}

