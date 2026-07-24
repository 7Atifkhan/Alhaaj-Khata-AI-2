package com.example.data.remote.models

import org.json.JSONObject

data class Profile(
    val id: String? = null,
    val userId: String,
    val businessName: String,
    val ownerName: String,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val currency: String = "PKR",
    val logoUrl: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            id?.let { put("id", it) }
            put("user_id", userId)
            put("business_name", businessName)
            put("owner_name", ownerName)
            email?.let { put("email", it) }
            phone?.let { put("phone", it) }
            address?.let { put("address", it) }
            put("currency", currency)
            logoUrl?.let { put("logo_url", it) }
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): Profile {
            return Profile(
                id = json.optString("id", null),
                userId = json.optString("user_id", ""),
                businessName = json.optString("business_name", ""),
                ownerName = json.optString("owner_name", ""),
                email = json.optString("email", null),
                phone = json.optString("phone", null),
                address = json.optString("address", null),
                currency = json.optString("currency", "PKR"),
                logoUrl = json.optString("logo_url", null),
                createdAt = json.optString("created_at", null),
                updatedAt = json.optString("updated_at", null)
            )
        }
    }
}
