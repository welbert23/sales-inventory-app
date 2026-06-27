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
import com.salesinventory.app.data.CreditPayment
import com.salesinventory.app.data.Customer
import com.salesinventory.app.ui.theme.*
import com.salesinventory.app.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val customers by viewModel.customers.collectAsState()
    val error by viewModel.error.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf<Customer?>(null) }
    var showPaymentHistory by remember { mutableStateOf<Customer?>(null) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customers", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { editingCustomer = null; showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Customer")
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
        if (customers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.People, contentDescription = null, modifier = Modifier.size(64.dp), tint = Grey400)
                    Spacer(Modifier.height(8.dp))
                    Text("No customers yet", color = Grey600)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { editingCustomer = null; showAddDialog = true },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add Customer")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(customers, key = { it.id }) { customer ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(1.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (customer.creditBalance > 0) Amber50 else Color.White
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    if (customer.phone.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = Grey600)
                                            Spacer(Modifier.width(4.dp))
                                            Text(customer.phone, fontSize = 13.sp, color = Grey600)
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(customer.customerType, fontSize = 11.sp, color = if (customer.customerType == "Online") Blue800 else Green700)
                                    Text("Balance:", fontSize = 11.sp, color = Grey600)
                                    Text(
                                        "PHP ${"%.2f".format(customer.creditBalance)}",
                                        fontWeight = FontWeight.Bold,
                                        color = if (customer.creditBalance > 0) Amber700 else Green700
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { showPaymentHistory = customer }) {
                                    Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("History", fontSize = 12.sp)
                                }
                                TextButton(onClick = { showPaymentDialog = customer }) {
                                    Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Payment", fontSize = 12.sp)
                                }
                                TextButton(onClick = { editingCustomer = customer; showAddDialog = true }) {
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

    if (showAddDialog) {
        CustomerDialog(
            customer = editingCustomer,
            onDismiss = { showAddDialog = false; editingCustomer = null },
            onSave = { c ->
                viewModel.addOrUpdateCustomer(c)
                showAddDialog = false
                editingCustomer = null
            },
            onDelete = if (editingCustomer != null) {{ viewModel.removeCustomer(editingCustomer!!.id); showAddDialog = false; editingCustomer = null }} else null
        )
    }

    showPaymentDialog?.let { customer ->
        PaymentDialog(
            customer = customer,
            onDismiss = { showPaymentDialog = null },
            onRecord = { amount, notes ->
                val payment = CreditPayment(
                    id = UUID.randomUUID().toString().take(8),
                    customerId = customer.id,
                    amount = amount,
                    date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                    notes = notes
                )
                viewModel.recordCreditPayment(payment)
                showPaymentDialog = null
            }
        )
    }

    showPaymentHistory?.let { customer ->
        PaymentHistoryDialog(
            customer = customer,
            payments = viewModel.getCustomerPayments(customer.id),
            onDismiss = { showPaymentHistory = null }
        )
    }

    if (error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) { Text("OK") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerDialog(
    customer: Customer?,
    onDismiss: () -> Unit,
    onSave: (Customer) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var email by remember { mutableStateOf(customer?.email ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var customerType by remember { mutableStateOf(customer?.customerType ?: "Walk-in") }
    var notes by remember { mutableStateOf(customer?.notes ?: "") }
    var showTypeDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (customer == null) "Add Customer" else "Edit Customer", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                ExposedDropdownMenuBox(expanded = showTypeDropdown, onExpandedChange = { showTypeDropdown = it }) {
                    OutlinedTextField(
                        value = customerType, onValueChange = {}, readOnly = true,
                        label = { Text("Type") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true, shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(expanded = showTypeDropdown, onDismissRequest = { showTypeDropdown = false }) {
                        listOf("Walk-in", "Online").forEach { type ->
                            DropdownMenuItem(text = { Text(type) }, onClick = { customerType = type; showTypeDropdown = false })
                        }
                    }
                }
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
                                Customer(
                                    id = customer?.id ?: UUID.randomUUID().toString().take(8),
                                    name = name,
                                    phone = phone,
                                    email = email,
                                    address = address,
                                    customerType = customerType,
                                    creditBalance = customer?.creditBalance ?: 0.0,
                                    totalPurchases = customer?.totalPurchases ?: 0.0,
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

@Composable
private fun PaymentDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onRecord: (Double, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Customer: ${customer.name}", fontWeight = FontWeight.Medium)
                Text("Current Balance: PHP ${"%.2f".format(customer.creditBalance)}", color = if (customer.creditBalance > 0) Amber700 else Green700, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Payment Amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        if (amt > 0) onRecord(amt, notes)
                    },
                    enabled = amount.toDoubleOrNull() ?: 0.0 > 0,
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Record Payment") }
            }
        }
    )
}

@Composable
private fun PaymentHistoryDialog(
    customer: Customer,
    payments: List<CreditPayment>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("Payment History", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(customer.name, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Grey600)
                Spacer(Modifier.height(8.dp))
                if (payments.isEmpty()) {
                    Box(Modifier.height(100.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No payments recorded", color = Grey600)
                    }
                } else {
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(payments, key = { it.id }) { payment ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("PHP ${"%.2f".format(payment.amount)}", fontWeight = FontWeight.Bold, color = Green700)
                                    Text(payment.date, fontSize = 11.sp, color = Grey600)
                                    if (payment.notes.isNotBlank()) Text(payment.notes, fontSize = 11.sp, color = Grey600)
                                }
                            }
                            if (payment != payments.last()) Divider()
                        }
                    }
                }
            }
        }
    )
}
