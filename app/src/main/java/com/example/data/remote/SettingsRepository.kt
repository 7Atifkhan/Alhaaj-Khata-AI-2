package com.example.data.remote

import com.example.data.remote.models.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SettingsRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun getSettings(userId: String, accessToken: String?): AuthResult<Settings?> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/settings?user_id=eq.$userId&select=*"
            val token = if (!accessToken.isNullOrBlank()) accessToken else SupabaseClientProvider.SUPABASE_ANON_KEY
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val jsonArray = JSONArray(bodyStr)
                    if (jsonArray.length() > 0) {
                        val settingsObj = jsonArray.getJSONObject(0)
                        AuthResult.Success(Settings.fromJsonObject(settingsObj))
                    } else {
                        AuthResult.Success(null)
                    }
                } else {
                    AuthResult.Error("Failed to fetch settings: HTTP ${response.code}")
                }
            }
        } catch (e: IOException) {
            AuthResult.Error("Network error while retrieving settings.")
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "An unexpected error occurred.")
        }
    }

    suspend fun saveSettings(settings: Settings, accessToken: String?): AuthResult<Settings> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/settings"
            val token = if (!accessToken.isNullOrBlank()) accessToken else SupabaseClientProvider.SUPABASE_ANON_KEY
            val requestBody = settings.toJsonObject().toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation,resolution=merge-duplicates")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val jsonArray = JSONArray(bodyStr)
                    if (jsonArray.length() > 0) {
                        val saved = Settings.fromJsonObject(jsonArray.getJSONObject(0))
                        AuthResult.Success(saved)
                    } else {
                        AuthResult.Success(settings)
                    }
                } else {
                    val errorObj = try { JSONObject(bodyStr) } catch (e: Exception) { null }
                    val msg = errorObj?.optString("message") ?: "Failed to save settings."
                    AuthResult.Error(msg)
                }
            }
        } catch (e: IOException) {
            AuthResult.Error("Network error while saving settings.")
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Failed to save settings.")
        }
    }
}
