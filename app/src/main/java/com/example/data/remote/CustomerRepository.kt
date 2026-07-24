package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.local.CustomerDao
import com.example.data.local.CustomerEntity
import com.example.data.remote.models.Customer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class CustomerRepository(private val customerDao: CustomerDao? = null) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun uploadCustomerPhoto(
        context: Context,
        imageUri: Uri,
        accessToken: String?
    ): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return@withContext null
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return@withContext null

            // Scale down to max 800px width/height
            val maxDim = 800
            val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val ratio = Math.min(maxDim.toDouble() / bitmap.width, maxDim.toDouble() / bitmap.height)
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt(),
                    (bitmap.height * ratio).toInt(),
                    true
                )
            } else {
                bitmap
            }

            val localDir = File(context.filesDir, "customer_photos")
            if (!localDir.exists()) localDir.mkdirs()
            val fileName = "cust_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val localFile = File(localDir, fileName)

            val baos = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val imageBytes = baos.toByteArray()
            localFile.writeBytes(imageBytes)
            val localPhotoUrl = localFile.toURI().toString()

            if (!SupabaseClientProvider.isConfigured()) {
                return@withContext localPhotoUrl
            }

            val uploadUrl = "${SupabaseClientProvider.SUPABASE_URL}/storage/v1/object/customer-photos/$fileName"
            val token = if (!accessToken.isNullOrBlank()) accessToken else SupabaseClientProvider.SUPABASE_ANON_KEY
            val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaType())

            val request = Request.Builder()
                .url(uploadUrl)
                .header("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "image/jpeg")
                .header("x-upsert", "true")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val publicUrl = "${SupabaseClientProvider.SUPABASE_URL}/storage/v1/object/public/customer-photos/$fileName"
                    publicUrl
                } else {
                    localPhotoUrl
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getCustomersFlow(userId: String): Flow<List<CustomerEntity>>? {
        return customerDao?.getCustomersForUser(userId)
    }

    fun getDeletedCustomersFlow(userId: String): Flow<List<CustomerEntity>>? {
        return customerDao?.getDeletedCustomersForUser(userId)
    }

    suspend fun addCustomer(customerEntity: CustomerEntity, accessToken: String?): AuthResult<CustomerEntity> = withContext(Dispatchers.IO) {
        // 1. Save locally first for instant UI response and offline support
        val localEntity = customerEntity.copy(isSynced = false)
        customerDao?.insertCustomer(localEntity)

        // 2. Attempt Supabase Sync
        val remoteCustomer = Customer.fromEntity(localEntity)
        val remoteResult = createCustomerRemote(remoteCustomer, accessToken)

        if (remoteResult is AuthResult.Success) {
            val syncedEntity = localEntity.copy(
                id = remoteResult.data.id ?: localEntity.id,
                isSynced = true
            )
            customerDao?.insertCustomer(syncedEntity)
            AuthResult.Success(syncedEntity)
        } else {
            // Keep saved locally with isSynced = false
            AuthResult.Success(localEntity)
        }
    }

    suspend fun updateCustomer(customerEntity: CustomerEntity, accessToken: String?): AuthResult<CustomerEntity> = withContext(Dispatchers.IO) {
        val updatedLocal = customerEntity.copy(
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        customerDao?.updateCustomer(updatedLocal)

        val remoteCustomer = Customer.fromEntity(updatedLocal)
        val remoteResult = updateCustomerRemote(remoteCustomer, accessToken)

        if (remoteResult is AuthResult.Success) {
            val synced = updatedLocal.copy(isSynced = true)
            customerDao?.updateCustomer(synced)
            AuthResult.Success(synced)
        } else {
            AuthResult.Success(updatedLocal)
        }
    }

    suspend fun softDeleteCustomer(id: String, userId: String, accessToken: String?): AuthResult<Boolean> = withContext(Dispatchers.IO) {
        customerDao?.softDeleteCustomer(id)
        val customer = customerDao?.getCustomerById(id)
        if (customer != null) {
            val remoteResult = updateCustomerRemote(Customer.fromEntity(customer), accessToken)
            if (remoteResult is AuthResult.Success) {
                customerDao.updateCustomer(customer.copy(isSynced = true))
            }
        }
        AuthResult.Success(true)
    }

    suspend fun restoreCustomer(id: String, userId: String, accessToken: String?): AuthResult<Boolean> = withContext(Dispatchers.IO) {
        customerDao?.restoreCustomer(id)
        val customer = customerDao?.getCustomerById(id)
        if (customer != null) {
            val remoteResult = updateCustomerRemote(Customer.fromEntity(customer), accessToken)
            if (remoteResult is AuthResult.Success) {
                customerDao.updateCustomer(customer.copy(isSynced = true))
            }
        }
        AuthResult.Success(true)
    }

    suspend fun syncWithSupabase(userId: String, accessToken: String?): AuthResult<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch remote customers for this user
            val remoteCustomersResult = getCustomers(userId, accessToken)
            if (remoteCustomersResult is AuthResult.Success) {
                val remoteList = remoteCustomersResult.data
                val remoteEntities = remoteList.map { it.toEntity(isSynced = true) }
                customerDao?.insertCustomers(remoteEntities)
            }

            // 2. Push unsynced local customers
            val unsyncedList = customerDao?.getUnsyncedCustomers(userId) ?: emptyList()
            for (unsynced in unsyncedList) {
                val remoteRes = if (unsynced.createdAt == unsynced.updatedAt) {
                    createCustomerRemote(Customer.fromEntity(unsynced), accessToken)
                } else {
                    updateCustomerRemote(Customer.fromEntity(unsynced), accessToken)
                }
                if (remoteRes is AuthResult.Success) {
                    customerDao?.updateCustomer(unsynced.copy(isSynced = true))
                }
            }
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Sync error")
        }
    }

    suspend fun getCustomers(userId: String, accessToken: String?): AuthResult<List<Customer>> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("SupabaseDB", "SELECT customers for user_id: $userId")
            println("[SupabaseDB] SELECT customers for user_id: $userId")
            val url = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/customers?user_id=eq.$userId&select=*"
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
                    val list = mutableListOf<Customer>()
                    for (i in 0 until jsonArray.length()) {
                        list.add(Customer.fromJsonObject(jsonArray.getJSONObject(i)))
                    }
                    AuthResult.Success(list)
                } else {
                    AuthResult.Error("Failed to fetch customers: HTTP ${response.code}")
                }
            }
        } catch (e: IOException) {
            AuthResult.Error("Network error while retrieving customers.")
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "An error occurred.")
        }
    }

    suspend fun createCustomerRemote(customer: Customer, accessToken: String?): AuthResult<Customer> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("SupabaseDB", "INSERT customer: name=${customer.name}, id=${customer.id}, user_id=${customer.userId}")
            println("[SupabaseDB] INSERT customer: name=${customer.name}, id=${customer.id}, user_id=${customer.userId}")
            val url = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/customers"
            val token = if (!accessToken.isNullOrBlank()) accessToken else SupabaseClientProvider.SUPABASE_ANON_KEY
            val requestBody = customer.toJsonObject().toString().toRequestBody("application/json".toMediaType())

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
                        val created = Customer.fromJsonObject(jsonArray.getJSONObject(0))
                        AuthResult.Success(created)
                    } else {
                        AuthResult.Success(customer)
                    }
                } else {
                    val errorObj = try { JSONObject(bodyStr) } catch (e: Exception) { null }
                    val msg = errorObj?.optString("message") ?: "Failed to add customer."
                    AuthResult.Error(msg)
                }
            }
        } catch (e: IOException) {
            AuthResult.Error("Network error while creating customer.")
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Failed to create customer.")
        }
    }

    suspend fun updateCustomerRemote(customer: Customer, accessToken: String?): AuthResult<Customer> = withContext(Dispatchers.IO) {
        try {
            val id = customer.id ?: return@withContext AuthResult.Error("Missing customer ID")
            val url = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/customers?id=eq.$id"
            val token = if (!accessToken.isNullOrBlank()) accessToken else SupabaseClientProvider.SUPABASE_ANON_KEY
            val requestBody = customer.toJsonObject().toString().toRequestBody("application/json".toMediaType())

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
                    AuthResult.Success(customer)
                } else {
                    val errorObj = try { JSONObject(bodyStr) } catch (e: Exception) { null }
                    val msg = errorObj?.optString("message") ?: "Failed to update customer."
                    AuthResult.Error(msg)
                }
            }
        } catch (e: IOException) {
            AuthResult.Error("Network error while updating customer.")
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Failed to update customer.")
        }
    }
}

