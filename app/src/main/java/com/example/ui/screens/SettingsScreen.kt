package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.UserSession
import com.example.ui.viewmodels.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    currentUser: UserSession? = null,
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: (Boolean) -> Unit = {},
    onNavigateToActiveDevices: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { br -> br.readText() }
                if (content != null) {
                    viewModel.importLocalBackup(content)
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (state.language == "ur") "سیٹنگز اور ترتیب" else "Settings & Configuration",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (state.language == "ur") "پروفائل، سیکیورٹی اور ڈیجیٹل بیک اپ کا انتظام کریں" else "Manage business profile, security & digital backups",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ================= SECTION 1: Business Profile =================
            item {
                SectionCard(
                    title = if (state.language == "ur") "کاروباری پروفائل (Business Profile)" else "Business Profile",
                    icon = Icons.Default.Store
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!state.logoUrl.isNullOrEmpty()) {
                                Text(
                                    text = "🖼️",
                                    fontSize = 24.sp
                                )
                            } else {
                                Text(
                                    text = state.businessName.take(2).uppercase().ifEmpty { "AK" },
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.businessName.ifEmpty { "Alhaaj Store" },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Owner: ${state.ownerName.ifEmpty { "Shopkeeper" }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${state.phone} | ${state.currency}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = state.address,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.setEditProfileOpen(true) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_profile_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (state.language == "ur") "ترمیم کریں" else "Edit Profile")
                        }

                        var isLogoDialogOpen by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { isLogoDialogOpen = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("change_logo_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (state.language == "ur") "لوگو تبدیل کریں" else "Change Logo")
                        }

                        if (isLogoDialogOpen) {
                            var logoInput by remember { mutableStateOf(state.logoUrl ?: "") }
                            AlertDialog(
                                onDismissRequest = { isLogoDialogOpen = false },
                                title = { Text("Update Business Logo") },
                                text = {
                                    Column {
                                        Text("Enter Image URL or Path for Business Logo:")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = logoInput,
                                            onValueChange = { logoInput = it },
                                            label = { Text("Logo URL") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(onClick = {
                                        viewModel.updateLogo(logoInput)
                                        isLogoDialogOpen = false
                                    }) {
                                        Text("Save Logo")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { isLogoDialogOpen = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ================= SECTION 2: Account =================
            item {
                SectionCard(
                    title = if (state.language == "ur") "اکاؤنٹ (Account)" else "Account & Security",
                    icon = Icons.Default.Person
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Logged in as",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = state.loggedInEmail.ifEmpty { "user@alhaaj.com" },
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Surface(
                            color = if (state.isEmailVerified) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (state.isEmailVerified) Icons.Default.VerifiedUser else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (state.isEmailVerified) Color(0xFF2E7D32) else Color(0xFFE65100),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (state.isEmailVerified) "Verified" else "Unverified",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (state.isEmailVerified) Color(0xFF2E7D32) else Color(0xFFE65100)
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.setChangePasswordOpen(true) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("change_password_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change Pass")
                        }

                        Button(
                            onClick = onLogout,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_logout_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Logout")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = { viewModel.setDeleteAccountOpen(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_account_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Account & Business Data", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // ================= SECTION 3: Appearance =================
            item {
                SectionCard(
                    title = if (state.language == "ur") "طرز و انداز (Appearance)" else "Appearance & Styling",
                    icon = Icons.Default.Palette
                ) {
                    Text("Theme Selection", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("light" to "Light", "dark" to "Dark", "system" to "System").forEach { (mode, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { viewModel.updateThemeMode(mode, onToggleDarkTheme) }
                            ) {
                                RadioButton(
                                    selected = state.themeMode == mode,
                                    onClick = { viewModel.updateThemeMode(mode, onToggleDarkTheme) }
                                )
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Text("Primary Accent Color", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        listOf(
                            "#1B5E20" to "Emerald",
                            "#0D47A1" to "Classic Blue",
                            "#4A148C" to "Royal Purple",
                            "#E65100" to "Sunset Orange"
                        ).forEach { (hex, name) ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (state.primaryColorHex == hex) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.updatePrimaryColor(hex) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.primaryColorHex == hex) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = name, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Text("Font Size", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("small" to "Small", "medium" to "Medium", "large" to "Large").forEach { (size, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { viewModel.updateFontSize(size) }
                            ) {
                                RadioButton(
                                    selected = state.fontSize == size,
                                    onClick = { viewModel.updateFontSize(size) }
                                )
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // ================= SECTION 4: Language =================
            item {
                SectionCard(
                    title = if (state.language == "ur") "زبان (Language)" else "Language Preferences",
                    icon = Icons.Default.Language
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.updateLanguage("en") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lang_en_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = if (state.language == "en") ButtonDefaults.outlinedButtonBorder else null,
                            colors = if (state.language == "en") ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("English", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        OutlinedButton(
                            onClick = { viewModel.updateLanguage("ur") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lang_ur_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = if (state.language == "ur") ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("اردو (Urdu)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ================= SECTION 5: Security =================
            item {
                SectionCard(
                    title = if (state.language == "ur") "سیکیورٹی (Security)" else "Security & App Lock",
                    icon = Icons.Default.Security
                ) {
                    SwitchRow(
                        title = "PIN Lock Protection",
                        subtitle = "Require 4-digit PIN upon app open",
                        checked = state.pinLockEnabled,
                        onCheckedChange = { checked ->
                            if (checked) viewModel.setPinSetupOpen(true)
                            else viewModel.togglePinLock(false)
                        }
                    )

                    if (state.pinLockEnabled) {
                        TextButton(onClick = { viewModel.setPinSetupOpen(true) }) {
                            Text("Change PIN Code")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SwitchRow(
                        title = "Fingerprint / Face ID",
                        subtitle = "Enable biometric authentication",
                        checked = state.biometricEnabled,
                        onCheckedChange = { viewModel.toggleBiometric(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Auto Lock", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("Lock app when inactive", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = when (state.autoLockMinutes) {
                                0 -> "Immediately"
                                1 -> "1 minute"
                                5 -> "5 minutes"
                                15 -> "15 minutes"
                                else -> "Never"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.setActiveDevicesOpen(true)
                            onNavigateToActiveDevices()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("show_active_devices_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Show Active Devices & Sessions")
                    }
                }
            }

            // ================= SECTION 6: Notifications =================
            item {
                SectionCard(
                    title = if (state.language == "ur") "نوٹیفکیشنز (Notifications)" else "Notifications & Alerts",
                    icon = Icons.Default.NotificationsActive
                ) {
                    SwitchRow(
                        title = "Payment Reminders",
                        subtitle = "Alerts for customer due dates",
                        checked = state.paymentReminder,
                        onCheckedChange = { viewModel.toggleNotificationSetting("paymentReminder", it) }
                    )
                    SwitchRow(
                        title = "Backup Reminders",
                        subtitle = "Weekly cloud sync reminder",
                        checked = state.backupReminder,
                        onCheckedChange = { viewModel.toggleNotificationSetting("backupReminder", it) }
                    )
                    SwitchRow(
                        title = "App Updates",
                        subtitle = "New feature & release alerts",
                        checked = state.appUpdates,
                        onCheckedChange = { viewModel.toggleNotificationSetting("appUpdates", it) }
                    )
                    SwitchRow(
                        title = "Low Balance Alert",
                        subtitle = "Warning when credit threshold is exceeded",
                        checked = state.lowBalanceAlert,
                        onCheckedChange = { viewModel.toggleNotificationSetting("lowBalanceAlert", it) }
                    )
                    SwitchRow(
                        title = "AI Insight Notifications",
                        subtitle = "Weekly summary of AI ledger insights",
                        checked = state.aiInsightNotif,
                        onCheckedChange = { viewModel.toggleNotificationSetting("aiInsightNotif", it) }
                    )
                }
            }

            // ================= SECTION 7: Backup & Restore =================
            item {
                SectionCard(
                    title = if (state.language == "ur") "بیک اپ اور بحالی (Backup & Restore)" else "Backup & Restore",
                    icon = Icons.Default.Backup
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Last Backup Time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (state.lastBackupTime > 0) SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(state.lastBackupTime)) else "Never",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = state.backupStatus,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.performCloudBackup() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cloud_backup_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cloud Backup")
                        }

                        OutlinedButton(
                            onClick = { viewModel.restoreFromCloud() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cloud_restore_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore Cloud")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { viewModel.exportLocalBackup() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export Local JSON")
                        }

                        TextButton(
                            onClick = { importLauncher.launch("application/json") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import Local JSON")
                        }
                    }
                }
            }

            // ================= SECTION 8: Storage =================
            item {
                SectionCard(
                    title = if (state.language == "ur") "سٹوریج (Storage)" else "Storage & Cache",
                    icon = Icons.Default.Storage
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StorageItem("Database", "${state.databaseSizeKb} KB")
                        StorageItem("Images", "${state.imagesCount} files")
                        StorageItem("Attachments", "${state.attachmentsCount} files")
                        StorageItem("Cache", "${state.cacheSizeKb} KB")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { viewModel.clearCache() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_cache_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Clear Cache & Temporary Files")
                    }
                }
            }

            // ================= SECTION 9: About =================
            item {
                SectionCard(
                    title = if (state.language == "ur") "متعلق (About)" else "About Application",
                    icon = Icons.Default.Info
                ) {
                    Text(state.appName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Version: ${state.version}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Developer: ${state.developer}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { viewModel.setPrivacyPolicyOpen(true) }) {
                            Text("Privacy Policy")
                        }
                        TextButton(onClick = { viewModel.setTermsOpen(true) }) {
                            Text("Terms of Service")
                        }
                        TextButton(onClick = { viewModel.setLicensesOpen(true) }) {
                            Text("Licenses")
                        }
                    }
                }
            }
        }

        // ================= DIALOGS =================

        // Edit Profile Dialog
        if (state.isEditProfileOpen) {
            var bName by remember { mutableStateOf(state.businessName) }
            var oName by remember { mutableStateOf(state.ownerName) }
            var ph by remember { mutableStateOf(state.phone) }
            var em by remember { mutableStateOf(state.email) }
            var addr by remember { mutableStateOf(state.address) }
            var curr by remember { mutableStateOf(state.currency) }

            AlertDialog(
                onDismissRequest = { viewModel.setEditProfileOpen(false) },
                title = { Text("Edit Business Profile") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bName,
                            onValueChange = { bName = it },
                            label = { Text("Business Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = oName,
                            onValueChange = { oName = it },
                            label = { Text("Owner Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ph,
                            onValueChange = { ph = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = em,
                            onValueChange = { em = it },
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = addr,
                            onValueChange = { addr = it },
                            label = { Text("Business Address") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = curr,
                            onValueChange = { curr = it },
                            label = { Text("Currency Code (e.g. PKR, USD)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateProfile(bName, oName, ph, em, addr, curr)
                        }
                    ) {
                        Text("Save Profile")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setEditProfileOpen(false) }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Change Password Dialog
        if (state.isChangePasswordOpen) {
            var currentPass by remember { mutableStateOf("") }
            var newPass by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { viewModel.setChangePasswordOpen(false) },
                title = { Text("Change Password") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = currentPass,
                            onValueChange = { currentPass = it },
                            label = { Text("Current Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPass,
                            onValueChange = { newPass = it },
                            label = { Text("New Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.changePassword(currentPass, newPass) }) {
                        Text("Update Password")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setChangePasswordOpen(false) }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete Account Dialog
        if (state.isDeleteAccountOpen) {
            var passConfirm by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { viewModel.setDeleteAccountOpen(false) },
                title = { Text("Delete Account & All Data", color = MaterialTheme.colorScheme.error) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Warning: This action is permanent! It will delete your Business Profile, Customers, Transactions, Settings, and Storage files.")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Please enter your account password to confirm deletion:")
                        OutlinedTextField(
                            value = passConfirm,
                            onValueChange = { passConfirm = it },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteAccount(passConfirm) {
                                onLogout()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Permanently Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setDeleteAccountOpen(false) }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // PIN Setup Dialog
        if (state.isPinSetupOpen) {
            var pinInput by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { viewModel.setPinSetupOpen(false) },
                title = { Text("Setup Security PIN") },
                text = {
                    Column {
                        Text("Enter a 4-digit PIN for app locking:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { if (it.length <= 4) pinInput = it },
                            label = { Text("4-Digit PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (pinInput.length == 4) {
                                viewModel.togglePinLock(true, pinInput)
                            }
                        }
                    ) {
                        Text("Enable PIN Lock")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setPinSetupOpen(false) }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Active Devices Dialog
        if (state.isActiveDevicesOpen) {
            AlertDialog(
                onDismissRequest = { viewModel.setActiveDevicesOpen(false) },
                title = { Text("Active Devices & Sessions") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.activeDevices.forEach { dev ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(dev.deviceName, fontWeight = FontWeight.Bold)
                                    Text(dev.osVersion, style = MaterialTheme.typography.bodySmall)
                                    Text("Active: ${dev.lastActive}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.setActiveDevicesOpen(false) }) {
                        Text("Close")
                    }
                }
            )
        }

        // Privacy Policy Dialog
        if (state.isPrivacyPolicyOpen) {
            AlertDialog(
                onDismissRequest = { viewModel.setPrivacyPolicyOpen(false) },
                title = { Text("Privacy Policy") },
                text = {
                    Text("Alhaaj Khata AI respects your privacy. All digital ledger entries and customer transaction data belong strictly to your authenticated account and are protected using local encryption and secure Supabase row-level authorization.")
                },
                confirmButton = { TextButton(onClick = { viewModel.setPrivacyPolicyOpen(false) }) { Text("Close") } }
            )
        }

        // Terms Dialog
        if (state.isTermsOpen) {
            AlertDialog(
                onDismissRequest = { viewModel.setTermsOpen(false) },
                title = { Text("Terms of Service") },
                text = {
                    Text("By using Alhaaj Khata AI, you agree to manage credit ledgers responsibly. Local backups and cloud synchronization are maintained for shopkeeper convenience.")
                },
                confirmButton = { TextButton(onClick = { viewModel.setTermsOpen(false) }) { Text("Close") } }
            )
        }

        // Licenses Dialog
        if (state.isLicensesOpen) {
            AlertDialog(
                onDismissRequest = { viewModel.setLicensesOpen(false) },
                title = { Text("Open Source Licenses") },
                text = {
                    Text("This app utilizes Jetpack Compose, Room Database, Kotlin Coroutines, Supabase REST Client, OkHttp, Coil Image Loader, and Material Design 3 Components.")
                },
                confirmButton = { TextButton(onClick = { viewModel.setLicensesOpen(false) }) { Text("Close") } }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            content()
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun StorageItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
