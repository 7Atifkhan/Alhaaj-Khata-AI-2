package com.example.data.remote.models

import org.json.JSONObject

data class Settings(
    val id: String? = null,
    val userId: String,
    val language: String = "en",
    val theme: String = "system",
    val notificationsEnabled: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            id?.let { put("id", it) }
            put("user_id", userId)
            put("language", language)
            put("theme", theme)
            put("notifications_enabled", notificationsEnabled)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): Settings {
            return Settings(
                id = json.optString("id", null),
                userId = json.optString("user_id", ""),
                language = json.optString("language", "en"),
                theme = json.optString("theme", "system"),
                notificationsEnabled = json.optBoolean("notifications_enabled", true),
                createdAt = json.optString("created_at", null),
                updatedAt = json.optString("updated_at", null)
            )
        }
    }
}
