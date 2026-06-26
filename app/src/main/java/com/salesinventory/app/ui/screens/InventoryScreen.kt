package com.salesinventory.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.salesinventory.app.data.InventoryItem
import com.salesinventory.app.scanner.BarcodeAnalyzer
import com.salesinventory.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val inventory by viewModel.inventory.collectAsState()
    val error by viewModel.error.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<InventoryItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = if (searchQuery.isBlank()) inventory
    else inventory.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.barcode.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Item")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                placeholder = { Text("Search by name or barcode...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )

            if (filteredItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.List, contentDescription = "Inventory", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Text("No items found", style = MaterialTheme.typography.titleMedium)
                        Text("Add items to your inventory", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { showAddDialog = true }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Item")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems, key = { it.barcode }) { item ->
                        InventoryItemCard(
                            item = item,
                            onEdit = { editItem = item },
                            onDelete = { viewModel.removeInventoryItem(item.barcode) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onSave = { item ->
                viewModel.addInventoryItem(item)
                showAddDialog = false
            }
        )
    }

    if (editItem != null) {
        EditItemDialog(
            item = editItem!!,
            onDismiss = { editItem = null },
            onSave = { item ->
                viewModel.addInventoryItem(item)
                editItem = null
            }
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
private fun InventoryItemCard(
    item: InventoryItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val stockColor = when {
        item.stock <= 0 -> MaterialTheme.colorScheme.error
        item.stock <= 5 -> androidx.compose.ui.graphics.Color(0xFFF57F17)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.imageUri.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Barcode: ${item.barcode}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("PHP ${"%.2f".format(item.price)}", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Text("Stock: ", fontSize = 13.sp)
                    Text("${item.stock}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = stockColor)
                    Spacer(Modifier.width(4.dp))
                    Text(item.unit, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.size.isNotBlank()) {
                        Text("Size: ${item.size}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (item.color.isNotBlank()) {
                        Text("Color: ${item.color}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (item.category.isNotBlank()) {
                        Text(item.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
internal fun AddItemDialog(
    onDismiss: () -> Unit,
    onSave: (InventoryItem) -> Unit,
    initialBarcode: String = ""
) {
    var barcode by remember { mutableStateOf(initialBarcode) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var costPrice by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("pcs") }
    var size by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var minStock by remember { mutableStateOf("") }
    var subLabel by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    var isLookingUp by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) imageUri = uri.toString()
    }

    LaunchedEffect(barcode) {
        if (barcode.isNotBlank() && name.isBlank() && !isLookingUp) {
            isLookingUp = true
            val info = com.salesinventory.app.util.lookupBarcode(barcode)
            if (info != null) {
                if (name.isBlank()) name = info.name
                if (category.isBlank()) category = info.category
                if (imageUri.isBlank() && info.imageUrl.isNotBlank()) imageUri = info.imageUrl
            }
            isLookingUp = false
        }
    }

    if (showScanner) {
        ScanBarcodeDialog(
            onBarcodeScanned = { scanned -> barcode = scanned; showScanner = false },
            onDismiss = { showScanner = false }
        )
    }



    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Inventory Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("Barcode *") }, singleLine = true, modifier = Modifier.weight(1f))
                    IconButton(onClick = { showScanner = true }) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan Barcode")
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = size, onValueChange = { size = it }, label = { Text("Size") }, singleLine = true, modifier = Modifier.weight(1f), placeholder = { Text("M, XL, 42") })
                    OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Color") }, singleLine = true, modifier = Modifier.weight(1f), placeholder = { Text("Red, Blue") })
                }
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price *") }, singleLine = true, modifier = Modifier.weight(1f), prefix = { Text("PHP ") })
                    OutlinedTextField(value = costPrice, onValueChange = { costPrice = it }, label = { Text("Cost Price") }, singleLine = true, modifier = Modifier.weight(1f), prefix = { Text("PHP ") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stock *") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = minStock, onValueChange = { minStock = it }, label = { Text("Min Stock") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit") }, singleLine = true, modifier = Modifier.fillMaxWidth(), placeholder = { Text("pcs, kg, box") })
                OutlinedTextField(value = subLabel, onValueChange = { subLabel = it }, label = { Text("Sub Label") }, singleLine = true, modifier = Modifier.fillMaxWidth(), placeholder = { Text("MS, CS, LS") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (imageUri.isNotBlank()) {
                        AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(imageUri).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(8.dp))
                    }
                    OutlinedButton(onClick = { imagePicker.launch("image/*") }) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Photo", fontSize = 12.sp)
                    }
                    if (imageUri.isNotBlank()) {
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = { imageUri = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val priceVal = price.toDoubleOrNull() ?: 0.0
                    val stockVal = stock.toIntOrNull() ?: 0
                    if (barcode.isNotBlank() && name.isNotBlank() && priceVal > 0) {
                        onSave(InventoryItem(barcode = barcode.trim(), name = name.trim(), category = category.trim(), price = priceVal, costPrice = costPrice.toDoubleOrNull() ?: 0.0, stock = stockVal, unit = unit.ifBlank { "pcs" }, size = size.trim(), color = color.trim(), subLabel = subLabel.trim().uppercase(), minStock = minStock.toIntOrNull() ?: 0, imageUri = imageUri))
                    }
                },
                enabled = barcode.isNotBlank() && name.isNotBlank() && (price.toDoubleOrNull() ?: 0.0) > 0
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EditItemDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onSave: (InventoryItem) -> Unit
) {
    var barcode by remember { mutableStateOf(item.barcode) }
    var name by remember { mutableStateOf(item.name) }
    var category by remember { mutableStateOf(item.category) }
    var price by remember { mutableStateOf(item.price.toString()) }
    var costPrice by remember { mutableStateOf(item.costPrice.toString()) }
    var stock by remember { mutableStateOf(item.stock.toString()) }
    var unit by remember { mutableStateOf(item.unit) }
    var size by remember { mutableStateOf(item.size) }
    var color by remember { mutableStateOf(item.color) }
    var minStock by remember { mutableStateOf(item.minStock.toString()) }
    var subLabel by remember { mutableStateOf(item.subLabel) }
    var imageUri by remember { mutableStateOf(item.imageUri) }
    var showScanner by remember { mutableStateOf(false) }
    val initialBarcode = remember { item.barcode }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) imageUri = uri.toString()
    }

    LaunchedEffect(barcode) {
        if (barcode.isNotBlank() && barcode != initialBarcode) {
            val info = com.salesinventory.app.util.lookupBarcode(barcode)
            if (info != null) {
                if (name.isBlank()) name = info.name
                if (category.isBlank()) category = info.category
                if (imageUri.isBlank() && info.imageUrl.isNotBlank()) imageUri = info.imageUrl
            }
        }
    }

    if (showScanner) {
        ScanBarcodeDialog(
            onBarcodeScanned = { scanned -> barcode = scanned; showScanner = false },
            onDismiss = { showScanner = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("Barcode *") }, singleLine = true, modifier = Modifier.weight(1f))
                    IconButton(onClick = { showScanner = true }) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan Barcode")
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = size, onValueChange = { size = it }, label = { Text("Size") }, singleLine = true, modifier = Modifier.weight(1f), placeholder = { Text("M, XL, 42") })
                    OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Color") }, singleLine = true, modifier = Modifier.weight(1f), placeholder = { Text("Red, Blue") })
                }
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price *") }, singleLine = true, modifier = Modifier.weight(1f), prefix = { Text("PHP ") })
                    OutlinedTextField(value = costPrice, onValueChange = { costPrice = it }, label = { Text("Cost Price") }, singleLine = true, modifier = Modifier.weight(1f), prefix = { Text("PHP ") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stock *") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = minStock, onValueChange = { minStock = it }, label = { Text("Min Stock") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit") }, singleLine = true, modifier = Modifier.fillMaxWidth(), placeholder = { Text("pcs, kg, box") })
                OutlinedTextField(value = subLabel, onValueChange = { subLabel = it }, label = { Text("Sub Label") }, singleLine = true, modifier = Modifier.fillMaxWidth(), placeholder = { Text("MS, CS, LS") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (imageUri.isNotBlank()) {
                        AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(imageUri).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(8.dp))
                    }
                    OutlinedButton(onClick = { imagePicker.launch("image/*") }) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Photo", fontSize = 12.sp)
                    }
                    if (imageUri.isNotBlank()) {
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = { imageUri = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val priceVal = price.toDoubleOrNull() ?: 0.0
                    val stockVal = stock.toIntOrNull() ?: 0
                    if (barcode.isNotBlank() && name.isNotBlank() && priceVal > 0) {
                        onSave(InventoryItem(barcode = barcode.trim(), name = name.trim(), category = category.trim(), price = priceVal, costPrice = costPrice.toDoubleOrNull() ?: 0.0, stock = stockVal, unit = unit.ifBlank { "pcs" }, size = size.trim(), color = color.trim(), subLabel = subLabel.trim().uppercase(), minStock = minStock.toIntOrNull() ?: 0, imageUri = imageUri))
                    }
                },
                enabled = barcode.isNotBlank() && name.isNotBlank() && (price.toDoubleOrNull() ?: 0.0) > 0
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ScanBarcodeDialog(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasPermission) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).also { previewView ->
                                val future = ProcessCameraProvider.getInstance(ctx)
                                future.addListener({
                                    val provider = future.get()
                                    cameraProvider = provider
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    val analysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                    var done = false
                                    analysis.setAnalyzer(
                                        ContextCompat.getMainExecutor(ctx),
                                        BarcodeAnalyzer { barcode ->
                                            if (!done) { done = true; onBarcodeScanned(barcode) }
                                        }
                                    )
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview, analysis
                                    )
                                }, ContextCompat.getMainExecutor(ctx))
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Text("Camera permission required", color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Grant Permission")
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }

                Text(
                    "Point camera at barcode",
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp)
                )
            }
        }
    }
}
