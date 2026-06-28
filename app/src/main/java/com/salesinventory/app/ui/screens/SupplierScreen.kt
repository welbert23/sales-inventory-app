package com.salesinventory.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salesinventory.app.data.Supplier
import com.salesinventory.app.ui.theme.*
import com.salesinventory.app.viewmodel.MainViewModel
import androidx.compose.material3.AlertDialog
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val suppliers by viewModel.suppliers.collectAsState()
    val error by viewModel.error.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<Supplier?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suppliers", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { editingSupplier = null; showDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Supplier")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Blue800,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        if (suppliers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Business, contentDescription = null, modifier = Modifier.size(64.dp), tint = Grey400)
                    Spacer(Modifier.height(8.dp))
                    Text("No suppliers yet", color = Grey600)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { editingSupplier = null; showDialog = true },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add Supplier")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suppliers, key = { it.id }) { supplier ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(supplier.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    if (supplier.contactPerson.isNotBlank()) {
                                        Text("Contact: ${supplier.contactPerson}", fontSize = 13.sp, color = Grey600)
                                    }
                                    if (supplier.phone.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = Grey600)
                                            Spacer(Modifier.width(4.dp))
                                            Text(supplier.phone, fontSize = 13.sp, color = Grey600)
                                        }
                                    }
                                }
                                TextButton(onClick = { editingSupplier = supplier; showDialog = true }) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Edit", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        SupplierDialog(
            supplier = editingSupplier,
            onDismiss = { showDialog = false; editingSupplier = null },
            onSave = { s ->
                viewModel.addOrUpdateSupplier(s)
                showDialog = false
                editingSupplier = null
            },
            onDelete = editingSupplier?.let { { showDeleteConfirm = true } }
        )
    }

    if (showDeleteConfirm) {
        val supp = editingSupplier
        if (supp != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Confirm Delete") },
                text = { Text("Delete supplier \"${supp.name}\"?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.removeSupplier(supp.id)
                        showDeleteConfirm = false
                        showDialog = false
                        editingSupplier = null
                    }) { Text("Delete", color = Red700) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                }
            )
        }
    }

    val errMsg = error
    if (errMsg != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(errMsg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun SupplierDialog(
    supplier: Supplier?,
    onDismiss: () -> Unit,
    onSave: (Supplier) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(supplier?.name ?: "") }
    var contactPerson by remember { mutableStateOf(supplier?.contactPerson ?: "") }
    var phone by remember { mutableStateOf(supplier?.phone ?: "") }
    var email by remember { mutableStateOf(supplier?.email ?: "") }
    var address by remember { mutableStateOf(supplier?.address ?: "") }
    var notes by remember { mutableStateOf(supplier?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (supplier == null) "Add Supplier" else "Edit Supplier", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Company Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = contactPerson, onValueChange = { contactPerson = it }, label = { Text("Contact Person") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = Red700)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSave(
                                Supplier(
                                    id = supplier?.id ?: UUID.randomUUID().toString().take(8),
                                    name = name,
                                    contactPerson = contactPerson,
                                    phone = phone,
                                    email = email,
                                    address = address,
                                    notes = notes
                                )
                            )
                        }
                    },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Save") }
            }
        }
    )
}
