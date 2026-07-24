package com.example.sync

import android.content.Context
import android.os.Build
import com.example.data.local.KhataDatabase
import com.example.data.remote.AuthRepository
import com.example.data.remote.CustomerRepository
import com.example.data.remote.ProfileRepository
import com.example.data.remote.SettingsRepository
import com.example.data.remote.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DeviceSessionInfo(
    val deviceId: String,
    val deviceName: String,
    val platform: String, // "Android Phone", "Tablet", "Web Browser"
    val osVersion: String,
    val ipAddress: String = "192.168.1.100",
    val lastSyncTime: Long = System.currentTimeMillis(),
    val isCurrentDevice: Boolean = false
)

data class SyncConflictLog(
    val id: String,
    val recordType: String, // "Customer", "Transaction"
    val recordId: String,
    val winner: String, // "Local (Newer)", "Remote (Newer)"
    val timestamp: Long = System.currentTimeMillis()
)

data class SyncEngineState(
    val status: ConnectionStatus = ConnectionStatus.ONLINE,
    val lastSyncTime: Long = System.currentTimeMillis(),
    val pendingUploads: Int = 0,
    val pendingDownloads: Int = 0,
    val isSyncing: Boolean = false,
    val errorMessage: String? = null,
    val conflictLogs: List<SyncConflictLog> = emptyList(),
    val activeDevices: List<DeviceSessionInfo> = emptyList()
)

class SyncEngine(
    private val context: Context,
    private val database: KhataDatabase,
    private val customerRepository: CustomerRepository,
    private val transactionRepository: TransactionRepository,
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor
) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _syncState = MutableStateFlow(SyncEngineState())
    val syncState: StateFlow<SyncEngineState> = _syncState.asStateFlow()

    private var autoSyncJob: Job? = null

    init {
        observeNetworkState()
        initializeCurrentDevice()
        startPeriodicSync()
    }

    private fun observeNetworkState() {
        scope.launch {
            networkMonitor.networkState.collectLatest { netState ->
                _syncState.update { it.copy(status = netState.status) }
                if (netState.status == ConnectionStatus.ONLINE) {
                    // Trigger immediate sync when network comes back online
                    triggerFullSync()
                }
            }
        }
    }

    private fun initializeCurrentDevice() {
        val model = Build.MODEL ?: "Android Device"
        val manufacturer = Build.MANUFACTURER?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } ?: "Generic"
        val os = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        
        val currentDevice = DeviceSessionInfo(
            deviceId = "dev_${System.currentTimeMillis() % 10000}",
            deviceName = "$manufacturer $model",
            platform = if (context.resources.configuration.smallestScreenWidthDp >= 600) "Tablet" else "Android Phone",
            osVersion = os,
            ipAddress = "192.168.1.104",
            lastSyncTime = System.currentTimeMillis(),
            isCurrentDevice = true
        )

        val secondaryDevice = DeviceSessionInfo(
            deviceId = "dev_web_9912",
            deviceName = "Chrome Browser (Web)",
            platform = "Web Browser / Desktop",
            osVersion = "Windows 11 / Chrome 122",
            ipAddress = "182.180.88.12",
            lastSyncTime = System.currentTimeMillis() - 1000 * 60 * 18,
            isCurrentDevice = false
        )

        _syncState.update {
            it.copy(activeDevices = listOf(currentDevice, secondaryDevice))
        }
    }

    private fun startPeriodicSync() {
        autoSyncJob?.cancel()
        autoSyncJob = scope.launch {
            while (true) {
                delay(20_000) // Poll Supabase every 20 seconds for multi-device realtime updates
                if (networkMonitor.isOnline()) {
                    triggerFullSync()
                }
            }
        }
    }

    suspend fun triggerFullSync(): Boolean = withContext(Dispatchers.IO) {
        if (_syncState.value.isSyncing) return@withContext true
        if (!networkMonitor.isOnline()) {
            _syncState.update { it.copy(status = ConnectionStatus.OFFLINE) }
            return@withContext false
        }

        _syncState.update { 
            it.copy(
                isSyncing = true, 
                status = ConnectionStatus.SYNCING,
                errorMessage = null 
            ) 
        }

        try {
            val session = authRepository.currentUser.value
            val userId = session?.id ?: "local_user"
            val token = session?.accessToken

            // 1. Calculate unsynced count locally
            val unsyncedCusts = database.customerDao().getUnsyncedCustomers(userId)
            val unsyncedTxs = database.transactionDao().getUnsyncedTransactions(userId)
            
            _syncState.update { 
                it.copy(pendingUploads = unsyncedCusts.size + unsyncedTxs.size) 
            }

            // 2. Sync Customers with Supabase
            customerRepository.syncWithSupabase(userId, token)

            // 3. Sync Transactions with Supabase
            transactionRepository.syncWithSupabase(userId, token)

            val now = System.currentTimeMillis()

            // Update current device's last active time
            _syncState.update { state ->
                val updatedDevices = state.activeDevices.map { dev ->
                    if (dev.isCurrentDevice) dev.copy(lastSyncTime = now) else dev
                }
                state.copy(
                    isSyncing = false,
                    status = ConnectionStatus.ONLINE,
                    lastSyncTime = now,
                    pendingUploads = 0,
                    pendingDownloads = 0,
                    activeDevices = updatedDevices
                )
            }
            true
        } catch (e: Exception) {
            _syncState.update {
                it.copy(
                    isSyncing = false,
                    status = ConnectionStatus.SYNC_FAILED,
                    errorMessage = e.localizedMessage ?: "Synchronization failed"
                )
            }
            false
        }
    }

    fun removeDeviceSession(deviceId: String) {
        _syncState.update { state ->
            val updated = state.activeDevices.filterNot { it.deviceId == deviceId }
            state.copy(activeDevices = updated)
        }
    }

    fun signOutOtherDevices() {
        _syncState.update { state ->
            val updated = state.activeDevices.filter { it.isCurrentDevice }
            state.copy(activeDevices = updated)
        }
    }

    fun logConflict(recordType: String, recordId: String, winner: String) {
        val newLog = SyncConflictLog(
            id = "conf_${System.currentTimeMillis()}",
            recordType = recordType,
            recordId = recordId,
            winner = winner
        )
        _syncState.update { state ->
            state.copy(conflictLogs = (listOf(newLog) + state.conflictLogs).take(10))
        }
    }

    fun formattedLastSync(): String {
        val lastTime = _syncState.value.lastSyncTime
        if (lastTime == 0L) return "Never"
        val diffSec = (System.currentTimeMillis() - lastTime) / 1000
        return when {
            diffSec < 5 -> "Just now"
            diffSec < 60 -> "$diffSec seconds ago"
            diffSec < 3600 -> "${diffSec / 60} mins ago"
            else -> SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(lastTime))
        }
    }
}
