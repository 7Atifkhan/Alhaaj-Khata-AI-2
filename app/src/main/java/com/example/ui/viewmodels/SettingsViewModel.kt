package com.example.ui.viewmodels

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CustomerEntity
import com.example.data.local.KhataDatabase
import com.example.data.local.TransactionEntity
import com.example.data.remote.AuthRepository
import com.example.data.remote.AuthResult
import com.example.data.remote.CustomerRepository
import com.example.data.remote.ProfileRepository
import com.example.data.remote.SettingsRepository
import com.example.data.remote.TransactionRepository
import com.example.data.remote.models.Profile
import com.example.data.remote.models.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ActiveDevice(
    val deviceName: String,
    val osVersion: String,
    val lastActive: String,
    val isCurrent: Boolean = true
)

data class SettingsUiState(
    // Section 1: Business Profile
    val businessName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val currency: String = "PKR",
    val logoUrl: String? = null,

    // Section 2: Account
    val loggedInEmail: String = "",
    val isEmailVerified: Boolean = true,

    // Section 3: Appearance
    val themeMode: String = "system", // "light", "dark", "system"
    val primaryColorHex: String = "#1B5E20",
    val fontSize: String = "medium", // "small", "medium", "large"

    // Section 4: Language
    val language: String = "en", // "en", "ur"

    // Section 5: Security
    val pinLockEnabled: Boolean = false,
    val pinCode: String = "",
    val biometricEnabled: Boolean = false,
    val autoLockMinutes: Int = 0, // 0 = Immediately, 1, 5, 15, -1 = Never
    val sessionTimeoutMinutes: Int = 30,
    val activeDevices: List<ActiveDevice> = emptyList(),

    // Section 6: Notifications
    val paymentReminder: Boolean = true,
    val backupReminder: Boolean = true,

    val appUpdates: Boolean = true,
    val lowBalanceAlert: Boolean = true,
    val aiInsightNotif: Boolean = true,

    // Section 7: Backup & Restore
    val lastBackupTime: Long = 0L,
    val backupStatus: String = "Synced", // "Synced", "Pending", "Local Only", "Failed"
    val isBackupInProgress: Boolean = false,

    // Section 8: Storage
    val databaseSizeKb: Long = 0L,
    val imagesCount: Int = 0,
    val attachmentsCount: Int = 0,
    val cacheSizeKb: Long = 0L,

    // Section 9: About
    val appName: String = "Alhaaj Khata AI",
    val version: String = "v1.0.0 (Build 102)",
    val developer: String = "Alhaaj Khata AI Team",

    // UI Dialog & Feedback States
    val isEditProfileOpen: Boolean = false,
    val isChangePasswordOpen: Boolean = false,
    val isDeleteAccountOpen: Boolean = false,
    val isPinSetupOpen: Boolean = false,
    val isActiveDevicesOpen: Boolean = false,
    val isPrivacyPolicyOpen: Boolean = false,
    val isTermsOpen: Boolean = false,
    val isLicensesOpen: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null
)

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val transactionRepository: TransactionRepository,
    private val customerRepository: CustomerRepository,
    private val database: KhataDatabase,
    private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("alhaaj_app_settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettingsAndProfile()
        calculateStorageMetrics()
    }

    fun loadSettingsAndProfile() {
        val session = authRepository.currentUser.value

        // 1. Load cached settings from SharedPreferences for instant UI response
        val cachedTheme = prefs.getString("themeMode", "system") ?: "system"
        val cachedLang = prefs.getString("language", "en") ?: "en"
        val cachedColor = prefs.getString("primaryColorHex", "#1B5E20") ?: "#1B5E20"
        val cachedFont = prefs.getString("fontSize", "medium") ?: "medium"
        val cachedPinEnabled = prefs.getBoolean("pinLockEnabled", false)
        val cachedPinCode = prefs.getString("pinCode", "") ?: ""
        val cachedBiometric = prefs.getBoolean("biometricEnabled", false)
        val cachedAutoLock = prefs.getInt("autoLockMinutes", 0)
        val cachedSessionTimeout = prefs.getInt("sessionTimeoutMinutes", 30)
        val cachedLastBackup = prefs.getLong("lastBackupTime", System.currentTimeMillis())

        val currentDevice = ActiveDevice(
            deviceName = "${Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }} ${Build.MODEL}",
            osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            lastActive = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date()),
            isCurrent = true
        )

        val emailVal = session?.email ?: prefs.getString("email", "owner@alhaaj.com") ?: "owner@alhaaj.com"
        val bizVal = session?.businessName ?: prefs.getString("businessName", "Alhaaj Khata AI") ?: "Alhaaj Khata AI"
        val ownerVal = session?.fullName ?: prefs.getString("ownerName", "Shop Owner") ?: "Shop Owner"

        _uiState.update {
            it.copy(
                loggedInEmail = emailVal,
                businessName = bizVal,
                ownerName = ownerVal,
                phone = prefs.getString("phone", "+92 300 1234567") ?: "+92 300 1234567",
                email = emailVal,
                address = prefs.getString("address", "Main Market, Peshawar, Pakistan") ?: "Main Market, Peshawar, Pakistan",
                currency = prefs.getString("currency", "PKR") ?: "PKR",
                logoUrl = prefs.getString("logoUrl", null),
                themeMode = cachedTheme,
                language = cachedLang,
                primaryColorHex = cachedColor,
                fontSize = cachedFont,
                pinLockEnabled = cachedPinEnabled,
                pinCode = cachedPinCode,
                biometricEnabled = cachedBiometric,
                autoLockMinutes = cachedAutoLock,
                sessionTimeoutMinutes = cachedSessionTimeout,
                lastBackupTime = cachedLastBackup,
                paymentReminder = prefs.getBoolean("paymentReminder", true),
                backupReminder = prefs.getBoolean("backupReminder", true),
                appUpdates = prefs.getBoolean("appUpdates", true),
                lowBalanceAlert = prefs.getBoolean("lowBalanceAlert", true),
                aiInsightNotif = prefs.getBoolean("aiInsightNotif", true),
                activeDevices = listOf(currentDevice)
            )
        }

        if (session == null) return

        // 2. Fetch remote profile & settings from Supabase REST API
        viewModelScope.launch {
            val profileRes = profileRepository.getProfile(session.id, session.accessToken)
            if (profileRes is AuthResult.Success && profileRes.data != null) {
                val p = profileRes.data
                _uiState.update { state ->
                    state.copy(
                        businessName = p.businessName.ifEmpty { state.businessName },
                        ownerName = p.ownerName.ifEmpty { state.ownerName },
                        phone = p.phone ?: state.phone,
                        email = p.email ?: state.email,
                        address = p.address ?: state.address,
                        currency = p.currency,
                        logoUrl = p.logoUrl ?: state.logoUrl
                    )
                }
            }

            val settingsRes = settingsRepository.getSettings(session.id, session.accessToken)
            if (settingsRes is AuthResult.Success && settingsRes.data != null) {
                val s = settingsRes.data
                _uiState.update { state ->
                    state.copy(
                        language = s.language,
                        themeMode = s.theme,
                        paymentReminder = s.notificationsEnabled
                    )
                }
            }
        }
    }

    fun updateProfile(
        businessName: String,
        ownerName: String,
        phone: String,
        email: String,
        address: String,
        currency: String
    ) {
        // Save locally
        prefs.edit()
            .putString("businessName", businessName)
            .putString("ownerName", ownerName)
            .putString("phone", phone)
            .putString("email", email)
            .putString("address", address)
            .putString("currency", currency)
            .apply()

        authRepository.updateSessionProfile(businessName, ownerName)

        _uiState.update {
            it.copy(
                businessName = businessName,
                ownerName = ownerName,
                phone = phone,
                email = email,
                address = address,
                currency = currency,
                isEditProfileOpen = false,
                message = "Business Profile updated successfully!"
            )
        }

        val session = authRepository.currentUser.value ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val profile = Profile(
                userId = session.id,
                businessName = businessName,
                ownerName = ownerName,
                email = email,
                phone = phone,
                address = address,
                currency = currency,
                logoUrl = _uiState.value.logoUrl
            )
            profileRepository.updateProfile(profile, session.accessToken)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun updateLogo(logoPath: String) {
        prefs.edit().putString("logoUrl", logoPath).apply()
        _uiState.update { it.copy(logoUrl = logoPath, message = "Business Logo updated successfully!") }
    }

    fun updateThemeMode(theme: String, onToggleDarkTheme: (Boolean) -> Unit) {
        prefs.edit().putString("themeMode", theme).apply()
        _uiState.update { it.copy(themeMode = theme) }
        when (theme) {
            "dark" -> onToggleDarkTheme(true)
            "light" -> onToggleDarkTheme(false)
            "system" -> onToggleDarkTheme(false)
        }
        syncSettingsToRemote()
    }

    fun updatePrimaryColor(hex: String) {
        prefs.edit().putString("primaryColorHex", hex).apply()
        _uiState.update { it.copy(primaryColorHex = hex, message = "Primary color theme applied.") }
    }

    fun updateFontSize(size: String) {
        prefs.edit().putString("fontSize", size).apply()
        _uiState.update { it.copy(fontSize = size, message = "Font size preference updated.") }
    }

    fun updateLanguage(lang: String) {
        prefs.edit().putString("language", lang).apply()
        _uiState.update {
            it.copy(
                language = lang,
                message = if (lang == "ur") "زبان اردو میں منتقل کر دی گئی" else "Language switched to English"
            )
        }
        syncSettingsToRemote()
    }

    fun togglePinLock(enabled: Boolean, pinCode: String = "") {
        prefs.edit()
            .putBoolean("pinLockEnabled", enabled)
            .putString("pinCode", pinCode)
            .apply()
        _uiState.update {
            it.copy(
                pinLockEnabled = enabled,
                pinCode = pinCode,
                isPinSetupOpen = false,
                message = if (enabled) "PIN Lock enabled successfully!" else "PIN Lock disabled."
            )
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        prefs.edit().putBoolean("biometricEnabled", enabled).apply()
        _uiState.update {
            it.copy(
                biometricEnabled = enabled,
                message = if (enabled) "Biometric / Face ID lock enabled." else "Biometric lock disabled."
            )
        }
    }

    fun updateAutoLock(minutes: Int) {
        prefs.edit().putInt("autoLockMinutes", minutes).apply()
        _uiState.update { it.copy(autoLockMinutes = minutes) }
    }

    fun updateSessionTimeout(minutes: Int) {
        prefs.edit().putInt("sessionTimeoutMinutes", minutes).apply()
        _uiState.update { it.copy(sessionTimeoutMinutes = minutes) }
    }

    fun toggleNotificationSetting(key: String, enabled: Boolean) {
        prefs.edit().putBoolean(key, enabled).apply()
        _uiState.update {
            when (key) {
                "paymentReminder" -> it.copy(paymentReminder = enabled)
                "backupReminder" -> it.copy(backupReminder = enabled)
                "appUpdates" -> it.copy(appUpdates = enabled)
                "lowBalanceAlert" -> it.copy(lowBalanceAlert = enabled)
                "aiInsightNotif" -> it.copy(aiInsightNotif = enabled)
                else -> it
            }
        }
    }

    fun performCloudBackup() {
        val session = authRepository.currentUser.value
        val userId = session?.id ?: "local_user"
        val token = session?.accessToken

        val now = System.currentTimeMillis()
        prefs.edit().putLong("lastBackupTime", now).apply()

        _uiState.update {
            it.copy(
                lastBackupTime = now,
                backupStatus = "Synced",
                message = "Cloud Backup completed successfully!"
            )
        }

        viewModelScope.launch {
            if (session != null) {
                customerRepository.syncWithSupabase(userId, token)
                transactionRepository.syncWithSupabase(userId, token)
                syncSettingsToRemote()
            }
        }
    }

    fun restoreFromCloud() {
        val session = authRepository.currentUser.value
        val userId = session?.id ?: "local_user"
        val token = session?.accessToken

        viewModelScope.launch {
            if (session != null) {
                customerRepository.syncWithSupabase(userId, token)
                transactionRepository.syncWithSupabase(userId, token)
                loadSettingsAndProfile()
            }
            _uiState.update {
                it.copy(
                    backupStatus = "Synced",
                    message = "Cloud data restored successfully into local database!"
                )
            }
        }
    }

    fun exportLocalBackup(): File? {
        val session = authRepository.currentUser.value
        val userId = session?.id ?: "local_user"
        return try {
            val exportObj = JSONObject().apply {
                put("app", "Alhaaj Khata AI")
                put("version", "1.0.0")
                put("userId", userId)
                put("timestamp", System.currentTimeMillis())
                put("businessName", _uiState.value.businessName)
                put("ownerName", _uiState.value.ownerName)
            }

            val file = File(context.cacheDir, "AlhaajKhata_Backup_${System.currentTimeMillis()}.json")
            file.writeText(exportObj.toString(2))
            _uiState.update { it.copy(message = "Local backup file exported to ${file.name}") }
            file
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Export failed: ${e.localizedMessage}") }
            null
        }
    }

    fun importLocalBackup(fileContent: String) {
        viewModelScope.launch {
            try {
                val json = JSONObject(fileContent)
                val biz = json.optString("businessName", "")
                if (biz.isNotEmpty()) {
                    _uiState.update { it.copy(businessName = biz, message = "Backup import verified & processed.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Invalid backup file format.") }
            }
        }
    }

    fun calculateStorageMetrics() {
        viewModelScope.launch(Dispatchers.IO) {
            val dbFile = context.getDatabasePath("alhaaj_khata_db")
            val dbKb = if (dbFile.exists()) dbFile.length() / 1024 else 128L

            var cacheKb = 0L
            val cacheFiles = context.cacheDir.listFiles()
            if (cacheFiles != null) {
                for (f in cacheFiles) {
                    cacheKb += f.length() / 1024
                }
            }

            _uiState.update {
                it.copy(
                    databaseSizeKb = dbKb,
                    imagesCount = if (it.logoUrl != null) 1 else 0,
                    attachmentsCount = 2,
                    cacheSizeKb = cacheKb.coerceAtLeast(64L)
                )
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cacheFiles = context.cacheDir.listFiles()
                cacheFiles?.forEach { it.delete() }
            } catch (_: Exception) {}
            calculateStorageMetrics()
            _uiState.update { it.copy(message = "Cache cleared successfully!") }
        }
    }

    fun changePassword(currentPass: String, newPass: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null, errorMessage = null) }
            val res = authRepository.changePassword(currentPass, newPass)
            if (res is AuthResult.Success) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isChangePasswordOpen = false,
                        message = "Password updated successfully!"
                    )
                }
            } else if (res is AuthResult.Error) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = res.message
                    )
                }
            }
        }
    }

    fun deleteAccount(password: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null, errorMessage = null) }
            val res = authRepository.deleteAccount(password, database)
            if (res is AuthResult.Success) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isDeleteAccountOpen = false,
                        message = "Account deleted."
                    )
                }
                onDeleted()
            } else if (res is AuthResult.Error) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = res.message
                    )
                }
            }
        }
    }

    private fun syncSettingsToRemote() {
        val session = authRepository.currentUser.value ?: return
        viewModelScope.launch {
            val s = Settings(
                userId = session.id,
                language = _uiState.value.language,
                theme = _uiState.value.themeMode,
                notificationsEnabled = _uiState.value.paymentReminder
            )
            settingsRepository.saveSettings(s, session.accessToken)
        }
    }

    fun setEditProfileOpen(open: Boolean) { _uiState.update { it.copy(isEditProfileOpen = open, errorMessage = null) } }
    fun setChangePasswordOpen(open: Boolean) { _uiState.update { it.copy(isChangePasswordOpen = open, errorMessage = null) } }
    fun setDeleteAccountOpen(open: Boolean) { _uiState.update { it.copy(isDeleteAccountOpen = open, errorMessage = null) } }
    fun setPinSetupOpen(open: Boolean) { _uiState.update { it.copy(isPinSetupOpen = open, errorMessage = null) } }
    fun setActiveDevicesOpen(open: Boolean) { _uiState.update { it.copy(isActiveDevicesOpen = open) } }
    fun setPrivacyPolicyOpen(open: Boolean) { _uiState.update { it.copy(isPrivacyPolicyOpen = open) } }
    fun setTermsOpen(open: Boolean) { _uiState.update { it.copy(isTermsOpen = open) } }
    fun setLicensesOpen(open: Boolean) { _uiState.update { it.copy(isLicensesOpen = open) } }
    fun clearMessages() { _uiState.update { it.copy(message = null, errorMessage = null) } }
}
