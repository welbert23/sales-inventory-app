package com.salesinventory.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salesinventory.app.data.CartItem
import com.salesinventory.app.data.InventoryItem
import com.salesinventory.app.data.PaymentType
import com.salesinventory.app.util.BluetoothPrinter
import com.salesinventory.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkCheckoutScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val inventory by viewModel.inventory.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val error by viewModel.error.collectAsState()

    var cartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var barcodeInput by remember { mutableStateOf("") }
    var showProductPicker by remember { mutableStateOf(false) }
    var selectedCustomerId by remember { mutableStateOf("") }
    var selectedPaymentType by remember { mutableStateOf(PaymentType.CASH) }
    var isCredit by remember { mutableStateOf(false) }
    var showCustomerDropdown by remember { mutableStateOf(false) }
    var showPaymentDropdown by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var lastTransactionId by remember { mutableStateOf("") }
    var showPrintDialog by remember { mutableStateOf(false) }
    var showPrinterPicker by remember { mutableStateOf(false) }
    var pairedPrinters by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    fun addToCart(barcode: String) {
        val item = inventory.find { it.barcode == barcode }
        if (item == null) {
            localError = "Item not found: $barcode"
            return
        }
        val existing = cartItems.indexOfFirst { it.barcode == barcode }
        if (existing >= 0) {
            cartItems = cartItems.toMutableList().also {
                val c = it[existing]
                it[existing] = c.copy(quantity = c.quantity + 1)
            }
        } else {
            cartItems = cartItems + CartItem(
                barcode = item.barcode,
                productName = item.name,
                quantity = 1,
                unitPrice = item.price,
                costPrice = item.costPrice
            )
        }
        barcodeInput = ""
    }

    fun itemTotal(cart: CartItem): Double = (cart.unitPrice * cart.quantity) - cart.discountAmount

    val totalAmount = cartItems.sumOf { (it.unitPrice * it.quantity) - it.discountAmount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bulk Checkout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = barcodeInput,
                    onValueChange = { barcodeInput = it },
                    placeholder = { Text("Scan or type barcode...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                )
                FilledTonalIconButton(onClick = { if (barcodeInput.isNotBlank()) addToCart(barcodeInput) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }
                FilledTonalIconButton(onClick = { showProductPicker = true }) {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                }
            }

            Spacer(Modifier.height(8.dp))

            if (cartItems.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("Cart is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(cartItems, key = { it.barcode }) { cart ->
                        val item = inventory.find { it.barcode == cart.barcode }
                        val stock = item?.stock ?: 0
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cart.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("PHP ${"%.2f".format(cart.unitPrice)} each", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(onClick = {
                                            cartItems = cartItems.toMutableList().also {
                                                val idx = it.indexOfFirst { c -> c.barcode == cart.barcode }
                                                if (idx >= 0) {
                                                    if (it[idx].quantity <= 1) it.removeAt(idx)
                                                    else it[idx] = it[idx].copy(quantity = it[idx].quantity - 1)
                                                }
                                            }
                                        }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Filled.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp))
                                        }
                                        Text("${cart.quantity}", fontWeight = FontWeight.Bold)
                                        IconButton(onClick = {
                                            if (cart.quantity < stock) {
                                                cartItems = cartItems.toMutableList().also {
                                                    val idx = it.indexOfFirst { c -> c.barcode == cart.barcode }
                                                    if (idx >= 0) it[idx] = it[idx].copy(quantity = it[idx].quantity + 1)
                                                }
                                            }
                                        }, enabled = cart.quantity < stock, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Filled.Add, contentDescription = "Increase", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("PHP ${"%.2f".format(itemTotal(cart))}", fontWeight = FontWeight.Bold)
                                    IconButton(onClick = {
                                        cartItems = cartItems.filter { it.barcode != cart.barcode }
                                    }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Items:", fontWeight = FontWeight.Medium)
                        Text("${cartItems.sumOf { it.quantity }}")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Amount:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("PHP ${"%.2f".format(totalAmount)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(expanded = showCustomerDropdown, onExpandedChange = { showCustomerDropdown = it }) {
                OutlinedTextField(
                    value = customers.find { it.id == selectedCustomerId }?.name ?: "Walk-in Customer",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Customer") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCustomerDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    singleLine = true
                )
                ExposedDropdownMenu(expanded = showCustomerDropdown, onDismissRequest = { showCustomerDropdown = false }) {
                    DropdownMenuItem(text = { Text("Walk-in Customer") }, onClick = { selectedCustomerId = ""; showCustomerDropdown = false })
                    customers.forEach { c ->
                        DropdownMenuItem(text = { Text("${c.name} (${"%.0f".format(c.creditBalance)})") }, onClick = { selectedCustomerId = c.id; showCustomerDropdown = false })
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            ExposedDropdownMenuBox(expanded = showPaymentDropdown, onExpandedChange = { showPaymentDropdown = it }) {
                OutlinedTextField(
                    value = selectedPaymentType.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Payment Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPaymentDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    singleLine = true
                )
                ExposedDropdownMenu(expanded = showPaymentDropdown, onDismissRequest = { showPaymentDropdown = false }) {
                    PaymentType.entries.forEach { pt ->
                        DropdownMenuItem(text = { Text(pt.name) }, onClick = { selectedPaymentType = pt; showPaymentDropdown = false })
                    }
                }
            }

            if (selectedCustomerId.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Checkbox(checked = isCredit, onCheckedChange = { isCredit = it })
                    Text("Post as credit", fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.processBulkSale(cartItems, selectedCustomerId, selectedPaymentType, isCredit)
                    lastTransactionId = java.util.UUID.randomUUID().toString().take(8)
                    cartItems = emptyList()
                    showPrintDialog = true
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = cartItems.isNotEmpty()
            ) {
                Icon(Icons.Filled.ShoppingCartCheckout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Checkout (PHP ${"%.2f".format(totalAmount)})")
            }
        }
    }

    if (showPrintDialog) {
        AlertDialog(
            onDismissRequest = { showPrintDialog = false },
            title = { Text("Print Receipt?") },
            text = { Text("Do you want to print a receipt for this transaction?") },
            confirmButton = {
                TextButton(onClick = {
                    showPrintDialog = false
                    pairedPrinters = BluetoothPrinter.getPairedPrinters()
                    if (pairedPrinters.isNotEmpty()) showPrinterPicker = true
                    else localError = "No paired Bluetooth printers found"
                }) { Text("Print") }
            },
            dismissButton = {
                TextButton(onClick = { showPrintDialog = false }) { Text("No") }
            }
        )
    }

    if (showPrinterPicker && pairedPrinters.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showPrinterPicker = false },
            title = { Text("Select Printer") },
            text = {
                Column {
                    pairedPrinters.forEach { (address, name) ->
                        TextButton(
                            onClick = {
                                showPrinterPicker = false
                                viewModel.printReceipt(lastTransactionId, address) { success ->
                                    if (success) localError = "Receipt printed"
                                    else localError = "Print failed"
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrinterPicker = false }) { Text("Cancel") }
            }
        )
    }

    if (localError != null) {
        AlertDialog(
            onDismissRequest = { localError = null },
            title = { Text("Error") },
            text = { Text(localError!!) },
            confirmButton = {
                TextButton(onClick = { localError = null }) { Text("OK") }
            }
        )
    }

    if (error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Notice") },
            text = { Text(error!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) { Text("OK") }
            }
        )
    }

    if (showProductPicker) {
        ProductPickerDialog(
            items = inventory,
            onDismiss = { showProductPicker = false },
            onSelect = { item ->
                showProductPicker = false
                val existing = cartItems.indexOfFirst { it.barcode == item.barcode }
                if (existing >= 0) {
                    cartItems = cartItems.toMutableList().also {
                        val c = it[existing]
                        it[existing] = c.copy(quantity = c.quantity + 1)
                    }
                } else {
                    cartItems = cartItems + CartItem(barcode = item.barcode, productName = item.name, quantity = 1, unitPrice = item.price, costPrice = item.costPrice)
                }
            }
        )
    }
}
