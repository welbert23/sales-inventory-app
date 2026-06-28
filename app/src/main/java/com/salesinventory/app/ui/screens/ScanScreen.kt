package com.salesinventory.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.salesinventory.app.data.CartItem
import com.salesinventory.app.data.DiscountType
import com.salesinventory.app.data.InventoryItem
import com.salesinventory.app.data.PaymentType
import com.salesinventory.app.scanner.BarcodeAnalyzer
import com.salesinventory.app.ui.theme.*
import com.salesinventory.app.util.BarcodeProductInfo
import com.salesinventory.app.util.lookupBarcode
import com.salesinventory.app.util.BluetoothPrinter
import com.salesinventory.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val inventory by viewModel.inventory.collectAsState()
    val error by viewModel.error.collectAsState()
    val discount by viewModel.currentDiscount.collectAsState()
    val discounts by viewModel.discounts.collectAsState()

    var cartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var isScanning by remember { mutableStateOf(true) }
    var showProductPicker by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf(false) }
    var currentItem by remember { mutableStateOf<InventoryItem?>(null) }
    var saleQty by remember { mutableStateOf(1) }
    var missingBarcode by remember { mutableStateOf("") }
    var lookupResult by remember { mutableStateOf<BarcodeProductInfo?>(null) }
    var lookupLoading by remember { mutableStateOf(false) }
    var showAddFromLookup by remember { mutableStateOf(false) }
    var lookupName by remember { mutableStateOf("") }
    var lookupCategory by remember { mutableStateOf("") }
    var lookupPrice by remember { mutableStateOf("") }
    var lastTransactionId by remember { mutableStateOf("") }
    var showPrintDialog by remember { mutableStateOf(false) }
    var showPrinterPicker by remember { mutableStateOf(false) }
    var pairedPrinters by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    val scope = rememberCoroutineScope()
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun addToCart(barcode: String) {
        val item = inventory.find { it.barcode == barcode }
        if (item == null) {
            currentItem = null
            missingBarcode = barcode
            lookupResult = null
            isScanning = true
            return
        }
        missingBarcode = ""
        lookupResult = null
        saleQty = 1
        currentItem = item
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Scan & Sell")
                        val totalQty = cartItems.sumOf { it.quantity } + (if (currentItem != null) saleQty else 0)
                        if (totalQty > 0) {
                            Spacer(Modifier.width(8.dp))
                            Badge(modifier = Modifier.padding(top = 4.dp)) {
                                Text("$totalQty")
                            }
                        }
                    }
                },
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
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            val capturedItem = currentItem
            val capturedLookup = lookupResult
            if (!hasCameraPermission) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Text("Camera permission is required to scan barcodes", textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Permission")
                    }
                }
            } else if (currentItem == null && isScanning && !cameraError) {
                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            try {
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    try {
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build()
                                        preview.setSurfaceProvider(previewView.surfaceProvider)
                                        val imageAnalysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()
                                        imageAnalysis.setAnalyzer(
                                            ContextCompat.getMainExecutor(ctx),
                                            BarcodeAnalyzer { barcode ->
                                                if (isScanning) {
                                                    isScanning = false
                                                    addToCart(barcode)
                                                }
                                            }
                                        )
                                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner, cameraSelector, preview, imageAnalysis
                                        )
                                    } catch (e: Exception) {
                                        cameraError = true
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                            } catch (e: Exception) {
                                cameraError = true
                            }
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(250.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x00000000)
                        ) {}
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Point camera at barcode", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("The scan will happen automatically", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }

                discount?.let { d ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        color = Amber50,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF57F17))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Active Discount: ${d.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                val desc = if (d.type == DiscountType.PERCENTAGE) "${d.value}% OFF" else "PHP ${d.value} OFF"
                                Text(desc, fontSize = 12.sp, color = Color(0xFFF57F17))
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { viewModel.setCurrentDiscount(null) }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                if (cartItems.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        color = Blue50,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("${cartItems.sumOf { it.quantity }} item(s) in cart", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Spacer(Modifier.weight(1f))
                            Text("PHP ${"%.2f".format(cartItems.sumOf { it.unitPrice * it.quantity })}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Or select manually:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { showProductPicker = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.List, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Choose from Inventory")
                        }
                        if (inventory.isEmpty()) {
                            Text("(No items in inventory yet)", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            } else if (capturedItem != null) {
                    SaleForm(
                        item = capturedItem,
                        quantity = saleQty,
                        cartItems = cartItems,
                        discount = discount,
                        discounts = discounts,
                        onSelectDiscount = { viewModel.setCurrentDiscount(it) },
                        onQuantityChange = { saleQty = it.coerceIn(1, capturedItem.stock) },
                        onConfirm = {
                            val allItems = cartItems + CartItem(
                                barcode = capturedItem.barcode,
                                productName = capturedItem.name,
                                quantity = saleQty,
                                unitPrice = capturedItem.price,
                                costPrice = capturedItem.costPrice
                            )
                            val tid = java.util.UUID.randomUUID().toString()
                            viewModel.processBulkSale(allItems, "", "Walk-in", PaymentType.CASH, false, tid)
                            lastTransactionId = tid
                            cartItems = emptyList()
                            currentItem = null
                            isScanning = true
                            showPrintDialog = true
                        },
                        onCancel = {
                            currentItem = null
                            isScanning = true
                        },
                        onAddAnother = {
                            cartItems = cartItems + CartItem(
                                barcode = capturedItem.barcode,
                                productName = capturedItem.name,
                                quantity = saleQty,
                                unitPrice = capturedItem.price,
                                costPrice = capturedItem.costPrice
                            )
                            currentItem = null
                            isScanning = true
                            saleQty = 1
                        }
                    )
            } else if (lookupLoading) {
                Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Looking up barcode online...", fontSize = 16.sp)
                }
            } else if (capturedLookup != null) {
                    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(56.dp), tint = Green700)
                        Spacer(Modifier.height(12.dp))
                        Text("Product Found Online", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(16.dp))
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Blue50)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(capturedLookup.name.ifBlank { "Unknown Product" }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (capturedLookup.brand.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text("Brand: ${capturedLookup.brand}", fontSize = 13.sp, color = Grey600)
                                }
                                if (capturedLookup.category.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text("Category: ${capturedLookup.category}", fontSize = 13.sp, color = Grey600)
                                }
                                Text("Barcode: $missingBarcode", fontSize = 12.sp, color = Grey600)
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = { showAddFromLookup = true }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add to Inventory")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { missingBarcode = ""; lookupResult = null; isScanning = true }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(10.dp)) {
                            Text("Scan Again")
                        }
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = onBack) {
                            Text("Go Back")
                        }
                    }
            } else if (cameraError) {
                Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Filled.VideocamOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Text("Camera unavailable", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { cameraError = false; isScanning = true }) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Retry Camera")
                    }
                }
            } else if (missingBarcode.isNotBlank()) {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Item not found in inventory", fontSize = 18.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Barcode: $missingBarcode", fontSize = 13.sp, color = Grey600)
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            lookupLoading = true
                            scope.launch {
                                lookupResult = lookupBarcode(missingBarcode)
                                lookupLoading = false
                                if (lookupResult == null) {
                                    Toast.makeText(context, "No product info found online", Toast.LENGTH_SHORT).show()
                                    missingBarcode = ""
                                    isScanning = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Language, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Lookup Online")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { missingBarcode = ""; isScanning = true }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(10.dp)) {
                        Text("Scan Again")
                    }
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onBack) {
                        Text("Go Back")
                    }
                }
            }
        }
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
                isScanning = false
                saleQty = 1
                currentItem = item
            }
        )
    }

    val lookupInfo = lookupResult
    if (showAddFromLookup && lookupInfo != null) {
        val info = lookupInfo
        LaunchedEffect(info) {
            lookupName = info.name
            lookupCategory = info.category
            lookupPrice = ""
        }
        AlertDialog(
            onDismissRequest = { showAddFromLookup = false },
            title = { Text("Add to Inventory", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = lookupName, onValueChange = { lookupName = it }, label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = lookupCategory, onValueChange = { lookupCategory = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = lookupPrice, onValueChange = { lookupPrice = it }, label = { Text("Selling Price") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    Text("Barcode: $missingBarcode", fontSize = 12.sp, color = Grey600)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val price = lookupPrice.toDoubleOrNull() ?: 0.0
                    if (lookupName.isBlank() || price <= 0.0) return@TextButton
                    viewModel.addInventoryItem(
                        InventoryItem(
                            barcode = missingBarcode,
                            name = lookupName.trim(),
                            category = lookupCategory.trim(),
                            price = price,
                            stock = 1,
                            unit = "pcs",
                            minStock = 1
                        )
                    )
                    showAddFromLookup = false
                    missingBarcode = ""
                    lookupResult = null
                    isScanning = true
                    Toast.makeText(context, "Item added to inventory", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Add Item")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFromLookup = false }) { Text("Cancel") }
            }
        )
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
                    else Toast.makeText(context, "No paired Bluetooth printers found", Toast.LENGTH_SHORT).show()
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
                                    if (success) Toast.makeText(context, "Receipt printed", Toast.LENGTH_SHORT).show()
                                    else Toast.makeText(context, "Print failed", Toast.LENGTH_SHORT).show()
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
}

@Composable
internal fun ProductPickerDialog(
    items: List<InventoryItem>,
    onDismiss: () -> Unit,
    onSelect: (InventoryItem) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = if (search.isBlank()) items
    else items.filter { it.name.contains(search, ignoreCase = true) || it.barcode.contains(search, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Select Product", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = search, onValueChange = { search = it },
                    placeholder = { Text("Search by name or barcode...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(if (items.isEmpty()) "No items in inventory" else "No matching items", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(filtered, key = { it.barcode }) { item ->
                            Surface(
                                onClick = { onSelect(item) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("PHP ${"%.2f".format(item.price)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(12.dp))
                                            Text("Stock: ${item.stock} ${item.unit}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (item.size.isNotBlank()) Text("Size: ${item.size}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (item.color.isNotBlank()) Text("Color: ${item.color}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("Barcode: ${item.barcode}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Filled.Add, contentDescription = "Select", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaleForm(
    item: InventoryItem,
    quantity: Int,
    cartItems: List<CartItem>,
    discount: com.salesinventory.app.data.Discount?,
    onQuantityChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onAddAnother: () -> Unit,
    discounts: List<com.salesinventory.app.data.Discount> = emptyList(),
    onSelectDiscount: (com.salesinventory.app.data.Discount?) -> Unit = {}
) {
    val allItems = remember(cartItems, item, quantity) {
        cartItems + CartItem(
            barcode = item.barcode,
            productName = item.name,
            quantity = quantity,
            unitPrice = item.price,
            costPrice = item.costPrice
        )
    }
    val overallSubtotal = allItems.sumOf { it.unitPrice * it.quantity }
    val totalQty = allItems.sumOf { it.quantity }
    val discountAmt = if (discount != null && discount.isActive) {
        when (discount.type) {
            DiscountType.PERCENTAGE -> overallSubtotal * discount.value / 100.0
            DiscountType.FIXED_AMOUNT -> discount.value * totalQty
        }
    } else 0.0
    val overallTotal = maxOf(0.0, overallSubtotal - discountAmt)
    var showDiscountPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text("RECEIPT PREVIEW", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                if (cartItems.isNotEmpty()) {
                    cartItems.forEachIndexed { _, cartItem ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cartItem.productName, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("PHP ${"%.2f".format(cartItem.unitPrice)} x ${cartItem.quantity}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("PHP ${"%.2f".format(cartItem.unitPrice * cartItem.quantity)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("PHP ${"%.2f".format(item.price)} x $quantity", fontSize = 11.sp, color = if (cartItems.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFF57F17))
                    }
                    Text("PHP ${"%.2f".format(item.price * quantity)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (cartItems.isEmpty()) MaterialTheme.colorScheme.onSurface else Color(0xFFF57F17))
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Items:", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text("$totalQty item(s)", fontSize = 13.sp)
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal:", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text("PHP ${"%.2f".format(overallSubtotal)}", fontSize = 13.sp)
                }
                if (discountAmt > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Discount (${discount!!.name}):", fontSize = 13.sp, color = Color(0xFFF57F17))
                        Text("-PHP ${"%.2f".format(discountAmt)}", fontSize = 13.sp, color = Color(0xFFF57F17))
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("PHP ${"%.2f".format(overallTotal)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            onClick = { if (discounts.isNotEmpty()) showDiscountPicker = true },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(if (discount != null) Color(0xFFFFF3E0) else Color(0xFFF5F5F5))
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (discount != null) Icons.Filled.Star else Icons.Filled.Add,
                    contentDescription = null,
                    tint = if (discount != null) Color(0xFFF57F17) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (discount != null) "Discount: ${discount.name}" else "Tap to add discount",
                    fontWeight = if (discount != null) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                    color = if (discount != null) Color(0xFFF57F17) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (discount != null) {
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { onSelectDiscount(null) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove discount", modifier = Modifier.size(16.dp))
                    }
                } else if (discounts.isEmpty()) {
                    Spacer(Modifier.weight(1f))
                    Text("(no discounts)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text("Quantity", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { onQuantityChange(quantity - 1) }, enabled = quantity > 1) {
                Icon(Icons.Filled.Delete, contentDescription = "Decrease", modifier = Modifier.size(28.dp))
            }
            Text("$quantity", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
            IconButton(onClick = { onQuantityChange(quantity + 1) }, enabled = quantity < item.stock) {
                Icon(Icons.Filled.Add, contentDescription = "Increase", modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text("Stock: ${item.stock}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(48.dp)) {
                Text("Cancel")
            }
            Button(
                onClick = onAddAnother,
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Another")
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f).height(48.dp),
                enabled = quantity <= item.stock && quantity > 0
            ) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Confirm Sale")
            }
        }
    }

    if (showDiscountPicker && discounts.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showDiscountPicker = false },
            title = { Text("Select Discount", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    discounts.forEach { d ->
                        val desc = if (d.type == DiscountType.PERCENTAGE) "${d.value}% OFF" else "PHP ${d.value} OFF"
                        Surface(
                            onClick = { onSelectDiscount(d); showDiscountPicker = false },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (discount?.id == d.id) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(d.name, fontWeight = FontWeight.Bold)
                                    Text(desc, fontSize = 12.sp, color = Color(0xFFF57F17))
                                }
                                if (discount?.id == d.id) {
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
}
