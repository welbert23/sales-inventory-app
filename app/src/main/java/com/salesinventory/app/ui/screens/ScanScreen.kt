package com.salesinventory.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextOverflow
import com.salesinventory.app.data.DiscountType
import com.salesinventory.app.data.InventoryItem
import com.salesinventory.app.scanner.BarcodeAnalyzer
import com.salesinventory.app.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val scanResult by viewModel.scanResult.collectAsState()
    val saleQuantity by viewModel.saleQuantity.collectAsState()
    val saleComplete by viewModel.saleComplete.collectAsState()
    val error by viewModel.error.collectAsState()
    val discount by viewModel.currentDiscount.collectAsState()

    var isScanning by remember { mutableStateOf(true) }
    var showProductPicker by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf(false) }
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

    LaunchedEffect(saleComplete) {
        if (saleComplete) {
            delay(2000)
            isScanning = true
            viewModel.setScanResult(null)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan & Sell") },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!hasCameraPermission) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = "No camera", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Text("Camera permission is required to scan barcodes", textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Permission")
                    }
                }
            } else if (scanResult == null && isScanning && !cameraError) {
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
                                                    viewModel.setSaleQuantity(1)
                                                    viewModel.setLastScannedBarcode(barcode)
                                                    val item = viewModel.inventory.value.find { it.barcode == barcode }
                                                    viewModel.setScanResult(item)
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
                            Text(
                                "Point camera at barcode",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "The scan will happen automatically",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (discount != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = "Discount", tint = Color(0xFFF57F17))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Active Discount: ${discount!!.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                val desc = if (discount!!.type == DiscountType.PERCENTAGE)
                                    "${discount!!.value}% OFF" else "PHP ${discount!!.value} OFF"
                                Text(desc, fontSize = 12.sp, color = Color(0xFFF57F17))
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { viewModel.setCurrentDiscount(null) }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Or select manually:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showProductPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.List, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Choose from Inventory")
                        }
                        if (viewModel.inventory.value.isEmpty()) {
                            Text("(No items in inventory yet)", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            } else if (scanResult != null) {
                SaleForm(
                    item = scanResult!!,
                    quantity = saleQuantity,
                    discount = discount,
                    onQuantityChange = { viewModel.setSaleQuantity(it) },
                    onConfirm = {
                        viewModel.processSale(scanResult!!.barcode, saleQuantity)
                    },
                    onCancel = {
                        isScanning = true
                        viewModel.setScanResult(null)
                    }
                )
            } else if (saleComplete) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Complete",
                        modifier = Modifier.size(80.dp),
                        tint = Color(0xFF2E7D32)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Sale Complete!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Ready to scan next item...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (cameraError) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.VideocamOff, contentDescription = "Camera error", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Text("Camera unavailable", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Please use the product picker below to select items", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(onClick = { cameraError = false; isScanning = true }) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Retry Camera")
                    }
                }
            } else {
                var showAddDialog by remember { mutableStateOf(false) }
                var scannedBarcode by remember { mutableStateOf("") }

                LaunchedEffect(scanResult) {
                    if (scanResult == null) {
                        scannedBarcode = viewModel.lastScannedBarcode.value
                    }
                }

                if (showAddDialog) {
                    AddItemDialog(
                        initialBarcode = scannedBarcode,
                        onDismiss = { showAddDialog = false },
                        onSave = { item ->
                            viewModel.addInventoryItem(item)
                            showAddDialog = false
                            isScanning = true
                            viewModel.setScanResult(null)
                        }
                    )
                }

                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "Search", modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Item not found in inventory", fontSize = 18.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        isScanning = true
                        viewModel.setScanResult(null)
                    }) {
                        Text("Scan Again")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add to Inventory")
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onBack) {
                        Text("Go Back")
                    }
                }
            }
        }
    }

    if (error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError(); isScanning = true; viewModel.setScanResult(null) }) {
                    Text("OK")
                }
            }
        )
    }

    if (showProductPicker) {
        ProductPickerDialog(
            items = viewModel.inventory.value,
            onDismiss = { showProductPicker = false },
            onSelect = { item ->
                showProductPicker = false
                isScanning = false
                viewModel.setSaleQuantity(1)
                viewModel.setScanResult(item)
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
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = {
            Text("Select Product", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search by name or barcode...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier.height(200.dp).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (items.isEmpty()) "No items in inventory" else "No matching items",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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

@Composable
private fun SaleForm(
    item: InventoryItem,
    quantity: Int,
    discount: com.salesinventory.app.data.Discount?,
    onQuantityChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val subtotal = item.price * quantity
    val discountAmt = if (discount != null && discount.isActive) {
        when (discount.type) {
            DiscountType.PERCENTAGE -> subtotal * discount.value / 100.0
            DiscountType.FIXED_AMOUNT -> discount.value * quantity
        }
    } else 0.0
    val total = subtotal - discountAmt

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Scanned Item", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(4.dp))
                Text("Barcode: ${item.barcode}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Price:", fontWeight = FontWeight.Medium)
                        Text("PHP ${"%.2f".format(item.price)}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    if (item.size.isNotBlank() || item.color.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (item.size.isNotBlank() && item.color.isNotBlank()) "Size / Color:" else if (item.size.isNotBlank()) "Size:" else "Color:", fontWeight = FontWeight.Medium)
                            Text("${item.size}${if (item.size.isNotBlank() && item.color.isNotBlank()) " / " else ""}${item.color}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Available Stock:", fontWeight = FontWeight.Medium)
                        Text("${item.stock} ${item.unit}")
                    }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Quantity", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { onQuantityChange(quantity - 1) }, enabled = quantity > 1) {
                    Icon(Icons.Filled.Delete, contentDescription = "Decrease", modifier = Modifier.size(36.dp))
            }
            Text(
                "$quantity",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            IconButton(onClick = { onQuantityChange(quantity + 1) }, enabled = quantity < item.stock) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase", modifier = Modifier.size(36.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                if (discount != null) Color(0xFFFFF3E0) else Color(0xFFF5F5F5)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal:")
                    Text("PHP ${"%.2f".format(subtotal)}")
                }
                if (discountAmt > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Discount (${discount!!.name}):", color = Color(0xFFF57F17))
                        Text("-PHP ${"%.2f".format(discountAmt)}", color = Color(0xFFF57F17))
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("PHP ${"%.2f".format(total)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(52.dp)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f).height(52.dp),
                enabled = quantity <= item.stock && quantity > 0
            ) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Confirm Sale")
            }
        }
    }
}


