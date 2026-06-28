package com.salesinventory.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salesinventory.app.data.CartItem
import com.salesinventory.app.data.DiscountType
import com.salesinventory.app.data.InventoryItem
import com.salesinventory.app.data.PaymentType
import com.salesinventory.app.ui.theme.*
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
    val discount by viewModel.currentDiscount.collectAsState()
    val discounts by viewModel.discounts.collectAsState()
    val context = LocalContext.current

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
        val trimmed = barcode.trim()
        val item = inventory.find { it.barcode == trimmed }
        if (item == null) {
            localError = "Item not found: $trimmed"
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
                title = { Text("Bulk Checkout", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Blue800,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = barcodeInput,
                        onValueChange = { barcodeInput = it },
                        placeholder = { Text("Type barcode...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (barcodeInput.isNotBlank()) addToCart(barcodeInput) })
                    )
                    FilledTonalIconButton(onClick = { if (barcodeInput.isNotBlank()) addToCart(barcodeInput) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add")
                    }
                    FilledTonalIconButton(onClick = { showProductPicker = true }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (cartItems.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(64.dp), tint = Grey400)
                        Spacer(Modifier.height(8.dp))
                        Text("Cart is empty", color = Grey600)
                        Text("Add items to begin checkout", fontSize = 13.sp, color = Grey600)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(cartItems, key = { it.barcode }) { cart ->
                        val item = inventory.find { it.barcode == cart.barcode }
                        val stock = item?.stock ?: 0
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cart.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("PHP ${"%.2f".format(cart.unitPrice)} each", fontSize = 12.sp, color = Blue800)
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
                                        Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = Red700, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Discount card
            val activeDiscount = discount
            var showDiscountPicker by remember { mutableStateOf(false) }
            if (discounts.isNotEmpty() || activeDiscount != null) {
                Card(
                    onClick = { showDiscountPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(if (activeDiscount != null) Color(0xFFFFF3E0) else Color(0xFFF5F5F5))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (activeDiscount != null) Icons.Filled.Star else Icons.Filled.Add,
                            contentDescription = null,
                            tint = if (activeDiscount != null) Color(0xFFF57F17) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (activeDiscount != null) "Discount: ${activeDiscount.name}" else "Tap to add discount",
                            fontWeight = if (activeDiscount != null) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = if (activeDiscount != null) Color(0xFFF57F17) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (activeDiscount != null) {
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { viewModel.setCurrentDiscount(null) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove discount", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            val discountAmt = if (activeDiscount != null && activeDiscount.isActive) {
                when (activeDiscount.type) {
                    DiscountType.PERCENTAGE -> totalAmount * activeDiscount.value / 100.0
                    DiscountType.FIXED_AMOUNT -> activeDiscount.value
                }
            } else 0.0
            val grandTotal = totalAmount - discountAmt

            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Blue50)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Items:", fontWeight = FontWeight.Medium)
                        Text("${cartItems.sumOf { it.quantity }}")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal:", fontWeight = FontWeight.Medium)
                        Text("PHP ${"%.2f".format(totalAmount)}")
                    }
                    if (discountAmt > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Discount:", fontSize = 13.sp, color = Color(0xFFF57F17))
                            Text("-PHP ${"%.2f".format(discountAmt)}", fontSize = 13.sp, color = Color(0xFFF57F17))
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TOTAL:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("PHP ${"%.2f".format(grandTotal)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Blue800)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    ExposedDropdownMenuBox(expanded = showCustomerDropdown, onExpandedChange = { showCustomerDropdown = it }) {
                        val displayText = when {
                            selectedCustomerId == "" -> "Walk-in"
                            selectedCustomerId == "__online__" -> "Online"
                            else -> customers.find { it.id == selectedCustomerId }?.let { "${it.name} (${it.customerType})" } ?: "Walk-in"
                        }
                        OutlinedTextField(
                            value = displayText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Customer") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCustomerDropdown) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(expanded = showCustomerDropdown, onDismissRequest = { showCustomerDropdown = false }) {
                            DropdownMenuItem(text = { Text("Walk-in") }, onClick = { selectedCustomerId = ""; showCustomerDropdown = false })
                            DropdownMenuItem(text = { Text("Online") }, onClick = { selectedCustomerId = "__online__"; showCustomerDropdown = false })
                            if (customers.isNotEmpty()) {
                                Divider()
                                customers.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text("${c.name} (${c.customerType}) - PHP ${"%.0f".format(c.creditBalance)}") },
                                        onClick = { selectedCustomerId = c.id; showCustomerDropdown = false }
                                    )
                                }
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
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
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
                }
            }

            Spacer(Modifier.height(8.dp))

            // Discount picker dialog
            if (showDiscountPicker && discounts.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = { showDiscountPicker = false },
                    title = { Text("Select Discount", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            discounts.forEach { d ->
                                val desc = if (d.type == DiscountType.PERCENTAGE) "${d.value}% OFF" else "PHP ${d.value} OFF"
                                Surface(
                                    onClick = { viewModel.setCurrentDiscount(d); showDiscountPicker = false },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (activeDiscount?.id == d.id) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(d.name, fontWeight = FontWeight.Bold)
                                            Text(desc, fontSize = 12.sp, color = Color(0xFFF57F17))
                                        }
                                        if (activeDiscount?.id == d.id) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showDiscountPicker = false }) { Text("Cancel") } }
                )
            }

            Button(
                onClick = {
                    val (actualCustomerId, customerType) = when {
                        selectedCustomerId == "__online__" -> "" to "Online"
                        selectedCustomerId.isBlank() -> "" to "Walk-in"
                        else -> {
                            val c = customers.find { it.id == selectedCustomerId }
                            selectedCustomerId to (c?.customerType ?: "Walk-in")
                        }
                    }
                    val tid = java.util.UUID.randomUUID().toString()
                    viewModel.processBulkSale(cartItems, actualCustomerId, customerType, selectedPaymentType, isCredit, tid)
                    lastTransactionId = tid
                    cartItems = emptyList()
                    showPrintDialog = true
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = cartItems.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue800)
            ) {
                Icon(Icons.Filled.ShoppingCartCheckout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Checkout (PHP ${"%.2f".format(grandTotal)})")
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

    if (showPrinterPicker) {
        AlertDialog(
            onDismissRequest = { showPrinterPicker = false },
            title = { Text("Select Printer") },
            text = {
                Column {
                    if (pairedPrinters.isNotEmpty()) {
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
                    } else {
                        Text("No paired Bluetooth printers found", fontSize = 13.sp, color = Grey600, modifier = Modifier.padding(8.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            showPrinterPicker = false
                            BluetoothPrinter.openBluetoothSettings(context)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Open Bluetooth Settings to pair printer")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrinterPicker = false }) { Text("Cancel") }
            }
        )
    }

    val localErr = localError
    if (localErr != null) {
        AlertDialog(
            onDismissRequest = { localError = null },
            title = { Text("Error") },
            text = { Text(localErr) },
            confirmButton = {
                TextButton(onClick = { localError = null }) { Text("OK") }
            }
        )
    }

    val errMsg = error
    if (errMsg != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Notice") },
            text = { Text(errMsg) },
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
