package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.local.CustomerEntity
import com.example.ui.components.CustomerAvatar
import com.example.ui.util.PdfStatementGenerator
import com.example.ui.viewmodels.CustomerFilter
import com.example.ui.viewmodels.CustomersViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(viewModel: CustomersViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val customers by viewModel.customers.collectAsState()
    val deletedCustomers by viewModel.deletedCustomers.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    var showAddEditSheet by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<CustomerEntity?>(null) }
    var selectedCustomerForDetail by remember { mutableStateOf<CustomerEntity?>(null) }
    var showTrashDialog by remember { mutableStateOf(false) }
    var customerToDeleteConfirm by remember { mutableStateOf<CustomerEntity?>(null) }
    var deleteWarningMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    customerToEdit = null
                    showAddEditSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Customer"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Customer Directory",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    )
                    Text(
                        text = "${customers.size} Active Accounts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sync Status
                    IconButton(onClick = { viewModel.syncWithCloud() }) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Synced",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Trash / Restoration Icon
                    IconButton(
                        onClick = { showTrashDialog = true },
                        modifier = Modifier.testTag("trash_bin_button")
                    ) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Trash",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (deletedCustomers.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary Totals Card
            val totalOwesMe = customers.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }
            val totalIOwe = customers.filter { it.currentBalance < 0 }.sumOf { Math.abs(it.currentBalance) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "You Will Get",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "PKR ${String.format("%.2f", totalOwesMe)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00897B)
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "You Will Give",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "PKR ${String.format("%.2f", totalIOwe)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE53935)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_search_input"),
                placeholder = { Text("Search by customer name or phone...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Tabs
            val tabs = listOf("All Customers", "Who Owe Me", "I Owe")
            val currentTabIndex = when (selectedFilter) {
                CustomerFilter.ALL -> 0
                CustomerFilter.OWE_ME -> 1
                CustomerFilter.I_OWE -> 2
            }

            TabRow(
                selectedTabIndex = currentTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = currentTabIndex == index,
                        onClick = {
                            val newFilter = when (index) {
                                0 -> CustomerFilter.ALL
                                1 -> CustomerFilter.OWE_ME
                                else -> CustomerFilter.I_OWE
                            }
                            viewModel.setFilter(newFilter)
                        },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Customer List or Empty State
            if (customers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PersonAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No customers yet",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No customer matched '$searchQuery'." else "Add your customers to record credit, debit, and send instant ledger WhatsApp updates.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    customerToEdit = null
                                    showAddEditSheet = true
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Your First Customer")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(customers, key = { it.id }) { customer ->
                        CustomerItemCard(
                            customer = customer,
                            onClick = { selectedCustomerForDetail = customer },
                            onCall = {
                                if (!customer.phone.isNullOrBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${customer.phone}")
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            onWhatsApp = {
                                val targetPhone = customer.whatsappNumber ?: customer.phone
                                if (!targetPhone.isNullOrBlank()) {
                                    val cleanPhone = targetPhone.replace("[^0-9]".toRegex(), "")
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://wa.me/$cleanPhone")
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Bottom Sheet
    if (showAddEditSheet) {
        AddEditCustomerSheet(
            customer = customerToEdit,
            viewModel = viewModel,
            onDismiss = { showAddEditSheet = false },
            onSave = { name, phone, whatsapp, address, notes, photoUrl, openingBalance, balanceType ->
                if (customerToEdit == null) {
                    viewModel.addCustomer(
                        name = name,
                        phone = phone,
                        whatsappNumber = whatsapp,
                        address = address,
                        notes = notes,
                        photoUrl = photoUrl,
                        openingBalance = openingBalance,
                        balanceType = balanceType,
                        onSuccess = { showAddEditSheet = false }
                    )
                } else {
                    viewModel.updateCustomer(
                        customer = customerToEdit!!,
                        name = name,
                        phone = phone,
                        whatsappNumber = whatsapp,
                        address = address,
                        notes = notes,
                        photoUrl = photoUrl,
                        openingBalance = openingBalance,
                        balanceType = balanceType,
                        onSuccess = { showAddEditSheet = false }
                    )
                }
            }
        )
    }

    // Customer Detail Modal Sheet
    selectedCustomerForDetail?.let { customer ->
        CustomerDetailSheet(
            customer = customer,
            viewModel = viewModel,
            onDismiss = { selectedCustomerForDetail = null },
            onEdit = {
                customerToEdit = customer
                selectedCustomerForDetail = null
                showAddEditSheet = true
            },
            onDelete = {
                viewModel.checkCustomerHasTransactions(customer.id) { hasTx, count ->
                    if (hasTx) {
                        deleteWarningMsg = "Warning: '${customer.name}' has $count transaction(s) recorded in their ledger. Moving this customer to trash will hide them from active lists."
                    } else {
                        deleteWarningMsg = "Are you sure you want to move '${customer.name}' to trash? You can restore them anytime."
                    }
                    customerToDeleteConfirm = customer
                }
            },
            onCall = {
                if (!customer.phone.isNullOrBlank()) {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${customer.phone}")
                    }
                    context.startActivity(intent)
                }
            },
            onWhatsApp = {
                val targetPhone = customer.whatsappNumber ?: customer.phone
                if (!targetPhone.isNullOrBlank()) {
                    val cleanPhone = targetPhone.replace("[^0-9]".toRegex(), "")
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://wa.me/$cleanPhone")
                    }
                    context.startActivity(intent)
                }
            },
            onShare = {
                val summaryText = "Account Summary for ${customer.name}:\nCurrent Balance: PKR ${String.format("%.2f", Math.abs(customer.currentBalance))} (${if (customer.currentBalance > 0) "You Will Get" else "You Will Give"})\n- Sent via Alhaaj Khata AI"
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, summaryText)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Ledger Summary"))
            },
            onGeneratePdf = {
                PdfStatementGenerator.generateAndOpenStatement(context, customer)
            }
        )
    }

    // Delete Confirmation Dialog
    customerToDeleteConfirm?.let { customer ->
        AlertDialog(
            onDismissRequest = { customerToDeleteConfirm = null },
            title = { Text("Delete Customer") },
            text = { Text(deleteWarningMsg ?: "Are you sure you want to move '${customer.name}' to trash?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.softDeleteCustomer(customer.id) {
                            if (selectedCustomerForDetail?.id == customer.id) {
                                selectedCustomerForDetail = null
                            }
                            customerToDeleteConfirm = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                    modifier = Modifier.testTag("delete_customer_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Trash / Restore Dialog
    if (showTrashDialog) {
        AlertDialog(
            onDismissRequest = { showTrashDialog = false },
            title = { Text("Deleted Customers (Trash)") },
            text = {
                if (deletedCustomers.isEmpty()) {
                    Text("No deleted customers found in trash.")
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        deletedCustomers.forEach { customer ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = customer.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        customer.phone?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.restoreCustomer(customer.id) },
                                        modifier = Modifier.testTag("restore_customer_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Restore,
                                            contentDescription = "Restore",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTrashDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun CustomerItemCard(
    customer: CustomerEntity,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("customer_item_${customer.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Customer Photo Avatar
            CustomerAvatar(
                photoUrl = customer.photoUrl,
                name = customer.name,
                size = 48.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                customer.phone?.let { phone ->
                    Text(
                        text = phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val formattedDate = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(customer.lastTransactionDate))
                Text(
                    text = "Last active: $formattedDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Balance Badge & Actions
            Column(horizontalAlignment = Alignment.End) {
                val isOwesMe = customer.currentBalance > 0
                val isIOwe = customer.currentBalance < 0
                val balanceColor = if (isOwesMe) Color(0xFF00897B) else if (isIOwe) Color(0xFFE53935) else Color.Gray

                Text(
                    text = "PKR ${String.format("%.2f", Math.abs(customer.currentBalance))}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = balanceColor
                    )
                )
                Text(
                    text = if (isOwesMe) "You Will Get" else if (isIOwe) "You Will Give" else "Settled",
                    style = MaterialTheme.typography.labelSmall,
                    color = balanceColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row {
                    if (!customer.phone.isNullOrBlank()) {
                        IconButton(
                            onClick = onCall,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("call_customer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (!customer.phone.isNullOrBlank() || !customer.whatsappNumber.isNullOrBlank()) {
                        IconButton(
                            onClick = onWhatsApp,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("whatsapp_customer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "WhatsApp",
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCustomerSheet(
    customer: CustomerEntity?,
    viewModel: CustomersViewModel,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        phone: String?,
        whatsapp: String?,
        address: String?,
        notes: String?,
        photoUrl: String?,
        openingBalance: Double,
        balanceType: String
    ) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var whatsapp by remember { mutableStateOf(customer?.whatsappNumber ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var notes by remember { mutableStateOf(customer?.notes ?: "") }
    var photoUrl by remember { mutableStateOf(customer?.photoUrl) }
    var openingBalanceStr by remember { mutableStateOf(customer?.openingBalance?.takeIf { it != 0.0 }?.toString() ?: "") }
    var balanceType by remember { mutableStateOf(customer?.balanceType ?: "YOU_WILL_GET") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var showPhotoPickerSheet by remember { mutableStateOf(false) }
    var isUploadingPhoto by remember { mutableStateOf(false) }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            photoUrl = it.toString()
            isUploadingPhoto = true
            viewModel.uploadPhoto(context, it) { uploaded ->
                if (uploaded != null) {
                    photoUrl = uploaded
                }
                isUploadingPhoto = false
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            photoUrl = tempCameraUri.toString()
            isUploadingPhoto = true
            viewModel.uploadPhoto(context, tempCameraUri!!) { uploaded ->
                if (uploaded != null) {
                    photoUrl = uploaded
                }
                isUploadingPhoto = false
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (customer == null) "Add New Customer" else "Edit Customer",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Photo Avatar & Picker Button
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp)
            ) {
                CustomerAvatar(
                    photoUrl = photoUrl,
                    name = if (name.isBlank()) "Customer" else name,
                    size = 80.dp,
                    modifier = Modifier.clickable { showPhotoPickerSheet = true }
                )
                IconButton(
                    onClick = { showPhotoPickerSheet = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Edit Photo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (isUploadingPhoto) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(36.dp),
                        strokeWidth = 3.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name (Required)
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (it.isNotBlank()) nameError = null
                },
                label = { Text("Customer Name *") },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_customer_name_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Phone
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_customer_phone_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // WhatsApp Number (Optional)
            OutlinedTextField(
                value = whatsapp,
                onValueChange = { whatsapp = it },
                label = { Text("WhatsApp Number (Optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Address
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Additional Remarks") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Opening Balance
            Text(
                text = "Opening Balance Settings",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = openingBalanceStr,
                onValueChange = { openingBalanceStr = it },
                label = { Text("Opening Balance Amount (PKR)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Balance Type Radio
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { balanceType = "YOU_WILL_GET" }
                ) {
                    RadioButton(
                        selected = balanceType == "YOU_WILL_GET",
                        onClick = { balanceType = "YOU_WILL_GET" }
                    )
                    Text("Customer Will Pay Me (You Will Get)")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { balanceType = "YOU_WILL_GIVE" }
                ) {
                    RadioButton(
                        selected = balanceType == "YOU_WILL_GIVE",
                        onClick = { balanceType = "YOU_WILL_GIVE" }
                    )
                    Text("I Will Pay Customer (You Will Give)")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        if (name.isBlank()) {
                            nameError = "Customer name is required"
                        } else {
                            val balance = openingBalanceStr.toDoubleOrNull() ?: 0.0
                            onSave(name, phone, whatsapp, address, notes, photoUrl, balance, balanceType)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_customer_submit")
                ) {
                    Text("Save Customer")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showPhotoPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoPickerSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Customer Photo",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                TextButton(
                    onClick = {
                        showPhotoPickerSheet = false
                        try {
                            val file = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            tempCameraUri = uri
                            cameraLauncher.launch(uri)
                        } catch (e: Exception) {
                            // Fallback
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Take Photo", modifier = Modifier.weight(1f))
                }

                TextButton(
                    onClick = {
                        showPhotoPickerSheet = false
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Choose from Gallery", modifier = Modifier.weight(1f))
                }

                if (!photoUrl.isNullOrBlank()) {
                    TextButton(
                        onClick = {
                            showPhotoPickerSheet = false
                            photoUrl = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Remove Photo", modifier = Modifier.weight(1f))
                    }
                }

                OutlinedButton(
                    onClick = { showPhotoPickerSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailSheet(
    customer: CustomerEntity,
    viewModel: CustomersViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onShare: () -> Unit,
    onGeneratePdf: () -> Unit
) {
    val context = LocalContext.current
    val customersList by viewModel.customers.collectAsState()
    val liveCustomer = customersList.firstOrNull { it.id == customer.id } ?: customer
    val transactions by viewModel.getCustomerTransactionsFlow(customer.id).collectAsState(initial = emptyList())

    var showAddEditTransactionSheet by remember { mutableStateOf(false) }
    var activeTransactionType by remember { mutableStateOf(TransactionType.PAYMENT_RECEIVED) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToDeleteConfirm by remember { mutableStateOf<TransactionEntity?>(null) }

    val totalReceived = transactions.filter { it.type == TransactionType.PAYMENT_RECEIVED }.sumOf { it.amount }
    val totalGiven = transactions.filter { it.type == TransactionType.PAYMENT_GIVEN }.sumOf { it.amount }
    val totalTransactionsCount = transactions.size

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Action Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Customer Ledger",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_customer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Customer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_customer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Photo & Name Banner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                CustomerAvatar(
                    photoUrl = liveCustomer.photoUrl,
                    name = liveCustomer.name,
                    size = 64.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = liveCustomer.name,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    if (!liveCustomer.phone.isNullOrBlank()) {
                        Text(
                            text = liveCustomer.phone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Account Balance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val isOwesMe = liveCustomer.currentBalance > 0
                    val isIOwe = liveCustomer.currentBalance < 0
                    val balanceColor = if (isOwesMe) Color(0xFF2E7D32) else if (isIOwe) Color(0xFFC62828) else Color.Gray

                    Text(
                        text = "Current Account Balance",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "PKR ${String.format("%.2f", Math.abs(liveCustomer.currentBalance))}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = balanceColor
                        )
                    )
                    Text(
                        text = if (isOwesMe) "Customer Will Pay You" else if (isIOwe) "You Will Pay Customer" else "Account Settled",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = balanceColor
                    )

                    if (liveCustomer.openingBalance != 0.0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val openingTypeLabel = if (liveCustomer.balanceType == "YOU_WILL_GIVE") "You Will Give" else "You Will Get"
                        Text(
                            text = "Opening Balance: PKR ${String.format("%.2f", Math.abs(liveCustomer.openingBalance))} ($openingTypeLabel)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Action Buttons (1. Payment Received & Payment Given)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        transactionToEdit = null
                        activeTransactionType = TransactionType.PAYMENT_RECEIVED
                        showAddEditTransactionSheet = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("payment_received_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Payment Received", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        transactionToEdit = null
                        activeTransactionType = TransactionType.PAYMENT_GIVEN
                        showAddEditTransactionSheet = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("payment_given_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Payment Given", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Information Card (Requirement 5)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Account Summary",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryMetricItem(
                            label = "Opening Balance",
                            value = "PKR ${String.format("%.2f", liveCustomer.openingBalance)}",
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMetricItem(
                            label = "Current Balance",
                            value = "PKR ${String.format("%.2f", Math.abs(liveCustomer.currentBalance))}",
                            color = if (liveCustomer.currentBalance > 0) Color(0xFF2E7D32) else if (liveCustomer.currentBalance < 0) Color(0xFFC62828) else Color.Gray,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryMetricItem(
                            label = "Total Received",
                            value = "PKR ${String.format("%.2f", totalReceived)}",
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMetricItem(
                            label = "Total Given",
                            value = "PKR ${String.format("%.2f", totalGiven)}",
                            color = Color(0xFFC62828),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Total Transactions: $totalTransactionsCount entries",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Communication & PDF Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCall,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call")
                }

                Button(
                    onClick = onWhatsApp,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }

                Button(
                    onClick = onGeneratePdf,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("generate_pdf_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF Statement")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Complete Transaction History Header (Requirement 4)
            Text(
                text = "Transaction History (${transactions.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (transactions.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Transactions Recorded Yet",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap 'Payment Received' or 'Payment Given' above to add your first transaction entry.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    transactions.forEach { tx ->
                        TransactionLedgerCard(
                            transaction = tx,
                            onEdit = {
                                transactionToEdit = tx
                                activeTransactionType = tx.type
                                showAddEditTransactionSheet = true
                            },
                            onDelete = {
                                transactionToDeleteConfirm = tx
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Add / Edit Transaction Sheet
    if (showAddEditTransactionSheet) {
        AddEditTransactionSheet(
            initialTx = transactionToEdit,
            defaultType = activeTransactionType,
            customerName = liveCustomer.name,
            viewModel = viewModel,
            onDismiss = { showAddEditTransactionSheet = false },
            onSave = { type, amount, notes, paymentMethod, date, attachmentUrl ->
                if (transactionToEdit == null) {
                    viewModel.addTransaction(
                        customerId = liveCustomer.id,
                        customerName = liveCustomer.name,
                        type = type,
                        amount = amount,
                        notes = notes,
                        paymentMethod = paymentMethod,
                        date = date,
                        attachmentUrl = attachmentUrl,
                        onSuccess = { showAddEditTransactionSheet = false }
                    )
                } else {
                    viewModel.updateTransaction(
                        existingTx = transactionToEdit!!,
                        type = type,
                        amount = amount,
                        notes = notes,
                        paymentMethod = paymentMethod,
                        date = date,
                        attachmentUrl = attachmentUrl,
                        onSuccess = { showAddEditTransactionSheet = false }
                    )
                }
            }
        )
    }

    // Delete Transaction Confirmation Dialog
    transactionToDeleteConfirm?.let { txToDelete ->
        AlertDialog(
            onDismissRequest = { transactionToDeleteConfirm = null },
            title = { Text("Delete Transaction Entry") },
            text = {
                Text("Are you sure you want to delete this ${if (txToDelete.type == TransactionType.PAYMENT_RECEIVED) "Payment Received" else "Payment Given"} entry of PKR ${String.format("%.2f", txToDelete.amount)}?\n\nThe customer account balance will be recalculated automatically.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val txId = txToDelete.id
                        transactionToDeleteConfirm = null
                        viewModel.deleteTransaction(txId) {}
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
    initialTx: TransactionEntity?,
    defaultType: TransactionType,
    customerName: String,
    viewModel: CustomersViewModel,
    onDismiss: () -> Unit,
    onSave: (
        type: TransactionType,
        amount: Double,
        notes: String?,
        paymentMethod: String?,
        date: Long,
        attachmentUrl: String?
    ) -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(initialTx?.type ?: defaultType) }
    var amountStr by remember { mutableStateOf(initialTx?.amount?.let { String.format("%.2f", it) } ?: "") }
    var notes by remember { mutableStateOf(initialTx?.notes ?: "") }
    var paymentMethod by remember { mutableStateOf(initialTx?.paymentMethod ?: "Cash") }
    var dateLong by remember { mutableStateOf(initialTx?.date ?: System.currentTimeMillis()) }
    var attachmentUrl by remember { mutableStateOf(initialTx?.attachmentUrl) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var isUploadingAttachment by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            isUploadingAttachment = true
            viewModel.uploadPhoto(context, it) { uploaded ->
                if (uploaded != null) {
                    attachmentUrl = uploaded
                }
                isUploadingAttachment = false
            }
        }
    }

    val isReceived = type == TransactionType.PAYMENT_RECEIVED
    val themeColor = if (isReceived) Color(0xFF2E7D32) else Color(0xFFC62828)
    val titleText = if (initialTx != null) "Edit Transaction" else if (isReceived) "Record Payment Received" else "Record Payment Given"

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = themeColor
                )
                Text(
                    text = customerName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Type Toggle (Received vs Given)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (type == TransactionType.PAYMENT_RECEIVED) Color(0xFF2E7D32) else Color.Transparent)
                        .clickable { type = TransactionType.PAYMENT_RECEIVED }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Payment Received (+)",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (type == TransactionType.PAYMENT_RECEIVED) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (type == TransactionType.PAYMENT_GIVEN) Color(0xFFC62828) else Color.Transparent)
                        .clickable { type = TransactionType.PAYMENT_GIVEN }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Payment Given (-)",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (type == TransactionType.PAYMENT_GIVEN) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input (Required)
            OutlinedTextField(
                value = amountStr,
                onValueChange = {
                    amountStr = it
                    amountError = null
                },
                label = { Text("Amount (PKR)*") },
                prefix = { Text("PKR ", fontWeight = FontWeight.Bold) },
                isError = amountError != null,
                supportingText = amountError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_amount_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Date Display
            val formattedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(dateLong))
            OutlinedTextField(
                value = formattedDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Transaction Date") },
                trailingIcon = {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Date")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { dateLong = System.currentTimeMillis() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Payment Method Selector Chips
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            val methods = listOf("Cash", "Bank Transfer", "Online", "Cheque", "Other")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                methods.forEach { method ->
                    val isSelected = paymentMethod.equals(method, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) themeColor else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { paymentMethod = method }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = method,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notes / Bill Description
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Reference (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Attachment Section
            Text(
                text = "Receipt Attachment (Optional)",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (!attachmentUrl.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.AttachFile, contentDescription = null, tint = themeColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Attachment Attached",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { attachmentUrl = null }) {
                        Text("Remove", color = Color.Red)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isUploadingAttachment) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Receipt / Photo Attachment")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        val parsedAmount = amountStr.toDoubleOrNull()
                        if (parsedAmount == null || parsedAmount <= 0) {
                            amountError = "Please enter a valid amount greater than 0"
                        } else {
                            onSave(type, parsedAmount, notes, paymentMethod, dateLong, attachmentUrl)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("save_transaction_button")
                ) {
                    Text("Save Transaction", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TransactionLedgerCard(
    transaction: TransactionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isReceived = transaction.type == TransactionType.PAYMENT_RECEIVED
    val typeColor = if (isReceived) Color(0xFF2E7D32) else Color(0xFFC62828)
    val typeBg = if (isReceived) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val typeLabel = if (isReceived) "Payment Received" else "Payment Given"
    val formattedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(transaction.date))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(typeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isReceived) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = typeColor
                            )
                        )
                    }
                }

                // Amount
                Text(
                    text = "${if (isReceived) "+" else "-"} PKR ${String.format("%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = typeColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!transaction.paymentMethod.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = transaction.paymentMethod,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!transaction.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Notes: ${transaction.notes}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (!transaction.attachmentUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Receipt Attached",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Running Balance: PKR ${String.format("%.2f", Math.abs(transaction.runningBalance))}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (transaction.runningBalance > 0) Color(0xFF2E7D32) else if (transaction.runningBalance < 0) Color(0xFFC62828) else Color.Gray
                )

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Transaction",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Transaction",
                            tint = Color.Red,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryMetricItem(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
    }
}
