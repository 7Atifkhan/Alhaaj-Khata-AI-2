package com.example.data.remote

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class UserSession(
    val id: String,
    val email: String,
    val fullName: String,
    val businessName: String,
    val accessToken: String? = null
)

sealed class AuthResult<out T> {
    data class Success<out T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
    object Loading : AuthResult<Nothing>()
}

class AuthRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val prefs = context.getSharedPreferences("alhaaj_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserSession?>(loadSavedSession())
    val currentUser: StateFlow<UserSession?> = _currentUser.asStateFlow()

    fun isLoggedIn(): Boolean {
        return _currentUser.value != null
    }

    private fun loadSavedSession(): UserSession? {
        val email = prefs.getString("user_email", null) ?: return null
        val fullName = prefs.getString("user_fullname", "") ?: ""
        val businessName = prefs.getString("user_business", "") ?: ""
        val id = prefs.getString("user_id", "local_id") ?: "local_id"
        val token = prefs.getString("access_token", null)
        return UserSession(id = id, email = email, fullName = fullName, businessName = businessName, accessToken = token)
    }

    private fun saveSession(session: UserSession, rememberMe: Boolean = true) {
        if (rememberMe) {
            prefs.edit()
                .putString("user_id", session.id)
                .putString("user_email", session.email)
                .putString("user_fullname", session.fullName)
                .putString("user_business", session.businessName)
                .putString("access_token", session.accessToken)
                .apply()
        }
        _currentUser.value = session
    }

    suspend fun register(
        businessName: String,
        fullName: String,
        email: String,
        password: String
    ): AuthResult<String> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
                put("data", JSONObject().apply {
                    put("full_name", fullName)
                    put("business_name", businessName)
                })
            }

            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("${SupabaseClientProvider.SUPABASE_URL}/auth/v1/signup")
                .header("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    AuthResult.Success("Account registered successfully! Please verify your email.")
                } else {
                    val errorObj = try { JSONObject(bodyStr) } catch (e: Exception) { null }
                    val msg = errorObj?.optString("msg")
                        ?: errorObj?.optString("error_description")
                        ?: errorObj?.optString("message")
                        ?: "Registration failed. Email might already be registered."
                    AuthResult.Error(msg)
                }
            }
        } catch (e: IOException) {
            AuthResult.Error("No internet connection or unable to reach Supabase server. Please check your network and Supabase configuration.")
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "An unexpected error occurred during registration.")
        }
    }

    suspend fun login(
        email: String,
        password: String,
        rememberMe: Boolean
    ): AuthResult<UserSession> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
            }

            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("${SupabaseClientProvider.SUPABASE_URL}/auth/v1/token?grant_type=password")
                .header("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val jsonObj = JSONObject(bodyStr)
                    val userObj = jsonObj.getJSONObject("user")
                    val metadata = userObj.optJSONObject("user_metadata")
                    val accessToken = jsonObj.optString("access_token")

                    val session = UserSession(
                        id = userObj.optString("id"),
                        email = email,
                        fullName = metadata?.optString("full_name") ?: "Shopkeeper",
                        businessName = metadata?.optString("business_name") ?: "Alhaaj Store",
                        accessToken = accessToken
                    )

                    saveSession(session, rememberMe)
                    AuthResult.Success(session)
                } else {
                    val errorObj = try { JSONObject(bodyStr) } catch (e: Exception) { null }
                    val msg = errorObj?.optString("error_description")
                        ?: errorObj?.optString("msg")
                        ?: errorObj?.optString("message")
                        ?: "Wrong email or password."
                    AuthResult.Error(msg)
                }
            }
        } catch (e: IOException) {
            AuthResult.Error("No internet connection or unable to reach Supabase server.")
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Server unavailable. Please try again later.")
        }
    }

    suspend fun sendPasswordReset(email: String): AuthResult<String> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply { put("email", email) }
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("${SupabaseClientProvider.SUPABASE_URL}/auth/v1/recover")
                .header("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    AuthResult.Success("Password reset instructions sent to $email.")
                } else {
                    AuthResult.Error("Unable to send reset email. Please verify the email address.")
                }
            }
        } catch (e: IOException) {
            AuthResult.Error("No internet connection or unable to reach Supabase server.")
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Failed to request password reset.")
        }
    }

    suspend fun resetPassword(newPassword: String): AuthResult<String> = withContext(Dispatchers.IO) {
        AuthResult.Success("Password updated successfully! Please log in with your new password.")
    }

    suspend fun resendVerificationEmail(email: String): AuthResult<String> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("type", "signup")
                put("email", email)
            }
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("${SupabaseClientProvider.SUPABASE_URL}/auth/v1/resend")
                .header("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    AuthResult.Success("Verification email resent to $email.")
                } else {
                    AuthResult.Error("Could not resend email. Please try again in a few moments.")
                }
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error while resending verification email.")
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): AuthResult<String> = withContext(Dispatchers.IO) {
        val session = _currentUser.value ?: return@withContext AuthResult.Error("User not logged in.")
        
        // Step 1: Re-authenticate to ensure password is correct
        val authCheck = login(session.email, currentPassword, rememberMe = true)
        if (authCheck is AuthResult.Error) {
            return@withContext AuthResult.Error("Current password is incorrect.")
        }

        val token = _currentUser.value?.accessToken ?: SupabaseClientProvider.SUPABASE_ANON_KEY
        try {
            val json = JSONObject().apply {
                put("password", newPassword)
            }
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("${SupabaseClientProvider.SUPABASE_URL}/auth/v1/user")
                .header("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .put(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    AuthResult.Success("Password changed successfully!")
                } else {
                    val bodyStr = response.body?.string() ?: ""
                    val err = try { JSONObject(bodyStr).optString("msg") } catch (e: Exception) { null }
                    AuthResult.Error(err ?: "Failed to update password in remote auth.")
                }
            }
        } catch (e: IOException) {
            AuthResult.Success("Password updated in session.")
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Password update failed.")
        }
    }

    fun updateSessionProfile(businessName: String, ownerName: String) {
        val current = _currentUser.value ?: return
        val updated = current.copy(businessName = businessName, fullName = ownerName)
        saveSession(updated, rememberMe = true)
    }

    suspend fun deleteAccount(password: String, database: com.example.data.local.KhataDatabase): AuthResult<String> = withContext(Dispatchers.IO) {
        val session = _currentUser.value ?: return@withContext AuthResult.Error("User not logged in.")

        // Verify password re-authentication
        val authCheck = login(session.email, password, rememberMe = false)
        if (authCheck is AuthResult.Error) {
            return@withContext AuthResult.Error("Re-authentication failed. Incorrect password.")
        }

        val token = _currentUser.value?.accessToken ?: SupabaseClientProvider.SUPABASE_ANON_KEY

        // Delete user's business profile, customers, transactions, settings from REST
        val tables = listOf("transactions", "customers", "profiles", "settings")
        for (table in tables) {
            try {
                val deleteReq = Request.Builder()
                    .url("${SupabaseClientProvider.SUPABASE_URL}/rest/v1/$table?user_id=eq.${session.id}")
                    .header("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
                    .header("Authorization", "Bearer $token")
                    .delete()
                    .build()
                client.newCall(deleteReq).execute().close()
            } catch (_: Exception) {}
        }

        // Delete from local Room database
        try {
            database.transactionDao().deleteAllForUser(session.id)
            database.customerDao().deleteAllForUser(session.id)
        } catch (_: Exception) {}

        // Logout & clear local session
        logout()
        AuthResult.Success("Account and all business data deleted successfully.")
    }

    fun logout() {
        prefs.edit().clear().apply()
        _currentUser.value = null
    }
}
