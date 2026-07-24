package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE userId = :userId AND isDeleted = 0 ORDER BY name ASC")
    fun getCustomersForUser(userId: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE userId = :userId AND isDeleted = 1 ORDER BY name ASC")
    fun getDeletedCustomersForUser(userId: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE userId = :userId AND isSynced = 0")
    suspend fun getUnsyncedCustomers(userId: String): List<CustomerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET isDeleted = 1, isSynced = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteCustomer(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE customers SET isDeleted = 0, isSynced = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun restoreCustomer(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun hardDeleteCustomer(id: String)

    @Query("DELETE FROM customers WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

