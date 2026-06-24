package com.salesinventory.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salesinventory.app.data.Discount
import com.salesinventory.app.data.DiscountType
import com.salesinventory.app.viewmodel.MainViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscountScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val discounts by viewModel.discounts.collectAsState()
    val currentDiscount by viewModel.currentDiscount.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Promotions & Discounts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Discount")
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
            if (currentDiscount != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFFFF3E0))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFFF57F17))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Active Discount: ${currentDiscount!!.name}", fontWeight = FontWeight.Bold)
                            val desc = if (currentDiscount!!.type == DiscountType.PERCENTAGE)
                                "${currentDiscount!!.value}% OFF" else "PHP ${currentDiscount!!.value} OFF"
                            Text(desc, color = androidx.compose.ui.graphics.Color(0xFFF57F17))
                        }
                        Button(
                            onClick = { viewModel.setCurrentDiscount(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }

            if (discounts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Star, contentDescription = "Discounts", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Text("No discounts configured", style = MaterialTheme.typography.titleMedium)
                        Text("Add discounts for monthly promotions", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { showAddDialog = true }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Discount")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(discounts, key = { it.id }) { discount ->
                        DiscountCard(
                            discount = discount,
                            isActive = currentDiscount?.id == discount.id,
                            onActivate = { viewModel.setCurrentDiscount(discount) },
                            onDelete = { viewModel.removeDiscount(discount.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddDiscountDialog(
            onDismiss = { showAddDialog = false },
            onSave = { discount ->
                viewModel.addDiscount(discount)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun DiscountCard(
    discount: Discount,
    isActive: Boolean,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) androidx.compose.ui.graphics.Color(0xFFFFF3E0)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.LocalOffer,
                contentDescription = null,
                tint = if (isActive) androidx.compose.ui.graphics.Color(0xFFF57F17) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(discount.name, fontWeight = FontWeight.Bold)
                val valStr = if (discount.type == DiscountType.PERCENTAGE)
                    "${discount.value}% OFF" else "PHP ${discount.value} OFF"
                Text(
                    valStr,
                    color = if (isActive) androidx.compose.ui.graphics.Color(0xFFF57F17) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isActive) {
                TextButton(onClick = onActivate) {
                    Text("Activate")
                }
            } else {
                Text("Active", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF2E7D32))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDiscountDialog(
    onDismiss: () -> Unit,
    onSave: (Discount) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var typeIndex by remember { mutableStateOf(0) }
    val types = listOf("Percentage (%)", "Fixed Amount (PHP)")
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Discount") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Discount Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Valentine Promo") }
                )

                Text("Discount Type:", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEachIndexed { index, label ->
                        FilterChip(
                            selected = typeIndex == index,
                            onClick = { typeIndex = index },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(if (typeIndex == 0) "Percentage (%) *" else "Amount (PHP) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text(if (typeIndex == 0) "%" else "PHP ") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val valNum = value.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && valNum > 0) {
                        val discount = Discount(
                            id = UUID.randomUUID().toString().take(8),
                            name = name.trim(),
                            type = if (typeIndex == 0) DiscountType.PERCENTAGE else DiscountType.FIXED_AMOUNT,
                            value = valNum,
                            isActive = true
                        )
                        onSave(discount)
                    }
                },
                enabled = name.isNotBlank() && (value.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
