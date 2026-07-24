package com.example.data.remote

import com.example.data.local.CustomerDao
import com.example.data.local.TransactionDao
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.remote.models.Customer
import com.example.data.remote.models.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class TransactionRepository(
    private val transactionDao: TransactionDao? = null,
    private val customerDao: CustomerDao? = null
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun getTransactionsFlow(userId: String): Flow<List<TransactionEntity>>? {
        return transactionDao?.getTransactionsForUser(userId)
    }

    fun getDeletedTransactionsFlow(userId: String): Flow<List<TransactionEntity>>? {
        return transactionDao?.getDeletedTransactionsForUser(userId)
    }

    fun getCustomerTransactionsFlow(customerId: String): Flow<List<TransactionEntity>>? {
        return transactionDao?.getTransactionsForCustomerDescending(customerId)
    }

    suspend fun recalculateCustomerBalances(customerId: String) = withContext(Dispatchers.IO) {
        if (transactionDao == null || customerDao == null) return@withContext

        val customer = customerDao.getCustomerById(customerId) ?: return@withContext
        val rawTxList = transactionDao.getTransactionsForCustomerAscending(customerId).first()

        var currentBal = if (customer.balanceType == "YOU_WILL_GIVE") -Math.abs(customer.openingBalance) else Math.abs(customer.openingBalance)

        for (tx in rawTxList) {
            val delta = when (tx.type) {
                TransactionType.PAYMENT_GIVEN, TransactionType.EXPENSE -> tx.amount
                TransactionType.PAYMENT_RECEIVED, TransactionType.INCOME -> -tx.amount
            }
            currentBal += delta
            if (tx.runningBalance != currentBal) {
                transactionDao.updateTransaction(tx.copy(runningBalance = currentBal))
            }
        }

        if (customer.currentBalance != currentBal) {
            customerDao.updateCustomer(customer.copy(currentBalance = currentBal, updatedAt = System.currentTimeMillis(), isSynced = false))
        }
    }

    suspend fun addTransaction(
        transactionEntity: TransactionEntity,
        accessToken: String?
    ): AuthResult<TransactionEntity> = withContext(Dispatchers.IO) {
        val local = transactionEntity.copy(isSynced = false)
        transactionDao?.insertTransaction(local)
        recalculateCustomerBalances(local.customerId)

        val updatedLocal = transactionDao?.getTransactionById(local.id) ?: local
        val remoteRes = createTransactionRemote(Transaction.fromEntity(updatedLocal), accessToken)

        if (remoteRes is AuthResult.Success) {
            val synced = updatedLocal.copy(
                id = remoteRes.data.id ?: updatedLocal.id,
                isSynced = true
            )
            transactionDao?.insertTransaction(synced)
            syncWithSupabase(updatedLocal.userId, accessToken)
            AuthResult.Success(synced)
        } else {
            remoteRes as AuthResult.Error
        }
    }

    suspend fun updateTransaction(
        transactionEntity: TransactionEntity,
        accessToken: String?
    ): AuthResult<TransactionEntity> = withContext(Dispatchers.IO) {
        val updatedLocal = transactionEntity.copy(
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        transactionDao?.updateTransaction(updatedLocal)
        recalculateCustomerBalances(updatedLocal.customerId)

        val remoteRes = updateTransactionRemote(Transaction.fromEntity(updatedLocal), accessToken)
        if (remoteRes is AuthResult.Success) {
            val synced = updatedLocal.copy(isSynced = true)
            transactionDao?.updateTransaction(synced)
            syncWithSupabase(updatedLocal.userId, accessToken)
            AuthResult.Success(synced)
        } else {
            remoteRes as AuthResult.Error
        }
    }

    suspend fun softDeleteTransaction(id: String, accessToken: String?): AuthResult<Boolean> = withContext(Dispatchers.IO) {
        val tx = transactionDao?.getTransactionById(id)
        transactionDao?.softDeleteTransaction(id)
        if (tx != null) {
            recalculateCustomerBalances(tx.customerId)
            val updatedTx = transactionDao?.getTransactionById(id)
            if (updatedTx != null) {
                updateTransactionRemote(Transaction.fromEntity(updatedTx), accessToken)
            }
        }
        AuthResult.Success(true)
    }

    suspend fun restoreTransaction(id: String, accessToken: String?): AuthResult<Boolean> = withContext(Dispatchers.IO) {
        transactionDao?.restoreTransaction(id)
        val tx = transactionDao?.getTransactionById(id)
        if (tx != null) {
            recalculateCustomerBalances(tx.customerId)
            val updatedTx = transactionDao?.getTransactionById(id)
            if (updatedTx != null) {
                updateTransactionRemote(Transaction.fromEntity(updatedTx), accessToken)
            }
        }
        AuthResult.Success(true)
    }

    suspend fun syncWithSupabase(userId: String, accessToken: String?): AuthResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val remoteResult = getTransactions(userId, accessToken)
            if (remoteResult is AuthResult.Success) {
                val remoteEntities = remoteResult.data.map { it.toEntity(isSynced = true) }
                transactionDao?.insertTransactions(remoteEntities)

                // Recalculate customer balances for all affected customers
                val customerIds = remoteEntities.map { it.customerId }.distinct()
                for (cId in customerIds) {
                    recalculateCustomerBalances(cId)
                }
            }

            val unsyncedList = transactionDao?.getUnsyncedTransactions(userId) ?: emptyList()
            for (unsynced in unsyncedList) {
                val remoteRes = if (unsynced.createdAt == unsynced.updatedAt) {
                    createTransactionRemote(Transaction.fromEntity(unsynced), accessToken)
                } else {
                    updateTransactionRemote(Transaction.fromEntity(unsynced), accessToken)
                }
                if (remoteRes is AuthResult.Success) {
                    transactionDao?.updateTransaction(unsynced.copy(isSynced = true))
                }
            }
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Sync error")
        }
    }

    suspend fun getTransactions(userId: String, accessToken: String?): AuthResult<List<Transaction>> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("SupabaseDB", "SELECT transactions for user_id: $userId")
            println("[SupabaseDB] SELECT transactions for user_id: $userId")
            val url = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/transactions?user_id=eq.$userId&select=*"
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
                android.util.Log.d("SupabaseDB", "SELECT transactions response code=${response.code}, body=$bodyStr")
                println("[SupabaseDB] SELECT transactions response code=${response.code}, body=$bodyStr")
                if (response.isSuccessful) {
                    val jsonArray = JSONArray(bodyStr)
                    val list = mutableListOf<Transaction>()
                    for (i in 0 until jsonArray.length()) {
                        list.add(Transaction.fromJsonObject(jsonArray.getJSONObject(i)))
                    }
                    AuthResult.Success(list)
                } else {
                    android.util.Log.e("SupabaseDB", "SELECT transactions error: HTTP ${response.code} $bodyStr")
                    AuthResult.Error("Failed to fetch transactions: HTTP ${response.code} $bodyStr")
                }
            }
        } catch (e: IOException) {
            android.util.Log.e("SupabaseDB", "SELECT transactions network error", e)
            AuthResult.Error("Network error while retrieving transactions.")
        } catch (e: Exception) {
            android.util.Log.e("SupabaseDB", "SELECT transactions exception", e)
            AuthResult.Error(e.localizedMessage ?: "An error occurred.")
        }
    }

    suspend fun createTransactionRemote(transaction: Transaction, accessToken: String?): AuthResult<Transaction> = withContext(Dispatchers.IO) {
        suspend fun executePost(jsonObj: JSONObject): Pair<Int, String> {
            val jsonPayload = jsonObj.toString()
            android.util.Log.d("SupabaseDB", "INSERT transaction payload: $jsonPayload")
            println("[SupabaseDB] INSERT transaction payload: $jsonPayload")

            val url = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/transactions"
            val token = if (!accessToken.isNullOrBlank()) accessToken else SupabaseClientProvider.SUPABASE_ANON_KEY
            val requestBody = jsonPayload.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .post(requestBody)
                .build()

            return client.newCall(request).execute().use { response ->
                val code = response.code
                val body = response.body?.string() ?: ""
                android.util.Log.d("SupabaseDB", "INSERT transaction response code=$code, body=$body")
                println("[SupabaseDB] INSERT transaction response code=$code, body=$body")
                Pair(code, body)
            }
        }

        try {
            var payload = transaction.toJsonObject(useTransactionDateColumn = true)
            var (code, bodyStr) = executePost(payload)

            if (code >= 400 && bodyStr.contains("transaction_date", ignoreCase = true)) {
                android.util.Log.w("SupabaseDB", "Retrying INSERT transaction with 'date' column...")
                payload = transaction.toJsonObject(useTransactionDateColumn = false)
                val res = executePost(payload)
                code = res.first
                bodyStr = res.second
            }

            if (code in 200..299) {
                val created = try {
                    val jsonArray = JSONArray(bodyStr)
                    if (jsonArray.length() > 0) Transaction.fromJsonObject(jsonArray.getJSONObject(0)) else transaction
                } catch (e: Exception) {
                    transaction
                }
                AuthResult.Success(created)
            } else {
                val errorObj = try { JSONObject(bodyStr) } catch (e: Exception) { null }
                val msg = errorObj?.optString("message")?.ifBlank { null }
                    ?: errorObj?.optString("hint")?.ifBlank { null }
                    ?: "HTTP $code: $bodyStr"
                android.util.Log.e("SupabaseDB", "INSERT transaction error: $msg")
                println("[SupabaseDB] INSERT transaction error: $msg")
                AuthResult.Error("Supabase INSERT error ($msg)")
            }
        } catch (e: IOException) {
            android.util.Log.e("SupabaseDB", "INSERT transaction network exception: ${e.message}", e)
            println("[SupabaseDB] INSERT transaction network exception: ${e.message}")
            AuthResult.Error("Network error: ${e.localizedMessage}")
        } catch (e: Exception) {
            android.util.Log.e("SupabaseDB", "INSERT transaction exception: ${e.message}", e)
            println("[SupabaseDB] INSERT transaction exception: ${e.message}")
            AuthResult.Error(e.localizedMessage ?: "Failed to create transaction.")
        }
    }

    suspend fun updateTransactionRemote(transaction: Transaction, accessToken: String?): AuthResult<Transaction> = withContext(Dispatchers.IO) {
        try {
            val id = transaction.id ?: return@withContext AuthResult.Error("Missing transaction ID")
            val jsonPayload = transaction.toJsonObject().toString()
            android.util.Log.d("SupabaseDB", "UPDATE transaction payload: $jsonPayload")
            println("[SupabaseDB] UPDATE transaction payload: $jsonPayload")

            val url = "${SupabaseClientProvider.SUPABASE_URL}/rest/v1/transactions?id=eq.$id"
            val token = if (!accessToken.isNullOrBlank()) accessToken else SupabaseClientProvider.SUPABASE_ANON_KEY
            val requestBody = jsonPayload.toRequestBody("application/json".toMediaType())

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
                android.util.Log.d("SupabaseDB", "UPDATE transaction response code=${response.code}, body=$bodyStr")
                println("[SupabaseDB] UPDATE transaction response code=${response.code}, body=$bodyStr")
                if (response.isSuccessful) {
                    AuthResult.Success(transaction)
                } else {
                    val errorObj = try { JSONObject(bodyStr) } catch (e: Exception) { null }
                    val msg = errorObj?.optString("message") ?: "Failed to update transaction."
                    android.util.Log.e("SupabaseDB", "UPDATE transaction error: $msg")
                    AuthResult.Error(msg)
                }
            }
        } catch (e: IOException) {
            android.util.Log.e("SupabaseDB", "UPDATE transaction network exception", e)
            AuthResult.Error("Network error while updating transaction.")
        } catch (e: Exception) {
            android.util.Log.e("SupabaseDB", "UPDATE transaction exception", e)
            AuthResult.Error(e.localizedMessage ?: "Failed to update transaction.")
        }
    }
}

