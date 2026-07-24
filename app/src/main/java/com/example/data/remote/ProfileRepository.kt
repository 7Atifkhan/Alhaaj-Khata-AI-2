package com.example.data.remote

import com.example.data.remote.models.Profile
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

class ProfileRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun getProfile(userId: String, accessToken: String?): AuthResult<Profile?> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/profiles?user_id=eq.$userId&select=*"
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
                        val profileObj = jsonArray.getJSONObject(0)
                        AuthResult.Success(Profile.fromJsonObject(profileObj))
                    } else {
                        AuthResult.Success(null)
                    }
                } else {
                    AuthResult.Error("Failed to fetch profile: HTTP ${response.code}")
                }
            }
        } catch (e: IOException) {
            AuthResult.Error("Network error while retrieving profile.")
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "An unexpected error occurred.")
        }
    }

    suspend fun createProfile(profile: Profile, accessToken: String?): AuthResult<Profile> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/profiles"
            val token = if (!accessToken.isNullOrBlank()) accessToken else SupabaseClientProvider.SUPABASE_ANON_KEY
            val requestBody = profile.toJsonObject().toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val jsonArray = JSONArray(bodyStr)
                    if (jsonArray.length() > 0) {
                        val created = Profile.fromJsonObject(jsonArray.getJSONObject(0))
                        AuthResult.Success(created)
                    } else {
                        AuthResult.Success(profile)
                    }
                } else {
                    val errorObj = try { JSONObject(bodyStr) } catch (e: Exception) { null }
                    val msg = errorObj?.optString("message") ?: errorObj?.optString("msg") ?: "Failed to save profile."
                    AuthResult.Error(msg)
                }
            }
        } catch (e: IOException) {
            AuthResult.Error("Network error while creating profile.")
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Failed to create profile.")
        }
    }

    suspend fun updateProfile(profile: Profile, accessToken: String?): AuthResult<Profile> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/profiles?user_id=eq.${profile.userId}"
            val token = if (!accessToken.isNullOrBlank()) accessToken else SupabaseClientProvider.SUPABASE_ANON_KEY
            val requestBody = profile.toJsonObject().toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .patch(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val jsonArray = JSONArray(bodyStr)
                    if (jsonArray.length() > 0) {
                        val updated = Profile.fromJsonObject(jsonArray.getJSONObject(0))
                        AuthResult.Success(updated)
                    } else {
                        AuthResult.Success(profile)
                    }
                } else {
                    val errorObj = try { JSONObject(bodyStr) } catch (e: Exception) { null }
                    val msg = errorObj?.optString("message") ?: errorObj?.optString("msg") ?: "Failed to update profile."
                    AuthResult.Error(msg)
                }
            }
        } catch (e: IOException) {
            AuthResult.Error("Network error while updating profile.")
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Failed to update profile.")
        }
    }
}
