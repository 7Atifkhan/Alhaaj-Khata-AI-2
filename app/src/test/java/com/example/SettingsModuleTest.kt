package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.local.KhataDatabase
import com.example.data.remote.AuthRepository
import com.example.data.remote.CustomerRepository
import com.example.data.remote.ProfileRepository
import com.example.data.remote.SettingsRepository
import com.example.data.remote.TransactionRepository
import com.example.ui.viewmodels.SettingsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsModuleTest {

    private lateinit var viewModel: SettingsViewModel
    private lateinit var authRepository: AuthRepository
    private lateinit var database: KhataDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = KhataDatabase.getDatabase(context)
        authRepository = AuthRepository(context)

        val profileRepo = ProfileRepository()
        val settingsRepo = SettingsRepository()
        val transactionRepo = TransactionRepository(database.transactionDao(), database.customerDao())
        val customerRepo = CustomerRepository(database.customerDao())

        viewModel = SettingsViewModel(
            authRepository = authRepository,
            profileRepository = profileRepo,
            settingsRepository = settingsRepo,
            transactionRepository = transactionRepo,
            customerRepository = customerRepo,
            database = database,
            context = context
        )
    }

    @Test
    fun testBusinessProfileUpdate() {
        viewModel.updateProfile(
            businessName = "Alhaaj Traders Corp",
            ownerName = "Alhaaj Ahmad",
            phone = "+92 300 9876543",
            email = "ahmad@alhaaj.com",
            address = "Karkhano Market, Peshawar",
            currency = "PKR"
        )

        val state = viewModel.uiState.value
        assertEquals("Alhaaj Traders Corp", state.businessName)
        assertEquals("Alhaaj Ahmad", state.ownerName)
        assertEquals("+92 300 9876543", state.phone)
        assertEquals("ahmad@alhaaj.com", state.email)
        assertEquals("PKR", state.currency)
    }

    @Test
    fun testLogoUpdate() {
        viewModel.updateLogo("https://example.com/logo.png")
        assertEquals("https://example.com/logo.png", viewModel.uiState.value.logoUrl)
    }

    @Test
    fun testThemeAndLanguageSwitching() {
        viewModel.updateThemeMode("dark", {})
        assertEquals("dark", viewModel.uiState.value.themeMode)

        viewModel.updateLanguage("ur")
        assertEquals("ur", viewModel.uiState.value.language)

        viewModel.updateLanguage("en")
        assertEquals("en", viewModel.uiState.value.language)
    }

    @Test
    fun testSecuritySettings() {
        viewModel.togglePinLock(true, "1234")
        assertTrue(viewModel.uiState.value.pinLockEnabled)
        assertEquals("1234", viewModel.uiState.value.pinCode)

        viewModel.toggleBiometric(true)
        assertTrue(viewModel.uiState.value.biometricEnabled)

        viewModel.updateAutoLock(5)
        assertEquals(5, viewModel.uiState.value.autoLockMinutes)

        viewModel.updateSessionTimeout(60)
        assertEquals(60, viewModel.uiState.value.sessionTimeoutMinutes)

        assertNotNull(viewModel.uiState.value.activeDevices)
        assertTrue(viewModel.uiState.value.activeDevices.isNotEmpty())
    }

    @Test
    fun testBackupAndRestoreLocalExport() {
        viewModel.performCloudBackup()
        val file = viewModel.exportLocalBackup()
        assertNotNull(file)
        assertTrue(file?.exists() == true)

        viewModel.importLocalBackup("{\"businessName\": \"Imported Shop\"}")
        assertEquals("Imported Shop", viewModel.uiState.value.businessName)
    }

    @Test
    fun testStorageMetricsAndClearCache() {
        viewModel.calculateStorageMetrics()
        val stateBefore = viewModel.uiState.value
        assertTrue(stateBefore.databaseSizeKb >= 0)

        viewModel.clearCache()
        val stateAfter = viewModel.uiState.value
        assertTrue(stateAfter.cacheSizeKb >= 0)
    }
}
